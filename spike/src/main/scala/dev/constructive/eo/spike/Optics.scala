package dev.constructive.eo.spike

import dev.constructive.eo.data.{Direct, PSVec}
import dev.constructive.eo.optics.{DirectGetter, Getter, Optic, Plated}

/** Recursion schemes expressed as **eo optics** — the eo-distinctive form the free-function
  * `Schemes` engine lacked. This is the whole reason to put schemes in an optics library rather
  * than just using droste: a scheme that *is* an optic composes with the rest of the optic surface
  * (`andThen` onto a `Lens`/`Getter`/…), which droste's schemes never will.
  *
  *   - `cata` is a **`Getter`** (a real `Optic[S, Unit, A, Unit, Direct]`) **driven by
  *     `Plated[S]`** — the structural fold generalising `Plated.transform` (`S=>S`) to `S=>A`.
  *     Connected to eo on both ends: it reads children through `Plated` and returns an optic.
  *   - `ana` is a **`Review`** — eo's reverse-construction optic, the established dual of `Getter`.
  *     Its `reverseGet` is the stack-safe unfold (`Schemes.hylo`).
  *
  * eo's generality is *compositional* (schemes join the optic algebra), the trade for droste's
  * *pattern-functor* generality (the full zoo over any `Functor`). That trade is the point of eo.
  */
object Optics:

  /** Catamorphism as a stack-safe `Getter`, driven by `Plated[S]`. The algebra sees the original
    * node `S` (paramorphism-flavored) plus its already-folded children. Reads children via
    * `P.childrenVec`; uses the same `ArrayDeque` post-order machine as `Plated.transformMachine`,
    * so it is stack-safe. Returns an eo `Getter`, hence composes via `andThen`.
    */
  def cata[S, A](alg: (S, PSVec[A]) => A)(using P: Plated[S]): DirectGetter[S, A] =
    Getter[S, A] { root =>
      final class Frame(val node: S, val kids: PSVec[S], val out: Array[AnyRef], var i: Int)
      val stack = new java.util.ArrayDeque[Frame]()
      var ret: AnyRef = null.asInstanceOf[AnyRef]
      def enter(s: S): Unit =
        val kids = P.childrenVec(s)
        if kids.isEmpty then ret = alg(s, PSVec.empty[A]).asInstanceOf[AnyRef]
        else stack.push(new Frame(s, kids, new Array[AnyRef](kids.length), 0))
      enter(root)
      while !stack.isEmpty do
        val fr = stack.peek()
        if fr.i > 0 then fr.out(fr.i - 1) = ret
        if fr.i < fr.kids.length then
          val child = fr.kids(fr.i)
          fr.i += 1
          enter(child)
        else
          ret = alg(fr.node, PSVec.unsafeWrap[A](fr.out)).asInstanceOf[AnyRef]
          val _ = stack.pop()
      ret.asInstanceOf[A]
    }

  /** Anamorphism as a `Review` that **is itself an `Optic`** — the exact mirror of `Getter`.
    *
    * `Getter` is `Optic[S, Unit, A, Unit, Direct]`: a real `to` (read `S => A`) and a vestigial
    * `from`. `ReviewOptic` is the dual `Optic[Unit, S, Unit, A, Direct]`: a vestigial `to` (reads
    * `Unit`) and a real `from` that *builds* `S` from the focus `A`. eo's core `Review` is a bare
    * case class that deliberately does not extend `Optic` ("pure Review has no `to`") — but that
    * asymmetry with `Getter` is a choice, not a necessity: with source `Unit` the `to` is exactly
    * as vestigial as `Getter`'s `from`. This proves `Review` is an optic.
    */
  final class ReviewOptic[S, A](val reverseGet: A => S) extends Optic[Unit, S, Unit, A, Direct]:
    type X = Nothing
    val to: Unit => Direct[X, Unit] = _ => Direct(())
    val from: Direct[X, A] => S = d => reverseGet(d.value)

    /** Fused `Review.andThen(Review)` — the build-direction mirror of `DirectGetter.andThen`.
      * `outer` builds `S` from `A`, `inner` builds `A` from `B`, so the composite builds `S` from
      * `B`. `inline` for the same per-level-distinct-lambda reason as `DirectGetter`.
      */
    inline def andThen[B](inner: ReviewOptic[A, B]): ReviewOptic[S, B] =
      new ReviewOptic(b => reverseGet(inner.reverseGet(b)))

    /** `Review.andThen(Getter)` — the two optics meet at the built type `S`: build `S` from the
      * focus `A` (this `Review`), then read it (the `Getter[S, B]`). Yields a `Getter[A, B]` (read
      * `B` from `A`). When the `Review` is an [[ana]] and the `Getter` a [[cata]], this composition
      * is exactly a (materializing) **hylo** — `cata ∘ ana` as `Getter ∘ Review`.
      */
    inline def andThen[B](getter: DirectGetter[S, B]): DirectGetter[A, B] =
      new DirectGetter(a => getter.get(reverseGet(a)))

  /** Anamorphism as a `ReviewOptic`: a stack-safe unfold `Seed => S`, returned as an eo optic (the
    * reverse-construction dual of [[cata]]'s `Getter`). The closure coalgebra carries its own node
    * builder (encoding A); the stack-safe machine is reused from `Schemes.hylo`.
    */
  def ana[Seed, S](coalg: Schemes.CoalgA[Seed, S]): ReviewOptic[S, Seed] =
    ReviewOptic(Schemes.hylo(coalg))

  /** Hylomorphism as the **composition of the two optics**: `cata ∘ ana`, built as
    * `Review.andThen(Getter)`. The `ana` `Review` builds `S` from a seed; the `cata` `Getter` folds
    * `S` to `A`; their composite is a `Getter[Seed, A]`.
    *
    * This is the **materializing** hylo — the intermediate `S` is constructed, then folded — the
    * textbook `hylo = cata . ana`. Contrast the **fused** `Schemes.hylo`, which never builds `S`.
    * The two agree (the hylo law; checked in `OpticsSpec`). The result is itself a `Getter`, so the
    * whole hylo composes further into the optic pipeline.
    */
  def hylo[Seed, S, A](
      anaReview: ReviewOptic[S, Seed],
      cataGetter: DirectGetter[S, A],
  ): DirectGetter[Seed, A] =
    anaReview.andThen(cataGetter)
