package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.data.Forgetful.given

/** `Getter.get` at the leaf plus deep manual composition, paired EO vs Monocle.
  *
  * '''Scope note.''' EO `Getter`s are `Optic[S, Unit, A, A, Forgetful]`; their T slot is fixed to
  * `Unit`, which means two Getters cannot be composed via `Optic.andThen` (the outer B / inner T
  * slots don't align). For depth-3 / depth-6 reads the bench chains `get` calls manually —
  * `g4.get(g3.get(g2.get(g1.get(s))))` — matching what a user would write. Monocle's
  * `Getter.andThen` (first-class on the trait) produces the equivalent composed reader on its side.
  */
class GetterBench extends JmhDefaults:

  import NestedOptics.{eoGetValue, eoG1, eoG2, eoG3, eoG4, eoG5, eoG6, mGetValue, mGet3, mGet6,
    leaf, d3, d6}

  @Benchmark def eoGet_0: Int = eoGetValue.get(leaf)
  @Benchmark def mGet_0: Int = mGetValue.get(leaf)

  @Benchmark def eoGet_3: Int =
    eoGetValue.get(eoG1.get(eoG2.get(eoG3.get(d3))))

  @Benchmark def mGet_3: Int =
    mGet3.get(d3)

  @Benchmark def eoGet_6: Int =
    eoGetValue.get(eoG1.get(eoG2.get(eoG3.get(eoG4.get(eoG5.get(eoG6.get(d6)))))))

  @Benchmark def mGet_6: Int =
    mGet6.get(d6)
