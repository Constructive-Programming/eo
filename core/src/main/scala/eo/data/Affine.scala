package eo
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

/** Carrier for the `Optional` family of optics: sealed hierarchy with two variants —
  * [[Affine.Miss]] (no focus; carries `Fst[A]`) and [[Affine.Hit]] (focus present; carries `Snd[A]`
  * + `B`).
  *
  * This representation drops the AnyVal wrapper and the inner `Tuple2` (for Hit) / inner `Either`
  * (for both) that the old `Affine[A, B](val affine: Either[Fst[A], (Snd[A], B)]) extends AnyVal`
  * paid. The new shape:
  *   - **Miss** stores `fst: Fst[A]` directly — no Either wrapper.
  *   - **Hit** stores `snd: Snd[A]` and `b: B` as two direct fields — no Tuple2.
  *
  * Net ~40 bytes/op saved on the hit branch and ~16 bytes/op on the miss branch versus the old
  * AnyVal-over-Either shape. The `.affine: Either[Fst[A], (Snd[A], B)]` accessor is preserved as a
  * legacy view (reconstructs an Either + Tuple2 on read) so law suites and external pattern matches
  * that walked the Either shape keep working; new code should pattern-match `Miss(...)` /
  * `Hit(...)` directly for zero-alloc access.
  *
  * At every constructor site the existential `A` is instantiated to a concrete `Tuple2` (so
  * `Fst[A]` / `Snd[A]` reduce); when Affine is carried through the existential position of an
  * `Optic[…, Affine]` value, `A` becomes abstract and the match types stay inert. Both uses are
  * supported.
  *
  * @tparam A
  *   existential leftover tuple — the `Tuple2` encoding of the miss and hit contexts
  * @tparam B
  *   focus type the caller reads / writes
  */
sealed trait Affine[A, B]:
  import Affine.*

  /** Monomorphic fold — pattern-matches on the Miss/Hit variant and runs the matching branch. No
    * boxing, no allocation beyond what the branches themselves do.
    */
  def fold[C](onMiss: Fst[A] => C, onHit: (Snd[A], B) => C): C = this match
    case m: Miss[A, B] => onMiss(m.fst)
    case h: Hit[A, B]  => onHit(h.snd, h.b)

  /** Legacy Either-shaped accessor. Reconstructs a fresh `Either` (and a fresh `Tuple2` on Hit) on
    * every call — kept for law suites and external code that pattern-match on `.affine`. Prefer
    * `fold` or direct pattern matching on `Miss` / `Hit` in new code.
    */
  def affine: Either[Fst[A], (Snd[A], B)] = this match
    case m: Miss[A, B] => Left(m.fst)
    case h: Hit[A, B]  => Right((h.snd, h.b))

  /** Fold both branches into a new `Affine[A, C]`. Mirrors the old `aFold` but dispatches via
    * `Miss`/`Hit` pattern match directly, so neither branch allocates an intermediate Either.
    */
  def aFold[C](
      f: Fst[A] => Affine[A, C],
      g: ((Snd[A], B)) => Affine[A, C],
  ): Affine[A, C] = this match
    case m: Miss[A, B] => f(m.fst)
    case h: Hit[A, B]  => g((h.snd, h.b))

  /** Effectful traversal over the focus — runs `f` only on the hit branch, passes the miss branch
    * through unchanged via `Applicative.pure`.
    */
  def aTraverse[C, G[_]: Applicative](f: B => G[C]): G[Affine[A, C]] = this match
    case m: Miss[A, B] =>
      Applicative[G].pure(new Miss[A, C](m.fst))
    case h: Hit[A, B] =>
      Applicative[G].map(f(h.b))(c => new Hit[A, C](h.snd, c))

/** Constructors and typeclass instances for [[Affine]]. */
object Affine:

  /** Miss-branch variant: no focus present. Stores `fst: Fst[A]` directly.
    *
    * `B` is phantom at the runtime shape — every `Miss[A, B]` stores only `fst: Fst[A]`, regardless
    * of `B`. Callers that need to re-type a Miss instance across a phantom-B change (e.g. when
    * adapting an `Affine`-carrier optic into another carrier whose variant type differs on the
    * focus side only) should prefer [[widenB]] to a raw `asInstanceOf`.
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

  /** Legacy smart constructor — accept a raw `Either[Fst[A], (Snd[A], B)]` and dispatch to the
    * matching variant. Kept so call sites that still build an Either manually keep working; prefer
    * `new Miss(...)` / `new Hit(...)` in new code to avoid the intermediate Either allocation.
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

  /** `ForgetfulFunctor[Affine]` — maps the focus `B` through `f`, passing the miss branch through
    * unchanged. One allocation per hit-branch map (fresh `Hit`); pure pass-through on the miss
    * branch.
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

  /** Composition functor for `Affine` carriers.
    *
    * The type parameters `X` and `Y` are **deliberately unbounded**. Affine's internals use
    * `Fst[X]` / `Snd[X]` match types, which stay inert (they do not reduce, but do not error
    * either) when `X` is not a `Tuple`; this makes the instance sound for all concrete optic
    * constructors we ship (`Optional.apply` sets `type X = (T, S)`; `Composer[Tuple2, Affine].to`
    * sets `type X = (T, o.X)`; `Composer[Either, Affine].to` sets `type X = (o.X, S)` — every
    * concrete X is a Tuple2).
    *
    * Prior to 0.1.0 this given carried `X <: Tuple, Y <: Tuple`. The bound was load-bearing
    * *defensively* — it prevented pathological composition over an Affine whose X isn't a tuple —
    * but it also blocked legitimate `Lens.andThen(Optional)` because the post-`morph[Affine]`
    * existential is abstract and Scala could not prove the bound through the Optic trait's
    * unbounded `F[_, _]` slot. Dropping the bound preserves composition at the cost of the
    * defensive guard.
    *
    * **Future work — `ValidCarrier[F, X]` witness.** A cleaner long-term story is to thread a
    * `ValidCarrier[F[_, _], X]` typeclass through every optic operation, so each carrier can
    * declare which existentials it admits (`ValidCarrier[Affine, X]` requires `X <: Tuple`;
    * `ValidCarrier[Tuple2, X]` is universal). This lives the constraint at the call-site rather
    * than at the class, and avoids the kind-compatibility wall that hits any attempt to put the
    * bound on `Affine` itself (`class Affine[A <: Tuple, B]` produces a kind mismatch when Affine
    * is passed to `Optic[…, F[_, _]]`). Tracked for a post-0.1.0 release when the public API is
    * stable enough to absorb the witness threading.
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

  /** `Composer[Tuple2, Affine]` — lets a Lens be expressed as an Optional so
    * `lens.andThen(optional)` type-checks through cross-carrier `.andThen` (which summons a
    * `Morph[Tuple2, Affine]` whose resolution picks up this composer). The resulting
    * `Optic[…, Affine]` always takes the "Hit" branch at read time: the Lens never fails.
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

  /** `Composer[Either, Affine]` — express a Prism as an Optional by reusing its `Either`
    * decomposition for Affine's miss / hit branches.
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
