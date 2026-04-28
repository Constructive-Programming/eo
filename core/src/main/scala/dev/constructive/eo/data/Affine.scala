package dev.constructive.eo
package data

import cats.Applicative

import optics.Optic

/** Extract the first element type of a `Tuple2`. Stays as an unreduced match type when `T` is not a
  * `Tuple2` — this is load bearing for [[Affine.assoc]] accepting unbounded existentials.
  */
type Fst[T] = T match
  case (f, s) => f

/** Extract the second element type of a `Tuple2`. See [[Fst]]. */
type Snd[T] = T match
  case (f, s) => s

/** Carrier for the `Optional` family — sealed hierarchy: [[Affine.Miss]] (no focus; carries
  * `Fst[A]`) and [[Affine.Hit]] (focus present; carries `Snd[A]` + `B`). Miss / Hit store fields
  * directly (no Either / Tuple2 wrapper); a `.affine: Either[Fst[A], (Snd[A], B)]` legacy accessor
  * reconstructs the old view on demand. Prefer matching `Miss(...)` / `Hit(...)` for zero-alloc
  * access.
  *
  * At every constructor site `A` is a concrete `Tuple2` (so `Fst[A]` / `Snd[A]` reduce); when
  * carried through an `Optic[…, Affine]` existential, `A` is abstract and the match types stay
  * inert.
  *
  * @tparam A
  *   existential leftover tuple
  * @tparam B
  *   focus type
  */
sealed trait Affine[A, B]:
  import Affine.*

  /** Monomorphic fold — pattern-match on Miss/Hit and run the matching branch.
    *
    * @tparam C
    *   output type
    */
  def fold[C](onMiss: Fst[A] => C, onHit: (Snd[A], B) => C): C = this match
    case m: Miss[A, B] => onMiss(m.fst)
    case h: Hit[A, B]  => onHit(h.snd, h.b)

  /** Legacy Either-shaped accessor (reconstructs a fresh `Either` + Tuple2 on each call). Kept for
    * law suites; new code should pattern-match `Miss` / `Hit` directly.
    */
  def affine: Either[Fst[A], (Snd[A], B)] = this match
    case m: Miss[A, B] => Left(m.fst)
    case h: Hit[A, B]  => Right((h.snd, h.b))

  /** Fold both branches into a new `Affine[A, C]`. Dispatches via direct pattern match (no
    * intermediate Either).
    *
    * @tparam C
    *   focus type of the output
    */
  def aFold[C](
      f: Fst[A] => Affine[A, C],
      g: ((Snd[A], B)) => Affine[A, C],
  ): Affine[A, C] = this match
    case m: Miss[A, B] => f(m.fst)
    case h: Hit[A, B]  => g((h.snd, h.b))

  /** Effectful traversal — runs `f` on the hit branch, passes the miss branch through via
    * `Applicative.pure`.
    *
    * @tparam C
    *   focus produced by `f`
    * @tparam G
    *   effect constructor
    */
  def aTraverse[C, G[_]: Applicative](f: B => G[C]): G[Affine[A, C]] = this match
    case m: Miss[A, B] =>
      Applicative[G].pure(new Miss[A, C](m.fst))
    case h: Hit[A, B] =>
      Applicative[G].map(f(h.b))(c => new Hit[A, C](h.snd, c))

/** Constructors and typeclass instances for [[Affine]]. */
object Affine:

  /** Miss-branch variant — no focus, stores `fst: Fst[A]` directly. `B` is phantom at runtime;
    * callers re-typing across a phantom-B change should prefer [[widenB]] over `asInstanceOf`.
    */
  final class Miss[A, B](val fst: Fst[A]) extends Affine[A, B]:
    override def toString(): String = s"Miss($fst)"

    override def equals(that: Any): Boolean = that match
      case other: Miss[?, ?] => fst == other.fst
      case _                 => false

    override def hashCode(): Int = fst.hashCode

    /** Re-type this `Miss[A, B]` as `Miss[A, B2]` without allocating a new instance. Safe because
      * `Miss` stores only `fst: Fst[A]` — the `B` parameter is phantom at the runtime shape.
      */
    inline def widenB[B2]: Miss[A, B2] = this.asInstanceOf[Miss[A, B2]]

  /** Hit-branch variant: focus present. Stores `snd` and `b` as direct fields. */
  final class Hit[A, B](val snd: Snd[A], val b: B) extends Affine[A, B]:
    override def toString(): String = s"Hit($snd, $b)"

    override def equals(that: Any): Boolean = that match
      case other: Hit[?, ?] => snd == other.snd && b == other.b
      case _                => false

    override def hashCode(): Int = snd.hashCode * 31 + (if b == null then 0 else b.hashCode)

  /** Legacy smart constructor — dispatches a raw Either to the matching variant. Prefer
    * `new Miss(...)` / `new Hit(...)` in new code.
    *
    * @group Constructors
    */
  def apply[A, B](e: Either[Fst[A], (Snd[A], B)]): Affine[A, B] = e match
    case Left(l)       => new Miss[A, B](l)
    case Right((s, b)) => new Hit[A, B](s, b)

  /** Convenience wrapping extension — turns a raw `Either[Fst[X], (Snd[X], B)]` into an
    * `Affine[X, B]` via the [[apply]] smart constructor.
    *
    * @group Instances
    */
  extension [X <: Tuple, B](e: Either[Fst[X], (Snd[X], B)]) def affine: Affine[X, B] = Affine(e)

  /** Miss-branch constructor — produce an `Affine[X, B]` carrying `l` as its Fst component.
    *
    * @group Constructors
    */
  def ofLeft[X, B](l: Fst[X]): Affine[X, B] = new Miss[X, B](l)

  /** Hit-branch constructor — produce an `Affine[X, B]` carrying `r._1` as Snd and `r._2` as the
    * focus.
    *
    * @group Constructors
    */
  def ofRight[X, B](r: (Snd[X], B)): Affine[X, B] = new Hit[X, B](r._1, r._2)

  /** `ForgetfulFunctor[Affine]` — maps the focus `B`, passing the miss branch through. One
    * allocation per hit-branch map; pure pass-through on miss.
    *
    * @group Instances
    */
  given map: ForgetfulFunctor[Affine] with

    def map[X, A, B](fa: Affine[X, A], f: A => B): Affine[X, B] = fa match
      case m: Miss[X, A] => new Miss[X, B](m.fst)
      case h: Hit[X, A]  => new Hit[X, B](h.snd, f(h.b))

  /** `ForgetfulTraverse[Affine, Applicative]` — lifts a focus-level `A => G[B]` into an
    * `Affine[X, A] => G[Affine[X, B]]`. Unlocks `.modifyA` / `.all` / `.modifyF` on Affine- carrier
    * optics.
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[Affine, Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: Affine[X, A] => (A => G[B]) => G[Affine[X, B]] =
      fa => f => fa.aTraverse(f)

  /** Composition functor for `Affine` carriers. `X` / `Y` are deliberately unbounded — Affine's
    * `Fst[X]` / `Snd[X]` match types stay inert when `X` is not a `Tuple`, which is sound for every
    * shipped concrete optic (every concrete X is a Tuple2). A `ValidCarrier[F, X]` threading is a
    * possible post-0.1.0 cleanup.
    *
    * @group Instances
    */
  given assoc[Xo, Xi]: AssociativeFunctor[Affine, Xo, Xi] with
    type Z = (Either[Fst[Xo], (Snd[Xo], Fst[Xi])], (Snd[Xo], Snd[Xi]))

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, Affine] { type X = Xo },
        inner: Optic[A, B, C, D, Affine] { type X = Xi },
    ): Affine[Z, C] = outer.to(s) match
      case om: Miss[Xo, A] =>
        new Miss[Z, C](Left(om.fst))
      case oh: Hit[Xo, A] =>
        inner.to(oh.b) match
          case im: Miss[Xi, C] =>
            new Miss[Z, C](Right((oh.snd, im.fst)))
          case ih: Hit[Xi, C] =>
            new Hit[Z, C]((oh.snd, ih.snd), ih.b)

    def composeFrom[S, T, A, B, C, D](
        xd: Affine[Z, D],
        inner: Optic[A, B, C, D, Affine] { type X = Xi },
        outer: Optic[S, T, A, B, Affine] { type X = Xo },
    ): T = xd match
      case m: Miss[Z, D] =>
        // Fst[Z] = Either[Fst[Xo], (Snd[Xo], Fst[Xi])] — but the match-type
        // reduction can't be proven at the trait level, so we cast.
        m.fst.asInstanceOf[Either[Fst[Xo], (Snd[Xo], Fst[Xi])]] match
          case Left(y)         => outer.from(new Miss[Xo, B](y))
          case Right((x1, y0)) =>
            val b: B = inner.from(new Miss[Xi, D](y0))
            outer.from(new Hit[Xo, B](x1, b))
      case h: Hit[Z, D] =>
        val pair = h.snd.asInstanceOf[(Snd[Xo], Snd[Xi])]
        val xoSnd = pair._1
        val xiSnd = pair._2
        val b: B = inner.from(new Hit[Xi, D](xiSnd, h.b))
        outer.from(new Hit[Xo, B](xoSnd, b))

  /** Lens → Affine. Always-Hit at read; lets `lens.andThen(optional)` type-check via the
    * cross-carrier `Morph[Tuple2, Affine]` that picks up this composer.
    *
    * @group Instances
    */
  given tuple2affine: Composer[Tuple2, Affine] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, Affine] =
      new Optic[S, T, A, B, Affine]:
        type X = (T, o.X)
        val to: S => Affine[X, A] = s =>
          val (xo, a) = o.to(s)
          new Hit[X, A](xo, a)

        val from: Affine[X, B] => T = a =>
          a match
            case m: Miss[X, B] => m.fst
            case h: Hit[X, B]  => o.from((h.snd, h.b))

  // Iso → Affine is handled by the low-priority `chainViaTuple2` fallback in
  // `LowPriorityComposerInstances`. No direct `Composer[Forgetful, Affine]` is shipped.

  /** Prism → Affine — reuses the `Either` decomposition for Affine's miss / hit branches.
    *
    * @group Instances
    */
  given either2affine: Composer[Either, Affine] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, Affine] =
      new Optic[S, T, A, B, Affine]:
        type X = (o.X, S)
        val to: S => Affine[X, A] = s =>
          o.to(s) match
            case Right(a) => new Hit[X, A](s, a)
            case Left(x)  => new Miss[X, A](x)
        val from: Affine[X, B] => T = xb =>
          xb match
            case m: Miss[X, B] => o.from(Left(m.fst))
            case h: Hit[X, B]  => o.from(Right(h.b))
