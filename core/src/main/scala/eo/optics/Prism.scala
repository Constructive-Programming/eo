package eo
package optics

object Prism:
  def apply[S, A](
      getOrModify: S => Either[S, A],
      reverseGet: A => S
  ) =
    pPrism(getOrModify, reverseGet)

  def pPrism[S, T, A, B](
      getOrModify: S => Either[T, A],
      reverseGet: B => T
  ) =
    new Optic[S, T, A, B, Either]:
      type X = T
      def to: S => Either[T, A] = getOrModify
      def from: Either[T, B] => T = _.fold(identity, reverseGet)

  def optional[S, A](getOption: S => Option[A], reverseGet: A => S) =
    pPrism((s: S) => getOption(s).toRight(s), reverseGet)

  def pOptional[S, A, B](getOption: S => Option[A], reverseGet: B => S) =
    pPrism((s: S) => getOption(s).toRight(s), reverseGet)
