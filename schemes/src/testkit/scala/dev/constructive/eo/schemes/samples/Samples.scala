package dev.constructive.eo.schemes.samples

/** Showcase ADTs for the schemes specs. Top-level — NOT nested in a spec class — because the
  * eo-generics `plate[S]` macro emits `new V(...)`, which loses outer-accessor wiring for nested
  * ADTs ("missing outer accessor"). Mirrors `tests/.../PlatedSpec.scala` and
  * `generics/.../samples/package.scala`.
  */
enum Expr:
  case Lit(value: Double)
  case Neg(arg: Expr)
  case Add(left: Expr, right: Expr)
  case Mul(left: Expr, right: Expr)

/** A non-recursive carrier holding an `Expr`, to show `cata`-as-`Getter` composing onto an outer
  * optic via `andThen`.
  */
final case class Wrapped(label: String, expr: Expr)
