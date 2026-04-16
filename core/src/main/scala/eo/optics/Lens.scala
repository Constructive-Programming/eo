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

  // first/second use specialized existential types (X=B and X=A
  // respectively) that differ from GetReplaceOptic's X=S, so they
  // remain as anonymous Optic subclasses to preserve the transform/
  // place/transfer evidence that depends on X matching a tuple slot.
  def first[A, B] =
    new Optic[(A, B), (A, B), A, A, Tuple2]:
      type X = B
      inline def get: ((A, B)) => A = _._1
      inline def replace: A => ((A, B)) => (A, B) = a =>
        s => (a, s._2)
      inline def to: ((A, B)) => (X, A) = _.swap
      inline def from: ((X, A)) => (A, B) = _.swap

  def second[A, B] =
    new Optic[(A, B), (A, B), B, B, Tuple2]:
      type X = A
      inline def get: ((A, B)) => B = _._2
      inline def replace: B => ((A, B)) => (A, B) = b =>
        s => (s._1, b)
      inline def to: ((A, B)) => (X, B) = identity
      inline def from: ((X, B)) => (A, B) = identity

/** Concrete Optic subclass that stores `get` and `replace` directly,
  * enabling fused extensions on [[Optic]] that bypass the Tuple2
  * carrier entirely: `s => _replace(s, f(_get(s)))`.
  *
  * All `Lens.*` constructors return this type so both hand-written
  * and macro-derived lenses benefit from the fused hot path.
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
  inline def modify[X](f: A => B): S => T =
      s => enplace(s, f(get(s)))
