package eo
package optics

object Lens:
  import Function.uncurried

  given tupleInterchangeable[A, B]: (((A, B)) => (B, A)) with
    def apply(t: (A, B)): (B, A) = t.swap

  def pLens[S, T, A, B](get: S => A, enplace: (S, B) => T) =
    GetReplaceOptic(get, enplace)

  def apply[S, A](get: S => A, enplace: (S, A) => S) =
    pLens[S, S, A, A](get, enplace)

  def curried[S, A](get: S => A, replace: A => S => S) =
    pLens[S, S, A, A](get, (s, a) => replace(a)(s))

  def pCurried[S, T, A, B](get: S => A, replace: B => S => T) =
    pLens(get, uncurried(replace))

  // first/second expose the actual structural complement via
  // SplitCombineOptic, so transform/place/transfer work for free.
  def first[A, B] =
    SplitCombineOptic[(A, B), (A, B), A, A, B](
      _._1,
      _.swap,
      (b, a) => (a, b),
      _.swap
    )

  def second[A, B] =
    SplitCombineOptic[(A, B), (A, B), B, B, A](
      _._2,
      identity,
      (a, b) => (a, b),
      identity
    )

/** Concrete Optic subclass that stores `get` and `replace` directly,
  * enabling fused extensions on [[Optic]] that bypass the Tuple2
  * carrier entirely: `s => _replace(s, f(_get(s)))`.
  *
  * Returned by [[Lens.apply]] / [[Lens.pLens]] / [[Lens.curried]] /
  * [[Lens.pCurried]] so hand-written lenses with opaque complement
  * types still benefit from the fused hot path.
  */
class GetReplaceOptic[S, T, A, B](
    val get: S => A,
    val enplace: (S, B) => T,
) extends Optic[S, T, A, B, Tuple2]:
  type X = S
  inline def to: S => (S, A) = s => (s, get(s))
  inline def from: ((S, B)) => T = enplace.tupled
  inline def replace: B => S => T = b =>
    enplace(_, b)
  inline def modify(f: A => B): S => T =
      s => enplace(s, f(get(s)))

/** Concrete Optic subclass that stores a `split` / `combine` pair,
  * exposing the actual structural complement of `S` as its existential
  * `X`. Unlike [[GetReplaceOptic]] (which uses `X = S` because the
  * complement is unknown), this class carries enough information for
  * `transform`, `place`, and `transfer` to work without any external
  * `T => F[X, D]` evidence.
  *
  * Used by [[Lens.first]], [[Lens.second]], and the `eo-generics`
  * `lens[S](_.field)` macro — all of which know the complement type
  * structurally (the sibling tuple slot, or the remaining case-class
  * fields).
  */
final class SplitCombineOptic[S, T, A, B, XA](
    val get: S => A,
    val to: S => (XA, A),
    val combine: (XA, B) => T,
    val complement: T => (XA, B),
) extends Optic[S, T, A, B, Tuple2]:
    type X = XA
    inline def from: ((XA, B)) => T = combine.tupled
    inline def modify(f: A => B): S => T =
      s =>
        val (x, a) = to(s)
        combine(x, f(a))
    inline def replace(b: B): S => T =
      s =>
        val (x, _) = to(s)
        combine(x, b)
    // place / transfer work without any external `T => F[X, D]`
    // evidence: the structural complement `X = XA` is stored on the
    // optic, so the complement part of `to(s)` can be reused verbatim
    // while the focus gets replaced or derived. These are defined on
    // the class body (rather than as an extension on `Optic`) so they
    // apply to polymorphic `SplitCombineOptic`s too, with `S` as the
    // receiver type in place of the generic `T`.
    inline def place(b: B): T => T =
      t =>
        val (x, _) = complement(t)
        combine(x, b)
    inline def transfer[C](f: C => B): T => C => T =
      s => c =>
        val (x, _) = complement(s)
        combine(x, f(c))

object SplitCombineOptic:
  given transformEvidence[S, T, A, B, XA](using o: SplitCombineOptic[S, T, A, B, XA]): (T => Tuple2[o.X, B]) = o.complement
