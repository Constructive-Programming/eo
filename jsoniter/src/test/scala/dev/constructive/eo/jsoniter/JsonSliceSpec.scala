package dev.constructive.eo.jsoniter

import scala.language.implicitConversions

import cats.Functor
import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray, JsonValueCodec, WriterConfig}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.laws.discipline.{OptionalTests, TraversalTests}
import dev.constructive.eo.laws.{OptionalLaws, TraversalLaws}
import dev.constructive.eo.optics.Optic.*
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

// Fixture ADTs at top level (mirrors the eo-generics convention for macro-facing test types).
final case class SliceInner(flag: Boolean, score: Int)
final case class SliceRec(id: Long, name: String, tags: List[Long], inner: SliceInner)

/** The slice faces, `JsoniterPrism.onSlice` / `JsoniterTraversal.onSlice`, read a JSON document in
  * place from a range of a larger array. The contract is DIFFERENTIAL: a document embedded between
  * junk bytes on both sides reads and writes exactly like the same bytes copied into an array of
  * their own, read by the whole-array optic — for every path shape (root, field, nested, index,
  * wildcard, raw, missing, mistyped). The junk is adversarial: JSON fragments that would extend the
  * document if a scan read past the slice (`42` followed by `999`, `{…}` followed by `,"id":7}`).
  *
  * Plus: the optic laws on embedded slices (`OptionalTests` / `TraversalTests`, slices compared by
  * content); the write rule (a splice returns a fresh array holding only the slice's bytes and
  * never writes the source; a miss hands back the input slice itself); the scanner's ranged entry
  * points (absolute spans, shifted by the offset); and `JsonSlice`'s fail-fast bounds check.
  */
class JsonSliceSpec extends Specification with ScalaCheck with Discipline:

  given JsonValueCodec[Long] = JsonCodecMaker.make
  given JsonValueCodec[Int] = JsonCodecMaker.make
  given JsonValueCodec[String] = JsonCodecMaker.make
  given JsonValueCodec[List[Long]] = JsonCodecMaker.make
  given JsonValueCodec[SliceInner] = JsonCodecMaker.make
  given JsonValueCodec[SliceRec] = JsonCodecMaker.make

  // ----- generators -----------------------------------------------------

  private val genRec: Gen[SliceRec] =
    for
      id <- Gen.choose(-1000L, 1000L)
      name <- Gen.oneOf("", "a", "Ω", "quote\"d", "back\\slash", "line\nbreak")
      tags <- Gen.choose(0, 4).flatMap(Gen.listOfN(_, Gen.choose(-50L, 50L)))
      flag <- Gen.oneOf(true, false)
      score <- Gen.choose(-9, 9)
    yield SliceRec(id, name, tags, SliceInner(flag, score))

  /** Bytes around a slice. JSON fragments that would continue — or complete — the document if a
    * scan crossed the slice's edge, a stray quote or backslash, and arbitrary bytes.
    */
  private val genJunk: Gen[Array[Byte]] = Gen.oneOf(
    Gen.const(Array.emptyByteArray),
    Gen
      .oneOf(
        // continue a complete document: a number, an object, an array
        "9",
        "999",
        ".5e3",
        "}",
        "]",
        ",\"id\":7}",
        "{\"id\":1,",
        "[1,2",
        "null",
        // complete a truncated one: `{` + `"id":7}`, `[1,` + `1]`, `nul` + `l`, `"open` + `"`
        "\"id\":7}",
        "1]",
        "l",
        "\"",
        "\\",
        "  ",
      )
      .map(_.getBytes("UTF-8")),
    Gen.listOf(Arbitrary.arbitrary[Byte]).map(_.toArray),
  )

  /** A document in the middle of junk, with the document's own bytes. */
  private def embed(doc: Array[Byte]): Gen[(JsonSlice, Array[Byte])] =
    for
      prefix <- genJunk
      suffix <- genJunk
    yield (JsonSlice(prefix ++ doc ++ suffix, prefix.length, doc.length), doc)

  private val compact: Gen[Array[Byte]] = genRec.map(writeToArray(_))

  private val anyLayout: Gen[Array[Byte]] = Gen.oneOf(
    compact,
    genRec.map(writeToArray(_, WriterConfig.withIndentionStep(2))),
    genRec.map(r => s"  ${new String(writeToArray(r), "UTF-8")}\n".getBytes("UTF-8")),
  )

  /** Documents of every shape a path can meet at its root: objects, arrays, bare scalars. */
  private val genDoc: Gen[Array[Byte]] = Gen.oneOf(
    anyLayout,
    Gen.listOf(Gen.choose(-99L, 99L)).map(writeToArray(_)),
    Gen.choose(-99999L, 99999L).map(writeToArray(_)),
    Gen.oneOf("", " ", "{", "[1,", "nul", "\"open").map(_.getBytes("UTF-8")),
  )

  // ----- the optics under test ------------------------------------------

  private val root = JsoniterPrism[SliceRec]
  private val id = JsoniterPrism[SliceRec].field(_.id)
  private val name = JsoniterPrism[SliceRec].field(_.name)
  private val score = JsoniterPrism[SliceRec].field(_.inner).field(_.score)
  private val firstTag: JsoniterPrism[Long] = JsoniterPrism[SliceRec].field(_.tags).at(0)
  private val inner = JsoniterPrism[SliceRec].field(_.inner).raw
  private val bareLong = JsoniterPrism[Long]
  private val headOfList = JsoniterPathFixtures.prism[Long]("$[0]")
  private val missing = JsoniterPathFixtures.prism[Long]("$.nope")
  private val mistyped = JsoniterPathFixtures.prism[Long]("$.name")
  private val tags: JsoniterTraversal[Long] = JsoniterPrism[SliceRec].field(_.tags).each
  private val listItems = JsoniterPathFixtures.traversal[Long]("$[*]")

  // ----- differential: in place == copied out -----------------------------

  /** The slice face reads, modifies and replaces like the whole-array prism on the copied bytes,
    * and leaves the source array alone.
    */
  private def samePrism[A](p: JsoniterPrism[A], f: A => A, a: A)(
      slice: JsonSlice,
      doc: Array[Byte],
  ): Prop =
    val before = slice.array.clone
    val s = p.onSlice
    def same(out: JsonSlice, expected: Array[Byte]): Boolean = out == JsonSlice(expected)
    val read = s.getOption(slice).map(normal) == p.getOption(doc).map(normal)
    val modified = same(s.modify(f)(slice), p.modify(f)(doc))
    val replaced = same(s.replace(a)(slice), p.replace(a)(doc))
    val untouched = slice.array.sameElements(before)
    (read :| "getOption") && (modified :| "modify") && (replaced :| "replace") &&
    (untouched :| "source array untouched")

  /** Arrays compare by reference; read their contents instead. */
  private def normal(a: Any): Any = a match
    case bytes: Array[Byte] => bytes.toSeq
    case other              => other

  private def sameTraversal(t: JsoniterTraversal[Long])(slice: JsonSlice, doc: Array[Byte]): Prop =
    val before = slice.array.clone
    val s = t.onSlice
    val read = s.foldMap(List(_))(slice) == t.foldMap(List(_))(doc)
    val modified = s.modify(_ * 3 - 1)(slice) == JsonSlice(t.modify(_ * 3 - 1)(doc))
    val replaced = s.replace(7L)(slice) == JsonSlice(t.replace(7L)(doc))
    (read :| "foldMap") && (modified :| "modify") && (replaced :| "replace") &&
    (slice.array.sameElements(before) :| "source array untouched")

  private def embedded(prop: (JsonSlice, Array[Byte]) => Prop): Prop =
    Prop.forAll(genDoc.flatMap(embed))((slice, doc) => prop(slice, doc))

  "in place == copied out: the root prism" >>
    embedded(
      samePrism(root, r => r.copy(id = r.id + 1), SliceRec(0, "z", Nil, SliceInner(false, 0)))
    )
  "in place == copied out: a field" >> embedded(samePrism(id, _ * 2, 5L))
  "in place == copied out: a string field" >> embedded(samePrism(name, _ + "!", "q\"x"))
  "in place == copied out: a nested field" >> embedded(samePrism(score, _ - 1, 3))
  "in place == copied out: an array index" >> embedded(samePrism(firstTag, _ + 10, -4L))
  "in place == copied out: a raw slice" >> embedded(
    samePrism(inner, identity, "{\"flag\":true,\"score\":1}".getBytes)
  )
  "in place == copied out: a bare scalar document" >> embedded(samePrism(bareLong, _ + 1, 123456L))
  "in place == copied out: an index at the root" >> embedded(samePrism(headOfList, _ - 1, 0L))
  "in place == copied out: a missing path" >> embedded(samePrism(missing, _ + 1, 1L))
  "in place == copied out: a mistyped path" >> embedded(samePrism(mistyped, _ + 1, 1L))
  "in place == copied out: a traversal" >> embedded(sameTraversal(tags))
  "in place == copied out: a traversal at the root" >> embedded(sameTraversal(listItems))

  // ----- the write rule --------------------------------------------------

  "a write that splices returns a fresh array of the slice's bytes; a miss returns the slice" >> {
    val doc = writeToArray(SliceRec(1, "n", List(1, 2), SliceInner(true, 2)))
    val buffer = "xx".getBytes ++ doc ++ "yy".getBytes
    val slice = JsonSlice(buffer, 2, doc.length)
    val hit = id.onSlice.replace(42L)(slice)
    val miss = missing.onSlice.replace(42L)(slice)
    val noFoci = listItems.onSlice.modify(_ + 1)(slice) // a record has no `$[*]`
    val written = tags.onSlice.modify(_ + 1)(slice)
    (hit.offset === 0)
      .and(hit.length === hit.array.length)
      .and(
        hit.toString === """JsonSlice({"id":42,"name":"n","tags":[1,2],"inner":{"flag":true,"score":2}})"""
      )
      .and(written.offset === 0)
      .and(written.array.length === written.length)
      .and(id.onSlice.getOption(written) === Some(1L))
      .and(tags.onSlice.foldMap(List(_))(written) === List(2L, 3L))
      .and(miss must beTheSameAs(slice))
      .and(noFoci must beTheSameAs(slice))
      .and(new String(buffer, "UTF-8") === "xx" + new String(doc, "UTF-8") + "yy")
  }

  "reverseGet on the slice face encodes a slice of its own" >> {
    val r = SliceRec(3, "r", Nil, SliceInner(false, 1))
    val out = root.onSlice.reverseGet(r)
    (out === JsonSlice(writeToArray(r))).and(out.offset === 0).and(out.length === out.array.length)
  }

  // ----- laws, on embedded slices -----------------------------------------

  private given Arbitrary[JsonSlice] = Arbitrary(compact.flatMap(embed).map(_._1))

  checkAll(
    "JsoniterPrism[SliceRec].field(_.id).onSlice",
    new OptionalTests[JsonSlice, Long]:
      val laws = new OptionalLaws[JsonSlice, Long]:
        val optional = id.onSlice
    .optional,
  )

  checkAll(
    "JsoniterPrism[SliceRec].field(_.inner).field(_.score).onSlice",
    new OptionalTests[JsonSlice, Int]:
      val laws = new OptionalLaws[JsonSlice, Int]:
        val optional = score.onSlice
    .optional,
  )

  /** A slice is one document, whatever its foci: the constant functor the traversal laws need. */
  private type Doc[X] = JsonSlice

  private given Functor[Doc] with
    def map[A, B](fa: JsonSlice)(f: A => B): JsonSlice = fa

  checkAll(
    "JsoniterPrism[SliceRec].field(_.tags).each.onSlice",
    new TraversalTests[Doc, Long]:
      val laws = new TraversalLaws[Doc, Long]:
        val traversal = tags.onSlice
    .traversal,
  )

  // ----- the scanner's ranged entry points ----------------------------------

  private val paths: List[List[PathStep]] = List(
    Nil,
    List(PathStep.Field("id")),
    List(PathStep.Field("inner"), PathStep.Field("score")),
    List(PathStep.Field("tags"), PathStep.Index(1)),
    List(PathStep.Index(0)),
    List(PathStep.Field("tags"), PathStep.Wildcard),
    List(PathStep.Wildcard),
  )

  "JsonPathScanner over a range: the whole-array spans, shifted by the offset" >>
    Prop.forAll(genDoc.flatMap(embed), Gen.oneOf(paths)) {
      case ((slice, doc), path) =>
        def shift(span: JsonPathScanner.Span) =
          if span.isHit then
            JsonPathScanner.Span(span.start + slice.offset, span.end + slice.offset)
          else span
        val one = JsonPathScanner.find(slice.array, slice.offset, slice.until, path)
        val all = JsonPathScanner.findAll(slice.array, slice.offset, slice.until, path)
        (one == shift(JsonPathScanner.find(doc, path))) :| "find" &&
        (all == JsonPathScanner.findAll(doc, path).map(shift)) :| "findAll"
    }

  // ----- JsonSlice -----------------------------------------------------------

  "JsonSlice: a range outside the array fails fast with IndexOutOfBoundsException" >>
    Prop.forAll(
      Gen.choose(0, 8),
      Gen.oneOf(Gen.choose(-3, 12), Gen.oneOf(Int.MinValue, Int.MaxValue)),
      Gen.oneOf(Gen.choose(-3, 12), Gen.oneOf(Int.MinValue, Int.MaxValue)),
    ) { (n, offset, length) =>
      val inside = offset >= 0 && length >= 0 && offset.toLong + length <= n
      val built =
        try Right(JsonSlice(new Array[Byte](n), offset, length))
        catch case e: IndexOutOfBoundsException => Left(e)
      built.isRight == inside
    }

  "JsonSlice: an empty slice is a document that misses everywhere" >> {
    val slice = JsonSlice("{\"id\":1}".getBytes, 8, 0)
    (id.onSlice.getOption(slice) === None)
      .and(tags.onSlice.foldMap(List(_))(slice) === Nil)
      .and(id.onSlice.replace(2L)(slice) must beTheSameAs(slice))
  }

  "JsonSlice: equal by content, wherever the bytes sit" >> {
    val a = JsonSlice("..{\"id\":1}".getBytes, 2, 8)
    val b = JsonSlice("{\"id\":1}".getBytes)
    val c = JsonSlice("{\"id\":2}".getBytes)
    (a === b)
      .and(a.hashCode === b.hashCode)
      .and((a == c) === false)
      .and(a.toArray.toSeq === b.array.toSeq)
      .and((a: Any) == "{\"id\":1}" === false)
  }
