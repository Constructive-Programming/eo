package eo
package optics

import data.Affine

object Optional:

  def apply[S, T, A, B, F[_, _]](
      getOrModify: S => Either[T, A],
      reverseGet: ((S, B)) => T,
  ) =
    new Optic[S, T, A, B, Affine]:
      type X = (T, S)
      def to: S => Affine[X, A] = s => Affine(getOrModify(s).map(s -> _))
      def from: Affine[X, B] => T = _.affine.fold(identity, reverseGet)
