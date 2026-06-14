package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Cozygomorphism citizen (the generalised apomorphism, *g-apo*) — the **build-side dual of
  * [[Zygo]]** ([[BuildScheme]]) with **`X = Either[B, A]`**: each child slot is either `Left(b)` — a
  * seed handed to the **auxiliary coalgebra** — or `Right(a)` — a seed for the main one.
  *
  * `aux: B => F[B]` is a self-contained unfold (a plain [[Ana]] over `B`); once a slot goes
  * `Left(b)` it stays in `B`-land. `coalg: A => F[Either[B, A]]` is the main unfold, choosing per
  * slot which coalgebra continues. It mirrors how [[Apo]] (`X = Either[S, A]`) sits above [[Ana]]:
  * `cozygo`'s residual is `Either[B, A]` for an arbitrary auxiliary carrier `B` rather than the
  * finished structure `S`. An all-`Right` `coalg` never consults `aux` and degenerates to [[Ana]].
  *
  * '''Versus [[Apo]].''' Apo's `Left(s)` grafts an *already-built* `S` by reference (O(1), no
  * recursion); cozygo's `Left(b)` keeps *unfolding* through `aux`, so it builds rather than grafts
  * — the honest dual of zygo's auxiliary fold. Stack-safe (the [[Machines.foldLayered]] machine).
  */
final class Cozygo[F[_], A, B, S](
    private[zoo] val aux: B => F[B],
    private[zoo] val coalg: A => F[Either[B, A]],
)(using F: Traverse[F], E: Embed[F, S])
    extends BuildScheme[S, A]:
  type X = Either[B, A]

  private val build: A => S =
    val expand: Either[B, A] => F[Either[B, A]] =
      case Left(b)  => F.map(aux(b))(Left(_))
      case Right(a) => coalg(a)
    val run = Machines.foldLayered[F, Either[B, A], S](expand, (_, fr) => E.embed(fr))
    a => run(Right(a))

  protected def write(a: A): S = build(a)
