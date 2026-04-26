package dev.constructive.eo.generics

import scala.quoted.*

/** Quote-context selector-AST helpers shared between [[LensMacro]] (this module) and
  * `JsonPrismMacro` in the circe integration module. Both macros parse single-field selector
  * lambdas (`_.fieldName`) and validate that no field appears twice in a multi-selector list;
  * the parsing and validation routines were duplicated across the two files.
  *
  * '''2026-04-26 dedup.''' Both macros now call into [[MacroSelectors]] from inside their splice
  * bodies. The macro-quote-context plumbing is the same on both sides — the only place the macros
  * still differ is in the per-call error-message tag (`"lens[S]"` vs `"JsonPrism.fields[A]"`),
  * which is passed in as a string argument.
  */
object MacroSelectors:

  /** Strips Inlined / Typed wrappers and peeks inside the lambda body for a single Select whose
    * receiver is exactly the lambda parameter (an `Ident`). Same shape used by every macro that
    * accepts a single-field selector lambda.
    *
    * Receiver-is-Ident is load-bearing: nested paths like `_.inner.count` parse as
    * `Select(Select(Ident(param), "inner"), "count")`. The strict variant rejects them with `None`,
    * which lets each macro produce its own "nested paths not supported" message.
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

  /** Validate that a `(positionIndex, fieldName)` list contains no duplicate field name. Both
    * `lens[S](...)` and `JsonPrism.fields[A](...)` reject duplicates as compile errors with the
    * same diagnostic shape — the only divergence is the leading "who" tag.
    *
    * Calls `report.errorAndAbort` on the first duplicate found, never returns. Pure side-effect.
    */
  def reportDuplicateSelectors(using Quotes)(
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
