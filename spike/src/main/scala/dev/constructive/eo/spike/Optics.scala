package dev.constructive.eo.spike

import dev.constructive.eo.data.PSVec
import dev.constructive.eo.optics.{DirectGetter, Getter, Plated, Review}

/** Recursion schemes expressed as **eo optics** — the eo-distinctive form the free-function
  * `Schemes` engine lacked. This is the whole reason to put schemes in an optics library
  * rather than just using droste: a scheme that *is* an optic composes with the rest of the
  * optic surface (`andThen` onto a `Lens`/`Getter`/…), which droste's schemes never will.
  *
  *   - `cata` is a **`Getter`** (a real `Optic[S, Unit, A, Unit, Direct]`) **driven by
  *     `Plated[S]`** — the structural fold generalising `Plated.transform` (`S=>S`) to `S=>A`.
  *     Connected to eo on both ends: it reads children through `Plated` and returns an optic.
  *   - `ana` is a **`Review`** — eo's reverse-construction optic, the established dual of
  *     `Getter`. Its `reverseGet` is the stack-safe unfold (`Schemes.hylo`).
  *
  * eo's generality is *compositional* (schemes join the optic algebra), the trade for droste's
  * *pattern-functor* generality (the full zoo over any `Functor`). That trade is the point of eo.
  */
object Optics:

  /** Catamorphism as a stack-safe `Getter`, driven by `Plated[S]`. The algebra sees the original
    * node `S` (paramorphism-flavored) plus its already-folded children. Reads children via
    * `P.childrenVec`; uses the same `ArrayDeque` post-order machine as `Plated.transformMachine`,
    * so it is stack-safe. Returns an eo `Getter`, hence composes via `andThen`. */
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

  /** Anamorphism as eo's `Review` (reverse-construction optic): a stack-safe unfold `Seed => S`.
    * The closure coalgebra carries its own node builder (encoding A); the stack-safe machine is
    * reused from `Schemes.hylo`. The dual of [[cata]]: `Review` is to construction what `Getter`
    * is to observation. */
  def ana[Seed, S](coalg: Schemes.CoalgA[Seed, S]): Review[S, Seed] =
    Review(Schemes.hylo(coalg))
