package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Zygomorphism citizen — a fold carrying an **auxiliary algebra** alongside the main one
  * ([[ReadScheme]]) with **`X = F[(B, A)]`**: each child slot pairs the auxiliary result `B` with
  * the main result `A`.
  *
  * `aux: F[B] => B` runs a second, self-contained fold whose results the main `alg: F[(B, A)] => A`
  * may read per child. It is the rung the comonad tower skips between [[Cata]] (`X = Nothing`) and
  * [[Para]] (`X = F[(S, A)]`): `para` is exactly `zygo` at `B = S` with `aux = embed` (the
  * auxiliary fold rebuilds the original subterm), and ignoring the `B` half (`alg ∘ map(_._2)`)
  * degenerates to [[Cata]]. The further generalisation — letting `aux` also see the `A` half — is
  * the mutumorphism ([[Mutu]]).
  *
  * '''On the existential.''' `X = F[(B, A)]` is the store comonad over the auxiliary carrier `B`,
  * the same store-comonad complement [[Para]] flags as its writable candidate, but over an
  * arbitrary `B` rather than the structure `S`. The two results are computed in **one pass** (the
  * fold yields `(B, A)` pairs; the final projection keeps the `A`). Stack-safe (the
  * [[Machines.foldLayered]] machine).
  */
final class Zygo[F[_], S, A, B](
    private[zoo] val aux: F[B] => B,
    private[zoo] val alg: F[(B, A)] => A,
)(using F: Traverse[F], P: Project[F, S])
    extends ReadScheme[S, A]:
  type X = F[(B, A)]

  private val run: S => A =
    val fold: S => (B, A) =
      Machines.foldLayered[F, S, (B, A)](
        P.project,
        (_, fba) => (aux(F.map(fba)(_._1)), alg(fba)),
      )
    s => fold(s)._2

  protected def read(s: S): A = run(s)
