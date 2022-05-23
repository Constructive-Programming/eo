package eo

trait Composer[F[_, _], G[_, _]] {
  def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G]
}

object Composer {

  given chain[F[_, _], G[_, _], H[_, _]](using
      f2g: Composer[F, G],
      g2h: Composer[G, H]
  ): Composer[F, H] with
    def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, H] =
      g2h.to(f2g.to(o))

  given forgetful2tuple: Composer[Forgetful, Tuple2] with
    def to[S, T, A, B](
        o: Optic[S, T, A, B, Forgetful]
    ): Optic[S, T, A, B, Tuple2] = new Optic[S, T, A, B, Tuple2] {
      type X = Unit
      def to: S => (X, A) = o.to.andThen(() -> _)
      def from: ((X, B)) => T = o.from.compose(_._2)
    }

  given forgetful2either: Composer[Forgetful, Either] with
    def to[S, T, A, B](
        o: Optic[S, T, A, B, Forgetful]
    ): Optic[S, T, A, B, Either] = new Optic[S, T, A, B, Either] {
      type X = Nothing
      def to: S => Either[X, A] = o.to.andThen(Right(_))
      def from: Either[X, B] => T = o.from.compose(_.getOrElse(???))
    }

  given tuple2affine: Composer[Tuple2, Affine] with
    def to[S, T, A, B](
        o: Optic[S, T, A, B, Tuple2]
    ): Optic[S, T, A, B, Affine] = new Optic[S, T, A, B, Affine] {
      type X = (T, o.X)
      def to: S => Affine[X, A] = s => Right(o.to(s))
      def from: Affine[X, B] => T = {
        case Left(t)  => t
        case Right(p) => o.from(p)
      }
    }

  given either2affine: Composer[Either, Affine] with
    def to[S, T, A, B](
        o: Optic[S, T, A, B, Either]
    ): Optic[S, T, A, B, Affine] = new Optic[S, T, A, B, Affine] {
      type X = (o.X, S)
      def to: S => Affine[X, A] = s => o.to(s).map(s -> _)
      def from: Affine[X, B] => T = xb => o.from(xb.map(_._2))
    }

}
