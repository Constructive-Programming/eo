package dev.constructive.eo
package accessor

import kyo.Result

trait ReverseAccessor[F[_, _]]:
  def reverseGet[X, A](a: A): F[X, A]

object ReverseAccessor:

  given resultRevAccessor: ReverseAccessor[Result] with
    def reverseGet[X, A](a: A): Result[X, A] = Result.succeed(a)
