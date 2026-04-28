package dev.constructive.eo.generics

import scala.quoted.*

/** Quote-context selector-AST helpers shared between [[LensMacro]] and `JsonPrismMacro` /
  * `AvroPrismMacro`. Both parse single-field selector lambdas (`_.fieldName`) and validate
  * non-duplicates; the per-macro divergence is just the error-message tag.
  */
object MacroSelectors:

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
