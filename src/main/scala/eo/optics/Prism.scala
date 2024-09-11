package eo
package optics

object Prism:
  def apply[S, T, A, B, F[_, _]](
      getOrModify: S => Either[T, A],
      reverseGet: B => T
  ) =
    new Optic[S, T, A, B, Either]:
      type X = T
      def to: S => Either[T, A] = getOrModify
      def from: Either[T, B] => T = _.fold(identity, reverseGet)

  def optional[S, A, B](getOption: S => Option[A], reverseGet: B => S) =
    apply((s: S) => getOption(s).toRight(s), reverseGet)
