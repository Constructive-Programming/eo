package dev.constructive.eo

/** Functor over the second parameter of `F[_, _]`. Uncurried for allocation-free hot paths.
  *
  * @tparam F
  *   two-argument carrier
  */
trait ForgetfulFunctor[F[_, _]]:
  def map[X, A, B](fa: F[X, A], f: A => B): F[X, B]

/** Typeclass instances for [[ForgetfulFunctor]]. Every shipped carrier has a direct instance; the
  * earlier generic `Bifunctor` / `Profunctor` fallbacks were dropped (they never fired). The
  * `Forget[F]` instance lives with the rest of the capability ladder in [[data.Forget]].
  */
object ForgetfulFunctor:

  /** Direct `Tuple2` — the hot Lens `.modify` / `.replace` path relies on skipping the
    * `Bifunctor[Tuple2].bimap` closure allocation. Do NOT define competing instances downstream.
    *
    * @group Instances
    */
  given directTuple: ForgetfulFunctor[Tuple2] with
    def map[X, A, B](fa: (X, A), f: A => B): (X, B) = (fa._1, f(fa._2))

  /** Direct `Either` — unlocks Prism `.modify` / `.replace`. @group Instances */
  given directEither: ForgetfulFunctor[Either] with
    def map[X, A, B](fa: Either[X, A], f: A => B): Either[X, B] = fa.map(f)
