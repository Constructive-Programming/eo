package eo

import optics.Optic

/** Bridge between two carriers — reshape an `F`-carrier optic into a `G`-carrier optic preserving
  * both halves. Required by `Optic.morph`; the principal mechanism by which optic families cross
  * boundaries (Lens → Optional, Lens → Setter, Iso → Lens, …).
  *
  * @tparam F
  *   source carrier
  * @tparam G
  *   target carrier
  */
trait Composer[F[_, _], G[_, _]]:
  def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G]

/** Typeclass instances for [[Composer]]. Additional composers live near the carrier they produce:
  * `Composer[Tuple2, Affine]` under [[data.Affine]], `Composer[Tuple2, SetterF]` under
  * [[data.SetterF]], `Composer[Tuple2, PowerSeries]` under [[data.PowerSeries]], etc.
  */
object Composer:

  import data.Forgetful

  /** Transitive derivation: given `F → G` and `G → H`, derive `F → H`. Lets callers morph across
    * two bridges without writing the intermediate type.
    *
    * @group Instances
    */
  given chain[F[_, _], G[_, _], H[_, _]](using
      f2g: Composer[F, G],
      g2h: Composer[G, H],
  ): Composer[F, H] with
    def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, H] = g2h.to(f2g.to(o))

  /** Express an Iso (or Getter) as a Lens — the Lens's leftover is `Unit` because the bijection
    * doesn't need any.
    *
    * @group Instances
    */
  given forgetful2tuple: Composer[Forgetful, Tuple2] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forgetful]): Optic[S, T, A, B, Tuple2] =
      new Optic[S, T, A, B, Tuple2]:
        type X = Unit
        val to: S => (X, A) = s => ((), o.to(s))
        val from: ((X, B)) => T = pair => o.from(pair._2)

  /** Express an Iso (or Getter) as a Prism — always takes the `Right` branch; `Nothing` in the
    * `Left` slot so the miss branch is uninhabited.
    *
    * @group Instances
    */
  given forgetful2either: Composer[Forgetful, Either] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forgetful]): Optic[S, T, A, B, Either] =
      new Optic[S, T, A, B, Either]:
        type X = Nothing
        val to: S => Either[X, A] = s => Right(o.to(s))
        val from: Either[X, B] => T = e =>
          e match
            case Right(b) => o.from(b)
            case Left(_)  => ???
