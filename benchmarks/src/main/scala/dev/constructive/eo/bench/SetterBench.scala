package dev.constructive.eo
package bench


import org.openjdk.jmh.annotations.*

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.optics.{Setter => EoSetter}
import dev.constructive.eo.optics.Optic.*

import monocle.{Setter => MSetter}

/** `Setter.modify` at the leaf plus deep manual composition, paired EO vs Monocle.
  *
  * **Scope note.** EO's `Setter` carrier is `SetterF`, which has a `ForgetfulFunctor[SetterF]`
  * instance but no `AssociativeFunctor[SetterF, X, Y]` instance — so two Setters cannot be composed
  * via `Optic.andThen`. For depth-3 / depth-6 writes the bench nests `modify` calls — each outer
  * Setter modifies the next `n`, with the innermost modifying the leaf's `value`. Monocle's
  * first-class `Setter.andThen` produces the equivalent composed Setter on its side.
  */
class SetterBench extends JmhDefaults:

  private val eoValue =
    EoSetter[Nested0, Nested0, Int, Int](f => n0 => n0.copy(value = f(n0.value)))

  private val mValue: MSetter[Nested0, Int] =
    MSetter[Nested0, Int](f => n0 => n0.copy(value = f(n0.value)))

  private val eoN1 =
    EoSetter[Nested1, Nested1, Nested0, Nested0](f => x => x.copy(n = f(x.n)))

  private val eoN2 =
    EoSetter[Nested2, Nested2, Nested1, Nested1](f => x => x.copy(n = f(x.n)))

  private val eoN3 =
    EoSetter[Nested3, Nested3, Nested2, Nested2](f => x => x.copy(n = f(x.n)))

  private val eoN4 =
    EoSetter[Nested4, Nested4, Nested3, Nested3](f => x => x.copy(n = f(x.n)))

  private val eoN5 =
    EoSetter[Nested5, Nested5, Nested4, Nested4](f => x => x.copy(n = f(x.n)))

  private val eoN6 =
    EoSetter[Nested6, Nested6, Nested5, Nested5](f => x => x.copy(n = f(x.n)))

  private val mN1: MSetter[Nested1, Nested0] =
    MSetter[Nested1, Nested0](f => x => x.copy(n = f(x.n)))

  private val mN2: MSetter[Nested2, Nested1] =
    MSetter[Nested2, Nested1](f => x => x.copy(n = f(x.n)))

  private val mN3: MSetter[Nested3, Nested2] =
    MSetter[Nested3, Nested2](f => x => x.copy(n = f(x.n)))

  private val mN4: MSetter[Nested4, Nested3] =
    MSetter[Nested4, Nested3](f => x => x.copy(n = f(x.n)))

  private val mN5: MSetter[Nested5, Nested4] =
    MSetter[Nested5, Nested4](f => x => x.copy(n = f(x.n)))

  private val mN6: MSetter[Nested6, Nested5] =
    MSetter[Nested6, Nested5](f => x => x.copy(n = f(x.n)))

  private val mSet3 = mN3.andThen(mN2).andThen(mN1).andThen(mValue)

  private val mSet6 =
    mN6.andThen(mN5).andThen(mN4).andThen(mN3).andThen(mN2).andThen(mN1).andThen(mValue)

  private val leaf: Nested0 = Nested.DefaultLeaf
  private val d3: Nested3 = Nested.Default3
  private val d6: Nested6 = Nested.Default6

  @Benchmark def eoModify_0: Nested0 = eoValue.modify(_ + 1)(leaf)
  @Benchmark def mModify_0: Nested0 = mValue.modify(_ + 1)(leaf)

  @Benchmark def eoModify_3: Nested3 =
    eoN3.modify(
      eoN2.modify(
        eoN1.modify(eoValue.modify(_ + 1))
      )
    )(d3)

  @Benchmark def mModify_3: Nested3 = mSet3.modify(_ + 1)(d3)

  @Benchmark def eoModify_6: Nested6 =
    eoN6.modify(
      eoN5.modify(
        eoN4.modify(
          eoN3.modify(
            eoN2.modify(
              eoN1.modify(eoValue.modify(_ + 1))
            )
          )
        )
      )
    )(d6)

  @Benchmark def mModify_6: Nested6 = mSet6.modify(_ + 1)(d6)
