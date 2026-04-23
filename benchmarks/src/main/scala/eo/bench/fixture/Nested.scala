package eo
package bench
package fixture

/** Shared nested-case-class fixture for per-optic benchmarks.
  *
  * Seven levels (`Nested0` … `Nested6`) so each bench can measure how composed optics scale with
  * depth — `_0` for the single-hop cost, `_3` for a reasonable production depth, `_6` for the
  * stress case.
  *
  * Each leaf (`Nested0`) carries three kinds of focus:
  *   - `value: Int` — drives Lens / Getter / Setter.
  *   - `flag: Option[String]` — drives Optional (the conditional branch is exactly the `None` side
  *     of the `Option`).
  *   - `items: List[Int]` — drives Fold over a `Foldable` carrier.
  *
  * `Nested1` … `Nested6` are a linear chain with a single `n` field each, so depth-to-depth lens
  * composition is unambiguous.
  */
final case class Nested0(
    value: Int,
    flag: Option[String],
    items: List[Int],
)

final case class Nested1(n: Nested0)
final case class Nested2(n: Nested1)
final case class Nested3(n: Nested2)
final case class Nested4(n: Nested3)
final case class Nested5(n: Nested4)
final case class Nested6(n: Nested5)

object Nested:

  /** A populated leaf with a non-empty `flag` and a 16-element list — so Optional's Some-branch and
    * Fold's list-walk both do real work.
    */
  val DefaultLeaf: Nested0 =
    Nested0(42, Some("hello"), List.tabulate(16)(identity))

  val Default1: Nested1 = Nested1(DefaultLeaf)
  val Default2: Nested2 = Nested2(Default1)
  val Default3: Nested3 = Nested3(Default2)
  val Default4: Nested4 = Nested4(Default3)
  val Default5: Nested5 = Nested5(Default4)
  val Default6: Nested6 = Nested6(Default5)

  /** A leaf with an empty `flag`. Some Optional benches run both variants to show the Some / None
    * cost split.
    */
  val EmptyFlagLeaf: Nested0 = DefaultLeaf.copy(flag = None)
