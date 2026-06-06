package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.circe.{JsonPrism, codecPrism}

import io.circe.Json
import io.circe.syntax.*

/** Integration-optic construction / amortization (plan 009, Phase 2).
  *
  * Every other bench builds its optics once, outside the loop — correct for steady state, but it
  * hides the construction cost. This bench answers "is one-shot `codecPrism` use cheap?" for the
  * circe backend by contrasting three shapes on the same depth-3 focus:
  *
  *   - `build` — construct `codecPrism[Order].field(_.customer).field(_.address).field(_.street)`
  *     and return it (the `transparent inline` `.field` chain resolves at compile time; this row is
  *     the runtime path-array allocation).
  *   - `buildAndUse` — construct *and* `modifyUnsafe` once (the true one-shot cost).
  *   - `reuseUse` — `modifyUnsafe` through a pre-built prism (the amortized steady state).
  *
  * `reuseUse` ≪ `buildAndUse` means long-lived optics are worth caching; `buildAndUse` ≈ `reuseUse`
  * means construction is negligible and ad-hoc use is fine.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class OpticBuildBench extends JmhDefaults:

  import OrderCirceBench.given

  private val json: Json = Domain.mkOrder(16).asJson

  private val prebuilt: JsonPrism[String] =
    codecPrism[Order].field(_.customer).field(_.address).field(_.street)

  @Benchmark def build: Any =
    codecPrism[Order].field(_.customer).field(_.address).field(_.street)

  @Benchmark def buildAndUse: Json =
    codecPrism[Order].field(_.customer).field(_.address).field(_.street).modifyUnsafe(_.toUpperCase)(json)

  @Benchmark def reuseUse: Json =
    prebuilt.modifyUnsafe(_.toUpperCase)(json)
