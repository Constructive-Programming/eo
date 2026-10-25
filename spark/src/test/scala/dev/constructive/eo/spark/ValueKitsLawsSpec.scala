package dev.constructive.eo
package spark

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.*
import org.apache.spark.unsafe.types.VariantVal
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

import laws.discipline.{NavigationTests, OptionalTests, PlatedTests, PrismTests, SeamTests}
import laws.{NavigationLaws, OptionalLaws, PlatedLaws, PrismLaws, SeamLaws}

object ValueKitsFixtures:

  // ---- DataType trees ---------------------------------------------------

  private val genAtomic: Gen[DataType] =
    Gen.oneOf(IntegerType, LongType, StringType, BooleanType, DoubleType)

  /** Structs carry the drilled name ("f") only sometimes and every node kind appears, so the miss
    * arms of every navigation optic are generated.
    */
  private def genType(depth: Int): Gen[DataType] =
    if depth <= 0 then genAtomic
    else
      Gen.frequency(
        2 -> genAtomic,
        3 -> Gen.lzy(
          for
            withF <- Gen.oneOf(true, false)
            n <- Gen.choose(0, 2)
            types <- Gen.listOfN(n + 1, genType(depth - 1))
            flags <- Gen.listOfN(n + 1, Gen.oneOf(true, false))
          yield
            val named = types.zip(flags).zipWithIndex.map {
              case ((t, nl), i) =>
                StructField(if withF && i == 0 then "f" else s"k$i", t, nl)
            }
            StructType(named.toArray)
        ),
        1 -> Gen.lzy(
          for
            e <- genType(depth - 1)
            cn <- Gen.oneOf(true, false)
          yield ArrayType(e, cn)
        ),
        1 -> Gen.lzy(
          for
            k <- genAtomic
            v <- genType(depth - 1)
            vcn <- Gen.oneOf(true, false)
          yield MapType(k, v, vcn)
        ),
      )

  given Arbitrary[DataType] = Arbitrary(Gen.sized(d => genType(math.min(d, 3))))
  given Cogen[DataType] = Cogen[String].contramap(_.json)

  // ---- Rows --------------------------------------------------------------

  /** Rows with a schema-consistent mix of: an Int "f" (sometimes absent, NULL, or a String instead
    * — three different misses), a String "s", and sometimes a nested row "n"; plus schema-less
    * rows.
    */
  private def genRow(depth: Int): Gen[Row] =
    for
      f <- Gen.frequency(
        3 -> Gen.choose(-100, 100).map(i => Some(StructField("f", IntegerType) -> (i: Any))),
        1 -> Gen.const(Some(StructField("f", IntegerType) -> (null: Any))),
        1 -> Gen.alphaStr.map(s => Some(StructField("f", StringType) -> (s: Any))),
        1 -> Gen.const(None),
      )
      s <- Gen.alphaStr.map(_.take(6))
      n <-
        if depth <= 0 then Gen.const(None)
        else Gen.option(Gen.lzy(genRow(depth - 1)))
      schemaless <- Gen.frequency(5 -> false, 1 -> true)
    yield
      val cells = f.toList ++ List(StructField("s", StringType) -> (s: Any)) ++
        n.toList.map(r => StructField("n", schemaOf(r)) -> (r: Any))
      val values = cells.map(_._2).toArray
      if schemaless then Row.fromSeq(values.toSeq)
      else new GenericRowWithSchema(values, StructType(cells.map(_._1).toArray))

  private def schemaOf(r: Row): DataType = (r.schema: StructType | Null) match
    case s: StructType => s
    case null          => StructType(Array.empty[StructField])

  // ---- Variants ----------------------------------------------------------

  private val genScalar: Gen[VariantVal] = Gen.oneOf(
    Gen.alphaStr.map(s => Variants.string.reverseGet(s.take(6))),
    Gen.choose(-100000L, 100000L).map(Variants.long.reverseGet),
    Gen.choose(-1e6, 1e6).map(Variants.double.reverseGet),
    Gen.oneOf(true, false).map(Variants.boolean.reverseGet),
    Gen.const(Variants.nullValue),
  )

  /** Built with the kit's own (canonical) constructors; objects carry "f" only sometimes and arrays
    * vary in length, so `field` / `at` misses are generated.
    */
  private def genVariant(depth: Int): Gen[VariantVal] =
    if depth <= 0 then genScalar
    else
      Gen.frequency(
        3 -> genScalar,
        2 -> Gen.lzy(
          for
            withF <- Gen.oneOf(true, false)
            n <- Gen.choose(0, 2)
            vs <- Gen.listOfN(n + 1, genVariant(depth - 1))
          yield Variants.obj(vs.zipWithIndex.map { (v, i) =>
            (if withF && i == 0 then "f" else s"k$i") -> v
          }*)
        ),
        2 -> Gen.lzy(
          Gen.choose(0, 3).flatMap(n => Gen.listOfN(n, genVariant(depth - 1))).map(Variants.arr(_*))
        ),
      )

  given Arbitrary[VariantVal] = Arbitrary(Gen.sized(d => genVariant(math.min(d, 3))))
  given Cogen[VariantVal] = Cogen[String].contramap(_.toJson(java.time.ZoneOffset.UTC))

  given Arbitrary[Row] = Arbitrary(genRow(2))
  given Cogen[Row] = Cogen[String].contramap(_.toString)

/** Law suites for the session-free value kits ([[Schemas]], [[Rows]]) — the same shared Optional /
  * Seam / Navigation / Plated / Prism contract the circe, zio and kyo tree kits run, every
  * navigation optic DRILLED (the generators surround the focus with siblings and generate misses).
  */
class ValueKitsLawsSpec extends Specification with Discipline:

  import Schemas.platedDataType
  import Rows.platedRow
  import ValueKitsFixtures.given

  private def eqT: (DataType, DataType) => Boolean = _ == _
  private def eqR: (Row, Row) => Boolean = _ == _

  // ---- DataTypes ---------------------------------------------------------

  checkAll(
    "Optional[DataType] via field",
    new OptionalTests[DataType, DataType]:
      val laws = new OptionalLaws[DataType, DataType]:
        val optional = Schemas.field("f")
    .optional,
  )

  checkAll(
    "Seam[DataType] via field",
    new SeamTests[DataType, DataType]:
      val laws = new SeamLaws[DataType, DataType]:
        val optic = Schemas.field("f")
        val eqv = eqT
    .seam,
  )

  checkAll(
    "Navigation[DataType] via field",
    new NavigationTests[DataType, DataType]:
      val laws = new NavigationLaws[DataType, DataType]:
        val navigation = Schemas.field("f")
        val eqv = eqT
    .navigation,
  )

  checkAll(
    "Navigation[DataType] via nullable",
    new NavigationTests[DataType, Boolean]:
      val laws = new NavigationLaws[DataType, Boolean]:
        val navigation = Schemas.nullable("f")
        val eqv = eqT
    .navigation,
  )

  checkAll(
    "Seam[DataType] via nullable",
    new SeamTests[DataType, Boolean]:
      val laws = new SeamLaws[DataType, Boolean]:
        val optic = Schemas.nullable("f")
        val eqv = eqT
    .seam,
  )

  checkAll(
    "Seam[DataType] via element",
    new SeamTests[DataType, DataType]:
      val laws = new SeamLaws[DataType, DataType]:
        val optic = Schemas.element
        val eqv = eqT
    .seam,
  )

  checkAll(
    "Navigation[DataType] via mapValue",
    new NavigationTests[DataType, DataType]:
      val laws = new NavigationLaws[DataType, DataType]:
        val navigation = Schemas.mapValue
        val eqv = eqT
    .navigation,
  )

  checkAll(
    "Seam[DataType] via mapKey",
    new SeamTests[DataType, DataType]:
      val laws = new SeamLaws[DataType, DataType]:
        val optic = Schemas.mapKey
        val eqv = eqT
    .seam,
  )

  checkAll(
    "Prism[DataType, StructType] via struct",
    new PrismTests[DataType, StructType]:
      val laws = new PrismLaws[DataType, StructType]:
        val prism = Schemas.struct
    .prism(using
      summon[Arbitrary[DataType]],
      Arbitrary(summon[Arbitrary[DataType]].arbitrary.flatMap {
        case s: StructType => Gen.const(s)
        case _             => Gen.const(StructType(Array(StructField("f", IntegerType))))
      }),
      Cogen[String].contramap[StructType](_.json),
    ),
  )

  checkAll(
    "Plated[DataType]",
    new PlatedTests[DataType]:
      val laws = new PlatedLaws[DataType] {}
    .plated,
  )

  // ---- Rows ----------------------------------------------------------------

  checkAll(
    "Optional[Row] via field",
    new OptionalTests[Row, Int]:
      val laws = new OptionalLaws[Row, Int]:
        val optional = Rows.field[Int]("f")
    .optional,
  )

  checkAll(
    "Seam[Row] via field",
    new SeamTests[Row, Int]:
      val laws = new SeamLaws[Row, Int]:
        val optic = Rows.field[Int]("f")
        val eqv = eqR
    .seam,
  )

  checkAll(
    "Navigation[Row] via field",
    new NavigationTests[Row, Int]:
      val laws = new NavigationLaws[Row, Int]:
        val navigation = Rows.field[Int]("f")
        val eqv = eqR
    .navigation,
  )

  checkAll(
    "Navigation[Row] via at",
    new NavigationTests[Row, String]:
      val laws = new NavigationLaws[Row, String]:
        val navigation = Rows.at[String](1)
        val eqv = eqR
    .navigation,
  )

  checkAll(
    "Seam[Row] via at",
    new SeamTests[Row, String]:
      val laws = new SeamLaws[Row, String]:
        val optic = Rows.at[String](1)
        val eqv = eqR
    .seam,
  )

  checkAll(
    "Plated[Row]",
    new PlatedTests[Row]:
      val laws = new PlatedLaws[Row] {}
    .plated,
  )

  // ---- Variants --------------------------------------------------------------

  import Variants.platedVariant

  private def eqV: (VariantVal, VariantVal) => Boolean = _ == _

  checkAll(
    "Optional[VariantVal] via field",
    new OptionalTests[VariantVal, VariantVal]:
      val laws = new OptionalLaws[VariantVal, VariantVal]:
        val optional = Variants.field("f")
    .optional,
  )

  checkAll(
    "Seam[VariantVal] via field",
    new SeamTests[VariantVal, VariantVal]:
      val laws = new SeamLaws[VariantVal, VariantVal]:
        val optic = Variants.field("f")
        val eqv = eqV
    .seam,
  )

  checkAll(
    "Navigation[VariantVal] via field",
    new NavigationTests[VariantVal, VariantVal]:
      val laws = new NavigationLaws[VariantVal, VariantVal]:
        val navigation = Variants.field("f")
        val eqv = eqV
    .navigation,
  )

  checkAll(
    "Seam[VariantVal] via at",
    new SeamTests[VariantVal, VariantVal]:
      val laws = new SeamLaws[VariantVal, VariantVal]:
        val optic = Variants.at(0)
        val eqv = eqV
    .seam,
  )

  checkAll(
    "Navigation[VariantVal] via at",
    new NavigationTests[VariantVal, VariantVal]:
      val laws = new NavigationLaws[VariantVal, VariantVal]:
        val navigation = Variants.at(0)
        val eqv = eqV
    .navigation,
  )

  checkAll(
    "Prism[VariantVal, String] via string",
    new PrismTests[VariantVal, String]:
      val laws = new PrismLaws[VariantVal, String]:
        val prism = Variants.string
    .prism,
  )

  checkAll(
    "Prism[VariantVal, Long] via long",
    new PrismTests[VariantVal, Long]:
      val laws = new PrismLaws[VariantVal, Long]:
        val prism = Variants.long
    .prism,
  )

  checkAll(
    "Prism[VariantVal, Double] via double",
    new PrismTests[VariantVal, Double]:
      val laws = new PrismLaws[VariantVal, Double]:
        val prism = Variants.double
    .prism,
  )

  checkAll(
    "Plated[VariantVal]",
    new PlatedTests[VariantVal]:
      val laws = new PlatedLaws[VariantVal] {}
    .plated,
  )
