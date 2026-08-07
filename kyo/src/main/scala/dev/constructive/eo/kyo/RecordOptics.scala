package dev.constructive.eo
package kyo

import scala.annotation.unused

import _root_.kyo.*

import optics.{BijectionIso, GetReplaceLens, Lens}

/** kyo `Record` integration — the structural seams between Kyo's staged records and eo optics:
  *
  *   - [[iso(Record)]] — a `Record[F]` and the Scala 3 NamedTuple of the same shape are the same
  *     data; the iso names the bijection. Exact on the NamedTuple side; on the Record side
  *     `get ∘ reverseGet` is `compact` — extra entries a widened record carries are dropped, not
  *     preserved.
  *   - [[lens(Record)]] — each `Name ~ Value` slot of a `Record[F]` is a lawful Lens: the same
  *     `Fields.Have` evidence behind `record.name` reads it, `record & (name ~ value)` replaces
  *     exactly that entry (`&` is right-biased on the underlying string-keyed `Dict`), siblings are
  *     untouched leftovers.
  *
  * Field lenses are construction-only, no automatic capability givens: the field NAME is not
  * determined by the `(S, A)` capability key, so two same-typed fields would collide — summon
  * explicitly and pass with `(using myLens)` where needed.
  */

extension (@unused R: Record.type)

  /** Iso between a NamedTuple OR case class and the kyo `Record` of the same shape:
    * `Record.iso[(name: String, age: Int)]` / `Record.iso[Person]` expands, at compile time, to
    * exactly the code you would write by hand for that shape —
    *
    * {{{
    *   Iso(                                     // named tuple
    *     nt => { val t = nt.toTuple; ("name" ~ t._1) & ("age" ~ t._2) },
    *     rec => (rec.getField("name"), rec.getField("age")).withNames[("name", "age")],
    *   )
    *   Iso(                                     // case class
    *     p => ("name" ~ p.name) & ("age" ~ p.age),
    *     rec => new Person(rec.getField("name"), rec.getField("age")),
    *   )
    * }}}
    *
    * — so both directions are straight-line and cast-free: field reads go through kyo's own
    * `Fields.Have` evidence, tuple reads through the real `_N` accessors (case-class reads are
    * plain field selects), and no runtime `Fields` instance or per-call iteration exists at all.
    * Beyond 22 fields the named-tuple expansion switches to the same `scala.runtime.Tuples` element
    * access the stdlib's `Tuple#apply` / `Tuple.fromIArray` compile to, so width is unbounded
    * either way. The case-class rebuild calls the primary constructor (`new`, not `copy`), so enum
    * cases work too; generic case classes are fine at concrete instantiations
    * (`Record.iso[Box[Int]]`).
    *
    * `reverseGet ∘ get` is the identity; `get ∘ reverseGet` is `compact` (see module notes above).
    */
  transparent inline def iso[T]: BijectionIso[T, T, ?, ?] =
    ${ RecordIsoMacro.isoImpl[T] }

  /** Lens onto one field of a `Record[F]`, GenLens-style partial application:
    * `Record.lens["name" ~ String & "age" ~ Int]("age")`. The field must exist in `F` — the same
    * `Fields.Have` evidence as `record.age` itself, so the focus type is the field's declared type,
    * inferred. Returns the fused [[optics.GetReplaceLens]], so drilling composes on the concrete
    * hot path.
    */
  def lens[F]: RecordLensPartial[F] = RecordLensPartial[F]()

/** Partial application for [[lens(Record)]] — pins `F` so the focus type can come from the
  * field-name singleton's `Fields.Have` evidence (kyo's `StageOps` pattern).
  */
final class RecordLensPartial[F]:

  def apply(name: String & Singleton)(using
      h: Fields.Have[F, name.type]
  ): GetReplaceLens[Record[F], Record[F], h.Value, h.Value] =
    Lens[Record[F], h.Value](
      r => r.getField(name),
      (r, v) => r & (name ~ v),
    )
