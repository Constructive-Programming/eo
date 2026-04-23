package eo

/** Functor over the second type parameter of a bifunctor-like `F[_, _]`.
  *
  * The primary method `map` is uncurried for allocation-free hot paths. If you need a curried
  * variant, derive it at the call site: `fa => f => FF.map(fa, f)`.
  */
trait ForgetfulFunctor[F[_, _]]:
  def map[X, A, B](fa: F[X, A], f: A => B): F[X, B]

/** Typeclass instances for [[ForgetfulFunctor]].
  *
  * Each supported carrier has a direct instance; the previous generic `Bifunctor` / `Profunctor`
  * fallback instances were removed because every carrier we ship provides a direct instance and the
  * fallbacks never fired. `Forget[F]`'s instance lives in [[data.Forget]] with the rest of the
  * Forget capability ladder.
  */
object ForgetfulFunctor:

  /** Direct `Tuple2` instance — the hot Lens path (`Optic.modify` / `.replace`) relies on this
    * skipping the `Bifunctor[Tuple2].bimap(_)(identity, f)` closure allocation the generic
    * derivation would otherwise incur.
    *
    * Do not define competing `ForgetfulFunctor[Tuple2]` instances downstream — ambiguity would
    * silently regress Lens performance.
    *
    * @group Instances
    */
  given directTuple: ForgetfulFunctor[Tuple2] with
    def map[X, A, B](fa: (X, A), f: A => B): (X, B) = (fa._1, f(fa._2))

  /** Direct `Either` instance — unblocks `.modify` / `.replace` on every Prism.
    *
    * @group Instances
    */
  given directEither: ForgetfulFunctor[Either] with
    def map[X, A, B](fa: Either[X, A], f: A => B): Either[X, B] = fa.map(f)
