package dev.constructive

/** Existential Optics for Scala 3 — the capability layer.
  *
  * This root package hosts the '''capability traits''' (`CanGet`, `CanGetOption`, `CanModify`,
  * `CanFold`, `CanPut`, `CanPlace`, `CanReverseGet`, `CanTransform`, `CanModifyA`, `CanModifyF`)
  * that consuming code demands instead of concrete optic types: a method that reads takes a
  * `CanGet[S, A]`, never a `Lens`. Concrete optics ([[optics]]) are for construction and
  * composition; capabilities are for use — one optic given per `(S, A)` pair keeps summoning
  * unambiguous. `recast` re-brands an optic's phantom slots where soundness allows.
  *
  * '''Where things live.''' This core module is deliberately macro-free: auto-derivation ships in
  * the SEPARATE `cats-eo-generics` artifact (`dev.constructive.eo.generics.{lens, prism, plate}`) —
  * do not look for a `GenLens` / `Focus` equivalent here. Coming from Monocle: `Setter` is
  * [[optics.Modify]] (`set` is `replace`), `Iso` / `Lens` / `Prism` / `Optional` / `Traversal` /
  * `Getter` / `Fold` keep their names, and `Each` / `Index` / `At` exist as plain constructor
  * OBJECTS, not a typeclass zoo — [[optics.Each]] is [[optics.Traversal.each]] under the
  * searched-for name, [[optics.Index]] focuses one positional/keyed slot (never inserts), and
  * [[optics.At]] is the total Map lens to `Option[V]` (`Some` upserts, `None` deletes). They return
  * ordinary optics; nothing to instance or derive. All composition, same-carrier or cross-family,
  * is the one `.andThen`.
  */
package object eo
