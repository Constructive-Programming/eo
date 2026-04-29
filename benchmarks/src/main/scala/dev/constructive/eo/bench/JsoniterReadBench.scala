package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import io.circe.{Codec, Json}
import io.circe.parser.parse as circeParse

import dev.constructive.eo.circe.{JsonPrism, codecPrism}
import dev.constructive.eo.data.{Affine, MultiFocus, PSVec}
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.jsoniter.{JsoniterPrism, JsoniterTraversal}
import dev.constructive.eo.optics.{Optic, Traversal}
import dev.constructive.eo.optics.Optic.*

import hearth.kindlings.circederivation.KindlingsCodecAsObject

/** Phase-1 read-side bench for `eo-jsoniter` vs `eo-circe` on three fixtures:
  *
  *   1. **Hit at depth 3, scalar (`Long`).** `$.payload.user.id` on a synthetic 1 KB JSON.
  *      jsoniter's expected sweet spot — skip the AST, decode just the Long.
  *
  *   2. **Hit at depth 4, scalar (`String`).** `$.payload.user.profile.email`. Same shape one
  *      level deeper to confirm the perf gap doesn't degrade with depth.
  *
  *   3. **Miss at depth 3.** `$.payload.user.absent`. Honest stress test: if the perf advantage
  *      collapses when the path doesn't resolve (because both libraries have to walk most of the
  *      document anyway), the perf story is more nuanced.
  *
  * Each pair compares:
  *   - `j*` — `JsoniterPrism[A].foldMap(identity)(bytes)` — phase-1 read on `Array[Byte]`.
  *   - `c*` — eo-circe `JsonPrism[A].foldMap(identity)(json)` after a `circeParse` to materialise
  *      the AST. The circe side starts from `Array[Byte]` to make the comparison fair: parse +
  *      drill is the realistic eo-circe workflow.
  *
  * Everything else is deliberately the same: same input bytes, same focus type, same Monoid (the
  * focus's identity Monoid).
  *
  * Run:
  * {{{
  *   sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 .*JsoniterReadBench.*"
  * }}}
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class JsoniterReadBench extends JmhDefaults:

  import JsoniterReadBench.{*, given}
  import cats.instances.int.given
  import cats.instances.list.given
  import cats.instances.long.given
  import cats.instances.string.given

  private val bytes: Array[Byte] = sampleBytes

  // ---- jsoniter-scala-side optics --------------------------------------

  private val jIdLong: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
    JsoniterPrism[Long]("$.payload.user.id")

  private val jEmailString: Optic[Array[Byte], Array[Byte], String, String, Affine] =
    JsoniterPrism[String]("$.payload.user.profile.email")

  private val jAbsentLong: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
    JsoniterPrism[Long]("$.payload.user.absent")

  // ---- eo-circe-side optics --------------------------------------------

  private val cIdLong: JsonPrism[Long] =
    codecPrism[Payload].field(_.user).field(_.id)

  private val cEmailString: JsonPrism[String] =
    codecPrism[Payload].field(_.user).field(_.profile).field(_.email)

  // No "absent" eo-circe equivalent because the macro-derived codec
  // wouldn't include a non-existent field; for the miss case we
  // measure circe-parse + foldMap on a path that DOES exist but
  // returns the same scalar — fair-ish comparison.
  private val cIdLongForMiss: JsonPrism[Long] = cIdLong

  // ---- benchmarks: hit @ depth 3, Long ---------------------------------

  @Benchmark def jHitDepth3Long: Long =
    jIdLong.foldMap(identity[Long])(bytes)

  @Benchmark def cHitDepth3Long: Long =
    val json = circeParse(new String(bytes, "UTF-8")).getOrElse(Json.Null)
    cIdLong.foldMap(identity[Long])(json)

  // ---- benchmarks: hit @ depth 4, String -------------------------------

  @Benchmark def jHitDepth4String: String =
    jEmailString.foldMap(identity[String])(bytes)

  @Benchmark def cHitDepth4String: String =
    val json = circeParse(new String(bytes, "UTF-8")).getOrElse(Json.Null)
    cEmailString.foldMap(identity[String])(json)

  // ---- benchmarks: miss @ depth 3 --------------------------------------

  @Benchmark def jMissDepth3: Long =
    jAbsentLong.foldMap(identity[Long])(bytes)

  @Benchmark def cMissDepth3: Long =
    // The miss bench measures path-walk over an already-decoded AST.
    // circe's path drill returns Monoid.empty when the field is missing;
    // we approximate via the same hit path on the circe side
    // (cIdLongForMiss) since deriving an "absent field" path through a
    // typed codec isn't natural — this gives circe its best case for
    // the miss comparison.
    val json = circeParse(new String(bytes, "UTF-8")).getOrElse(Json.Null)
    cIdLongForMiss.foldMap(identity[Long])(json)

  // ---- benchmarks: fold over array of 10 Long elements ------------------
  // Phase-1.5 traversal — sums `$.payload.items[*]`, ten elements,
  // each a single-digit Long.

  private val jItemsSum: Optic[Array[Byte], Array[Byte], Long, Long, MultiFocus[PSVec]] =
    JsoniterTraversal[Long]("$.payload.items[*]")

  // eo-circe peer: focus the items array via codecPrism, then traverse it
  // with the existing Traversal.each over List. This is the realistic
  // eo-circe shape for "fold over an array field".
  private val cItemsSum =
    codecPrism[Payload].field(_.items).andThen(Traversal.each[List, Int])

  @Benchmark def jSumItems: Long =
    jItemsSum.foldMap(identity[Long])(bytes)

  @Benchmark def cSumItems: Int =
    val json = circeParse(new String(bytes, "UTF-8")).getOrElse(Json.Null)
    cItemsSum.foldMap(identity[Int])(json)

object JsoniterReadBench:

  // ---- circe codec fixture ---------------------------------------------
  // eo-circe drills through `codecPrism[Payload].field(_.user).field(_.id)`,
  // which needs a `Codec.AsObject` for every type along the path.

  final case class Profile(email: String, age: Int)
  object Profile:
    given Codec.AsObject[Profile] = KindlingsCodecAsObject.derive

  final case class User(id: Long, profile: Profile, name: String)
  object User:
    given Codec.AsObject[User] = KindlingsCodecAsObject.derive

  final case class Payload(user: User, items: List[Int], tag: String)
  object Payload:
    given Codec.AsObject[Payload] = KindlingsCodecAsObject.derive

  // ---- jsoniter codec fixture ------------------------------------------

  given JsonValueCodec[Long]   = JsonCodecMaker.make
  given JsonValueCodec[String] = JsonCodecMaker.make

  // ---- shared sample bytes ---------------------------------------------

  /** Synthetic JSON document used by every bench fixture. ~250 bytes — large enough that the
    * AST-vs-byte-walk gap is observable, small enough that GC doesn't dominate.
    */
  val sampleBytes: Array[Byte] =
    s"""{"payload":{"user":{"id":42,"profile":{"email":"alice@example.com","age":30},"name":"Alice"},"items":[1,2,3,4,5,6,7,8,9,10],"tag":"hot"}}""".getBytes("UTF-8")
