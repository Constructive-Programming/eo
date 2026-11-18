package dev.constructive.eo
package laws
package eo

import cats.data.{Const, State}
import cats.{Applicative, Id, Traverse}
import dev.constructive.eo.forgetful.*

import optics.Optic
import laws.discipline.internal.MultiShot
import optics.Optic.*

// D-series laws — `Optic.modifyF` specialised at three canonical
// applicatives:
//
//   D1. `modifyF[Id]` ≡ `modify` — the identity applicative collapses
//       effectful update to plain update.
//   D2. `modifyF[List]` ≡ cats' cartesian `Traverse[List]` over the
//       foci — the multi-shot applicative runs every continuation once
//       per branch, which D1 / D3 (one result per call) cannot see.
//   D3. `modifyF[Const[M, *]]` ≡ `foldMap` — the Const applicative
//       collapses effectful update to a monoidal fold (this is the
//       van Laarhoven-style phantom-write trick).

// CPD-OFF — each D-series law is its own trait (discipline rule sets pick them one by one), so
// the optic-under-test header and law signature repeat by design, as in every law file.
/** D1 — `optic.modifyF[Id]` is pointwise equal to `optic.modify`. */
trait ModifyAIdLaws[S, A, F[_, _]]:
  /** The optic under test. */
  def optic: Optic[S, S, A, A, F]

  /** `modifyF[Id](f)(s) == modify(f)(s)`. */
  def modifyAIdIsModify(s: S, f: A => A)(using
      ForgetfulFunctor[F],
      ForgetfulTraverse[F, Applicative],
  ): Boolean =
    val viaModifyA: Id[S] = optic.modifyF[Id](a => f(a): Id[A])(s)
    val viaModify: S = optic.modify(f)(s)
    viaModifyA == viaModify

/** D2 — `optic.modifyF[List]` is cats' cartesian product over the foci. */
trait ModifyAListLaws[S, A, F[_, _]]:
  /** The optic under test. */
  def optic: Optic[S, S, A, A, F]

  /** `modifyF[List](f)(s) == Traverse[List].traverse(foldMap(List(_))(s))(f).map(refill)`, where
    * `refill(bs)` writes the foci `bs` back into `s` in order (a single-shot `State` traverse).
    */
  def modifyAListIsCartesian(s: S, f: A => List[A])(using
      ForgetfulFold[F],
      ForgetfulTraverse[F, Applicative],
  ): Boolean =
    val refill = MultiShot.refill[A, S](optic.modifyF[[z] =>> State[List[A], z]](_)(s))
    val foci = optic.foldMap(List(_))(s)
    optic.modifyF[List](f)(s) == Traverse[List].traverse(foci)(f).map(refill)

/** D3 — `optic.modifyF[Const[M, *]]` is pointwise equal to `optic.foldMap` on the same function,
  * with `M = Int` (additive).
  */
trait ModifyAConstLaws[S, A, F[_, _]]:
  /** The optic under test. */
  def optic: Optic[S, S, A, A, F]

  /** `modifyF[Const[Int, *]](Const(f(_)))(s).getConst == foldMap(f)(s)`. */
  def modifyAConstIsFoldMap(s: S, f: A => Int)(using
      ForgetfulFold[F],
      ForgetfulTraverse[F, Applicative],
  ): Boolean =
    // Const[Int, _] has Applicative iff Monoid[Int] exists (additive)
    type ConstInt[X] = Const[Int, X]
    val viaModifyA: Int =
      optic.modifyF[ConstInt](a => Const(f(a)))(s).getConst
    val viaFoldMap: Int = optic.foldMap(f)(s)
    viaModifyA == viaFoldMap

// CPD-ON
