package eo.laws.data

import eo.data.{Fst, SetterF, Snd}
import eo.ForgetfulFunctor

/** Carrier-level laws for `SetterF[X, A]`.
  *
  * `SetterF[X, A]` wraps a pair `(Fst[X], Snd[X] => A)` — the first component carries "outer
  * state", the second a builder closure that eventually produces the focused `A`. Its
  * `ForgetfulFunctor` instance post-composes the builder with the function argument.
  *
  * Because `SetterF` holds a function, structural `==` cannot witness equality. The laws below
  * therefore assert extensional equality: after applying a law-relevant transformation, the first
  * component and the builder's output on a test input must match the expected values.
  *
  * The `ForgetfulTraverse[SetterF, Distributive]` instance is intentionally not exercised here —
  * writing a standalone law for the distributive-traverse path requires picking a concrete
  * distributive functor (`Id` is distributive, so the identity law collapses to a tautology). The
  * traverse identity is already exercised through the `SetterLaws` discipline suite that consumes
  * this carrier.
  */
trait SetterFLaws[X, A]:

  /** `map(fa, identity)` produces a SetterF whose builder behaves identically to the input on every
    * `Snd[X]` sample.
    */
  def functorIdentity(
      fst: Fst[X],
      fn: Snd[X] => A,
      x: Snd[X],
  )(using FF: ForgetfulFunctor[SetterF]): Boolean =
    val fa = SetterF[X, A]((fst, fn))
    val mapped = FF.map(fa, identity[A])
    mapped.setter._1 == fst && mapped.setter._2(x) == fn(x)

  /** `map(map(fa, f), g)` is extensionally equal to `map(fa, f andThen g)` — sampled at one
    * `Snd[X]`.
    */
  def functorComposition(
      fst: Fst[X],
      fn: Snd[X] => A,
      f: A => A,
      g: A => A,
      x: Snd[X],
  )(using FF: ForgetfulFunctor[SetterF]): Boolean =
    val fa = SetterF[X, A]((fst, fn))
    val lhs = FF.map(FF.map(fa, f), g)
    val rhs = FF.map(fa, f.andThen(g))
    lhs.setter._1 == rhs.setter._1 &&
    lhs.setter._2(x) == rhs.setter._2(x)
