package dev.constructive.eo
package laws
package eo

import cats.data.{Const, State}
import cats.{Applicative, Id, Traverse}
import dev.constructive.eo.forgetful.*

import optics.Optic
import optics.Optic.*

// D-series laws — `Optic.modifyA` specialised at three canonical
// applicatives:
//
//   D1. `modifyA[Id]` ≡ `modify` — the identity applicative collapses
//       effectful update to plain update.
//   D2. `modifyA[List]` ≡ cats' cartesian `Traverse[List]` over the
//       foci — the multi-shot applicative runs every continuation once
//       per branch, which D1 / D3 (one result per call) cannot see.
//   D3. `modifyA[Const[M, *]]` ≡ `foldMap` — the Const applicative
//       collapses effectful update to a monoidal fold (this is the
//       van Laarhoven-style phantom-write trick).

/** D1 — `optic.modifyA[Id]` is pointwise equal to `optic.modify`. */
trait ModifyAIdLaws[S, A, F[_, _]]:
  /** The optic under test. */
  def optic: Optic[S, S, A, A, F]

  /** `modifyA[Id](f)(s) == modify(f)(s)`. */
  def modifyAIdIsModify(s: S, f: A => A)(using
      ForgetfulFunctor[F],
      ForgetfulTraverse[F, Applicative],
  ): Boolean =
    val viaModifyA: Id[S] = optic.modifyA[Id](a => f(a): Id[A])(s)
    val viaModify: S = optic.modify(f)(s)
    viaModifyA == viaModify

/** D2 — `optic.modifyA[List]` is cats' cartesian product over the foci. */
trait ModifyAListLaws[S, A, F[_, _]]:
  /** The optic under test. */
  def optic: Optic[S, S, A, A, F]

  /** `modifyA[List](f)(s) == Traverse[List].traverse(foldMap(List(_))(s))(f).map(refill)`, where
    * `refill(bs)` writes the foci `bs` back into `s` in order (a single-shot `State` traverse).
    */
  def modifyAListIsCartesian(s: S, f: A => List[A])(using
      ForgetfulFold[F],
      ForgetfulTraverse[F, Applicative],
  ): Boolean =
    type Refill[Z] = State[List[A], Z]
    val pop: A => Refill[A] = a =>
      State {
        case b :: rest => (rest, b)
        case Nil       => (Nil, a)
      }
    val refill: List[A] => S = bs => optic.modifyA[Refill](pop)(s).runA(bs).value
    val foci = optic.foldMap(List(_))(s)
    optic.modifyA[List](f)(s) == Traverse[List].traverse(foci)(f).map(refill)

/** D3 — `optic.modifyA[Const[M, *]]` is pointwise equal to `optic.foldMap` on the same function,
  * with `M = Int` (additive).
  */
trait ModifyAConstLaws[S, A, F[_, _]]:
  /** The optic under test. */
  def optic: Optic[S, S, A, A, F]

  /** `modifyA[Const[Int, *]](Const(f(_)))(s).getConst == foldMap(f)(s)`. */
  def modifyAConstIsFoldMap(s: S, f: A => Int)(using
      ForgetfulFold[F],
      ForgetfulTraverse[F, Applicative],
  ): Boolean =
    // Const[Int, _] has Applicative iff Monoid[Int] exists (additive)
    type ConstInt[X] = Const[Int, X]
    val viaModifyA: Int =
      optic.modifyA[ConstInt](a => Const(f(a)))(s).getConst
    val viaFoldMap: Int = optic.foldMap(f)(s)
    viaModifyA == viaFoldMap
