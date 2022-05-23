package eo

trait Accessor[F[_, _]] {
  def get[A]: [X] => F[X, A] => A
}

object Accessor {
  given tupleAccessor: Accessor[Tuple2] with
    def get[A]: [X] => ((X, A)) => A =
      [X] => (_: (X, A))._2
}

trait ReverseAccessor[F[_, _]] {
  def reverseGet[A]: [X] => A => F[X, A]
}

object ReverseAccessor {
  given eitherRevAccessor: ReverseAccessor[Either] with
    def reverseGet[A]: [X] => A => Either[X, A] =
      [X] => Right[X, A](_: A)
}
