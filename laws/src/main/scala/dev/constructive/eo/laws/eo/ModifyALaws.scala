package dev.constructive.eo
package laws
package eo

import optics.Optic
import optics.Optic.*

import cats.{Applicative, Id}
import cats.data.Const

// D-series laws — `Optic.modifyA` specialised at two canonical
// applicatives:
//
//   D1. `modifyA[Id]` ≡ `modify` — the identity applicative collapses
//       effectful update to plain update.
//   D3. `modifyA[Const[M, *]]` ≡ `foldMap` — the Const applicative
//       collapses effectful update to a monoidal fold (this is the
//       van Laarhoven-style phantom-write trick).

/** D1 — `optic.modifyA[Id]` is pointwise equal to `optic.modify`. */
trait ModifyAIdLaws[S, A, F[_, _]]:
  def optic: Optic[S, S, A, A, F]

  def modifyAIdIsModify(s: S, f: A => A)(using
      ForgetfulFunctor[F],
      ForgetfulTraverse[F, Applicative],
  ): Boolean =
    val viaModifyA: Id[S] = optic.modifyA[Id](a => f(a): Id[A])(s)
    val viaModify: S = optic.modify(f)(s)
    viaModifyA == viaModify

/** D3 — `optic.modifyA[Const[M, *]]` is pointwise equal to `optic.foldMap` on the same function,
  * with `M = Int` (additive).
  */
trait ModifyAConstLaws[S, A, F[_, _]]:
  def optic: Optic[S, S, A, A, F]

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
