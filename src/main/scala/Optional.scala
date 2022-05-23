package eo

object Optional {

  def apply[S, T, A, B, F[_, _]](
      getOrModify: S => Either[T, A],
      reverseGet: ((S, B)) => T
  ) =
    new Optic[S, T, A, B, Affine] {
      type X = (T, S)
      def to: S => Either[T, (S, A)] = s => getOrModify(s).map(s -> _)
      def from: Either[T, (S, B)] => T = _.fold(identity, reverseGet)
    }

}
