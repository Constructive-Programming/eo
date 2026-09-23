package dev.constructive.eo.avro.vulcan

import scala.quoted.*

import _root_.vulcan.Codec as VCodec
import dev.constructive.eo.avro.vulcan.WholeRecordBuilder.{
  CodecKind,
  DirectKind,
  FieldShape,
  Kind,
  OptionKind,
  RecordKind,
  RecordShape,
  SelfKind
}
import org.apache.avro.Schema

/** The macro behind [[AvroVulcan.recordBuilder]] — walks `A`'s case fields at EXPANSION and emits
  * the [[WholeRecordBuilder.RecordShape]] the runtime assembly resolves against the codec's schema.
  *
  * The macro's whole job is classification: it never emits per-field code. Each case field becomes
  * one [[WholeRecordBuilder.FieldShape]] arm —
  *
  *   - `Option[X]` (dealiased) → [[WholeRecordBuilder.OptionKind]] over `X`'s classification;
  *   - Boolean / Int / Long / Float / Double / String → [[WholeRecordBuilder.DirectKind]];
  *   - a case class (Case-flagged class, not sealed, not a module, not an AnyVal) →
  *     [[WholeRecordBuilder.RecordKind]] with the sub-shape, or [[WholeRecordBuilder.SelfKind]]
  *     when the type is already an ancestor on the derivation path (recursive case classes
  *     terminate at compile time and resolve through the runtime level chain);
  *   - everything else → [[WholeRecordBuilder.CodecKind]] holding the field type's own
  *     `vulcan.Codec`, summoned HERE so the caller's scope answers for its leaves — a missing leaf
  *     codec is a compile error pointing at the exact field.
  *
  * Because the emitted value is plain data, everything schema-dependent (slot resolution, arm
  * validation) happens at builder construction in [[WholeRecordBuilder.derive]] — ordinary testable
  * Scala, no staged code. The whole classification lives in [[builderImpl]] as local defs under ONE
  * `Quotes`: `TypeRepr` / `Symbol` are path-dependent on the Quotes instance, so a
  * `(using Quotes)`-taking helper called from inside a quote would type against a different path.
  */
object RecordBuilderMacro:

  /** Cap on nested record levels per shape. A diamond-heavy case-class graph re-expands shared
    * branches per occurrence, so the shape tree grows multiplicatively even though no type ever
    * repeats on one path (a repeat becomes a [[WholeRecordBuilder.SelfKind]]); beyond this depth
    * the shape is pathological for a whole-record builder and the deep part belongs to its codec.
    */
  private val MaxDepth = 24

  /** Entry: `AvroVulcan.recordBuilder[A]`. Requires a case class `A` (sums encode through their
    * codec, not a builder) and the in-scope `vulcan.Codec[A]` whose schema the builder writes.
    */
  def builderImpl[A: Type](codec: Expr[VCodec[A]])(using
      Quotes
  ): Expr[Exception | WholeRecordBuilder[A]] =
    import quotes.reflect.*

    def recordShapeOf(
        tpe: TypeRepr,
        who: String,
        ancestors: List[Symbol],
        depth: Int,
    ): Expr[RecordShape] =
      if depth > MaxDepth then
        report.errorAndAbort(
          s"$who: shape derivation exceeded $MaxDepth nested record levels — a diamond-heavy"
            + " case-class graph; derive the builder at a shallower type and encode the deep part"
            + " through its codec."
        )
      val fieldExprs = tpe.typeSymbol.caseFields.map { fieldSym =>
        val name = fieldSym.name
        val kind = fieldKind(name, tpe.memberType(fieldSym).dealias, ancestors, depth, who)
        '{ FieldShape(${ Expr(name) }, $kind) }
      }
      '{ RecordShape(${ Expr(tpe.show) }, ${ Expr.ofList(fieldExprs) }) }

    def fieldKind(
        name: String,
        t: TypeRepr,
        ancestors: List[Symbol],
        depth: Int,
        who: String,
    ): Expr[Kind] =
      t.dealias match
        case AppliedType(tc, arg :: Nil) if tc =:= TypeRepr.of[Option] =>
          '{ OptionKind(${ fieldKind(name, arg.dealias, ancestors, depth, who) }) }
        case _ =>
          val tt = t.widen.dealias
          directSchemaType(tt) match
            case null =>
              val tsym = tt.typeSymbol
              if isCaseClass(tt) then
                ancestors.indexOf(tsym) match
                  case -1 =>
                    '{
                      RecordKind(${
                        recordShapeOf(tt, s"$who → $name", tsym :: ancestors, depth + 1)
                      })
                    }
                  case d => '{ SelfKind(${ Expr(d) }) }
              else summonLeafCodec(name, tt, who)
            case st => '{ DirectKind(${ schemaTypeExpr(st) }) }

    /** The schema type a direct (value-is-the-datum) leaf writes, or null when the type has no
      * exact direct arm. Exact kinds only: the builder never widens numerically, so a case field of
      * the wrong primitive falls to its codec rather than to a silent promotion.
      */
    def directSchemaType(t: TypeRepr): Schema.Type | Null =
      if t =:= TypeRepr.of[Boolean] then Schema.Type.BOOLEAN
      else if t =:= TypeRepr.of[Int] then Schema.Type.INT
      else if t =:= TypeRepr.of[Long] then Schema.Type.LONG
      else if t =:= TypeRepr.of[Float] then Schema.Type.FLOAT
      else if t =:= TypeRepr.of[Double] then Schema.Type.DOUBLE
      else if t =:= TypeRepr.of[String] then Schema.Type.STRING
      else null

    /** The quoted `Schema.Type` constant — Java enums do not lift, so each arm is spelled out. */
    def schemaTypeExpr(st: Schema.Type): Expr[Schema.Type] =
      st match
        case Schema.Type.BOOLEAN => '{ Schema.Type.BOOLEAN }
        case Schema.Type.INT     => '{ Schema.Type.INT }
        case Schema.Type.LONG    => '{ Schema.Type.LONG }
        case Schema.Type.FLOAT   => '{ Schema.Type.FLOAT }
        case Schema.Type.DOUBLE  => '{ Schema.Type.DOUBLE }
        case Schema.Type.STRING  => '{ Schema.Type.STRING }
        case other               =>
          report.errorAndAbort(
            s"RecordBuilderMacro: internal — unhandled direct schema type $other"
          )

    def isCaseClass(t: TypeRepr): Boolean =
      val sym = t.typeSymbol
      sym.isClassDef && sym.flags.is(Flags.Case) && !sym.flags.is(Flags.Sealed)
      && !sym.flags.is(Flags.Module) && !(t <:< TypeRepr.of[AnyVal])

    def summonLeafCodec(name: String, t: TypeRepr, who: String): Expr[Kind] =
      t.asType match
        case '[x] =>
          Expr.summon[VCodec[x]] match
            case Some(codecE) => '{ CodecKind(${ codecE }.asInstanceOf[VCodec[Any]]) }
            case None         =>
              report.errorAndAbort(
                s"$who: field '$name' of type ${Type.show[x]} has no given vulcan.Codec in scope."
                  + " The builder fast-paths Boolean/Int/Long/Float/Double/String leaves and nested"
                  + " case-class fields; everything else (enums, bytes, logical types, collections,"
                  + " sums, value classes) falls back to the field type's own vulcan codec — provide"
                  + " it or change the field's type."
              )

    val who = s"AvroVulcan.recordBuilder[${Type.show[A]}]"
    val tpe = TypeRepr.of[A].dealias
    if !isCaseClass(tpe) then
      report.errorAndAbort(
        s"$who: ${Type.show[A]} is not a case class — the builder mirrors a case class onto record"
          + " fields; sums (sealed traits, enums, unions) encode through their codec."
      )
    val shape = recordShapeOf(tpe, who, tpe.typeSymbol :: Nil, depth = 0)
    // Total: the codec's schema failure and the assembly failure both come back as the union's
    // Exception half (the vulcan error wrapped, its own throwable as the cause).
    '{
      ${ codec }
        .schema
        .fold(
          e =>
            IllegalArgumentException(
              ${ Expr(who) } + ": the vulcan codec's schema did not resolve",
              e.throwable,
            ),
          schema => WholeRecordBuilder.derive[A](schema, $shape, ${ Expr(who) }),
        )
    }.asExprOf[Exception | WholeRecordBuilder[A]]

end RecordBuilderMacro
