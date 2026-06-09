package dev.constructive.eo.spike.samples

/** Showcase ADT for the corecursion (generate) spike.
  *
  * Declared at the top level — NOT nested in a spec class or object — because the eo-generics
  * `plate` / `lens` / `prism` macros emit `new V(...)` constructor calls that lose their
  * outer-accessor wiring when the ADT is nested ("missing outer accessor in class …"). This mirrors
  * the hoisting in `tests/.../PlatedSpec.scala` and `generics/.../samples/package.scala`.
  *
  * Homogeneous children (every recursive position is again `Expr`), which is all the single-sorted
  * `PSVec[S]` engine can represent — the spike's verdict is scoped to such self-similar types (see
  * the plan's R4 engine-scope finding).
  */
enum Expr:
  case Lit(value: Double)
  case Neg(arg: Expr)
  case Add(left: Expr, right: Expr)
  case Mul(left: Expr, right: Expr)
