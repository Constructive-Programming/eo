package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*

import dev.constructive.eo.bench.fixture.*

/** `Setter.modify` at the leaf plus deep manual composition, paired EO vs Monocle.
  *
  * '''Scope note.''' EO's `Setter` carrier is `SetterF`, which has a `ForgetfulFunctor[SetterF]`
  * instance but no `AssociativeFunctor[SetterF, X, Y]` instance — so two Setters cannot be composed
  * via `Optic.andThen`. For depth-3 / depth-6 writes the bench nests `modify` calls — each outer
  * Setter modifies the next `n`, with the innermost modifying the leaf's `value`. Monocle's
  * first-class `Setter.andThen` produces the equivalent composed Setter on its side.
  */
class SetterBench extends JmhDefaults:

  import NestedOptics.{
    d3,
    d6,
    eoS1,
    eoS2,
    eoS3,
    eoS4,
    eoS5,
    eoS6,
    eoSetValue,
    leaf,
    mSet3,
    mSet6,
    mSetValue,
  }

  @Benchmark def eoModify_0: Nested0 = eoSetValue.modify(_ + 1)(leaf)
  @Benchmark def mModify_0: Nested0 = mSetValue.modify(_ + 1)(leaf)

  @Benchmark def eoModify_3: Nested3 =
    eoS3.modify(
      eoS2.modify(
        eoS1.modify(eoSetValue.modify(_ + 1))
      )
    )(d3)

  @Benchmark def mModify_3: Nested3 = mSet3.modify(_ + 1)(d3)

  @Benchmark def eoModify_6: Nested6 =
    eoS6.modify(
      eoS5.modify(
        eoS4.modify(
          eoS3.modify(
            eoS2.modify(
              eoS1.modify(eoSetValue.modify(_ + 1))
            )
          )
        )
      )
    )(d6)

  @Benchmark def mModify_6: Nested6 = mSet6.modify(_ + 1)(d6)
