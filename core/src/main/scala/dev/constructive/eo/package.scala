package dev.constructive

/** Existential Optics for Scala 3 — the capability layer.
  *
  * This root package hosts the '''capability traits''' (`CanGet`, `CanGetOption`, `CanModify`,
  * `CanFold`, `CanPut`, `CanPlace`, `CanReverseGet`, `CanTransform`, `CanModifyA`, `CanModifyF`)
  * that consuming code demands instead of concrete optic types: a method that reads takes a
  * `CanGet[S, A]`, never a `Lens`. Concrete optics ([[optics]]) are for construction and
  * composition; capabilities are for use — one optic given per `(S, A)` pair keeps summoning
  * unambiguous. `recast` re-brands an optic's phantom slots where soundness allows.
  */
package object eo
