package dev.constructive.eo
package forgetful

import kyo.Result

trait ForgetfulFunctor[F[_, _]]:
  def map[X, A, B](fa: F[X, A], f: A => B): F[X, B]

object ForgetfulFunctor:

  given directTuple: ForgetfulFunctor[Tuple2] with
    def map[X, A, B](fa: (X, A), f: A => B): (X, B) = (fa._1, f(fa._2))

  given directResult: ForgetfulFunctor[Result] with
    def map[X, A, B](fa: Result[X, A], f: A => B): Result[X, B] = fa.map(f)
