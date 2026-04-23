package eo.circe

import scala.quoted.*

import io.circe.{Decoder, Encoder}

/** Companion macro for `NamedTupleCodecSpike`. Lives in a sibling file because Scala 3 forbids
  * calling a macro in the same compilation unit that defines its impl.
  *
  * Synthesises a NamedTuple type via `TypeRepr.of[*:].appliedTo(...)` (the same mechanism the real
  * `.fields` macro will use in Unit 3), then runs `Expr.summon[Encoder[nt]]` /
  * `Expr.summon[Decoder[nt]]` on the synthesised type. Returns a pair of booleans indicating
  * whether each summon succeeded — the call-site must have `given Codec.AsObject[<matching NT>]` in
  * scope for the summons to resolve.
  */
object NamedTupleCodecSpikeMacro:

  inline def summonEncDec: (Boolean, Boolean) = ${ summonEncDecImpl }

  def summonEncDecImpl(using q: Quotes): Expr[(Boolean, Boolean)] =
    import quotes.reflect.*

    val namesTpe: TypeRepr =
      TypeRepr.of[*:].appliedTo(List(ConstantType(StringConstant("a")), TypeRepr.of[EmptyTuple]))
    val valuesTpe: TypeRepr =
      TypeRepr.of[*:].appliedTo(List(TypeRepr.of[Int], TypeRepr.of[EmptyTuple]))
    val ntTpe: TypeRepr =
      TypeRepr.of[scala.NamedTuple.NamedTuple].appliedTo(List(namesTpe, valuesTpe))

    ntTpe.asType match
      case '[nt] =>
        val enc = Expr.summon[Encoder[nt]]
        val dec = Expr.summon[Decoder[nt]]
        val encOk = Expr(enc.isDefined)
        val decOk = Expr(dec.isDefined)
        '{ ($encOk, $decOk) }
