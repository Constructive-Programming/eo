package dev.constructive.eo.generics

import scala.quoted.*

/** Quote-context selector-AST helpers shared between [[LensMacro]] and the `JsonPrismMacro` /
  * `AvroPrismMacro` cursor macros. All parse single-field selector lambdas (`_.fieldName`) and
  * validate non-duplicates; the per-macro divergence is just the error-message tag.
  */
object MacroSelectors:

  /** Loose variant of [[extractSingleFieldName]] — strips `Inlined` / `Typed` wrappers around the
    * lambda AND around its `Select` body, and does NOT require the `Select` receiver to be the bare
    * lambda parameter. Used by the cursor macros' `.field(_.x)` sugar, whose selectors are always
    * single-hop but may arrive wrapped. Kept distinct from [[extractSingleFieldName]] (which
    * rejects nested chains by construction) so callers that need the strict form still have it.
    */
  def extractFieldName(using Quotes)(t: quotes.reflect.Term): Option[String] =
    import quotes.reflect.*
    t match
      case Inlined(_, _, inner)                      => extractFieldName(inner)
      case Typed(inner, _)                           => extractFieldName(inner)
      case Lambda(_, Select(_, name))                => Some(name)
      case Lambda(_, Inlined(_, _, Select(_, name))) => Some(name)
      case Lambda(_, Typed(Select(_, name), _))      => Some(name)
      case _                                         => None

  /** The single element type of a Scala collection type `A` (via its `Iterable` base type), or
    * abort with a `who`-tagged error. Shared by the cursor macros' `.at` / `.each` sugar.
    */
  def iterableElementType[A: Type](
      who: String
  )(using q: Quotes): q.reflect.TypeRepr =
    import quotes.reflect.*
    val aTpe = TypeRepr.of[A]
    val iterSym = TypeRepr.of[Iterable[Any]].typeSymbol
    aTpe.baseType(iterSym) match
      case AppliedType(_, elem :: Nil) => elem.widen
      case _                           =>
        report.errorAndAbort(
          s"$who: expected a Scala collection (Vector / List / Seq / …) with a single element type; got ${Type
              .show[A]}."
        )

  /** Strips `Inlined` / `Typed` wrappers and peeks inside the lambda body for a single `Select`
    * whose receiver is exactly the lambda parameter (an `Ident`). Receiver-is-Ident is load-bearing
    * — nested paths like `_.a.b` parse as `Select(Select(Ident(_), "a"), "b")` and fall through to
    * `None` so each macro can produce its own "nested paths" message.
    */
  def extractSingleFieldName(using Quotes)(t: quotes.reflect.Term): Option[String] =
    import quotes.reflect.*
    def oneHop(body: Term): Option[String] =
      body match
        case Inlined(_, _, inner)   => oneHop(inner)
        case Typed(inner, _)        => oneHop(inner)
        case Select(Ident(_), name) => Some(name)
        case _                      => None
    t match
      case Inlined(_, _, inner) => extractSingleFieldName(inner)
      case Typed(inner, _)      => extractSingleFieldName(inner)
      case Lambda(_, body)      => oneHop(body)
      case _                    => None

  /** Reject duplicate field names; aborts on the first duplicate. The `who` tag prefixes the
    * message.
    */
  def reportDuplicateSelectors(using
      Quotes
  )(
      who: String,
      resolved: List[(Int, String)],
  ): Unit =
    import quotes.reflect.*
    val byName: Map[String, List[Int]] = resolved.groupMap(_._2)(_._1)
    byName.find(_._2.sizeIs > 1).foreach {
      case (name, positions) =>
        val sorted = positions.sorted
        report.errorAndAbort(
          s"$who: duplicate field selector '$name' at positions"
            + s" ${sorted.mkString(", ")}. Each field may appear at most once."
        )
    }
