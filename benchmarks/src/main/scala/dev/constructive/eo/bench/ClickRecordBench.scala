package dev.constructive.eo
package bench

import java.util.concurrent.TimeUnit

import cats.syntax.all.*

import org.openjdk.jmh.annotations.*

import _root_.vulcan.Codec as VCodec

import avro.AvroCodec
import avro.codecPrism
import avro.vulcan.{AvroVulcan, WholeRecordBuilder}
import org.apache.avro.generic.GenericData

/** Whole-record encode shoot-out on the issue-#95 filer's production shape: a ~66-leaf ClickInfo
  * whose leaves live in 5 nested sub-records (Geo 8, UserAgentInfo 9, PostClick 11, Ivt 20,
  * MavenEntities 10) plus 8 root primitives — every leaf a primitive or nullable primitive, so the
  * arms differ ONLY in mechanism:
  *
  *   - '''vulcanFull''' — the full `vulcan.Codec[ClickInfo].encode` (the pre-#95 baseline): pays
  *     vulcan's per-field composition (FreeApplicative analyze, Either + Chain per field,
  *     put-by-name hash probe) once per field PER LEVEL.
  *   - '''positional''' — the whole-record builder over held per-field leaf codecs the filer
  *     implemented and REJECTED: positional puts of `leafCodec.encode(value)`, nested sub-records
  *     through `subCodec.encode` — strips ONE level of composition (the outermost shell) and keeps
  *     paying the rest, which is what measured 7.5x time / 16.9x allocation on their real shape.
  *   - '''hand''' — the leaf-by-leaf `.put(pos, value)` builder the filer hand-maintains today: one
  *     line per leaf across every nesting level.
  *   - '''derived''' — `AvroVulcan.recordBuilder[ClickInfo]`: the compile-time-derived builder —
  *     the hand-built shape with no hand-maintained lines (recursive sub-record levels, primitive
  *     bulk as positional puts).
  *   - '''derivedThroughPrism''' — `codecPrism[ClickInfo].record.reverseGet` over the derived codec
  *     (`asAvroCodec`): the eo-idiomatic surface at the call site, which is how the filer wires it.
  *
  * '''B/op (`-prof gc`) is the gate; ns/op advises''' (project doctrine, and these boxes are
  * noisy). The gate the filer set: derived ≈ hand (both put raw values; String-emitting, Utf8
  * materialises at serialise time), positional ≈ vulcan-per-sub-record.
  */
object ClickRecordImpls:

  final case class Geo(
      country: String,
      region: String,
      city: String,
      postalCode: String,
      latitude: Double,
      longitude: Double,
      accuracyM: Long,
      geoHash: String,
  )

  object Geo:

    given VCodec[Geo] = VCodec.record(name = "Geo", namespace = "dev.constructive.eo.bench") { fb =>
      (
        fb("country", _.country),
        fb("region", _.region),
        fb("city", _.city),
        fb("postalCode", _.postalCode),
        fb("latitude", _.latitude),
        fb("longitude", _.longitude),
        fb("accuracyM", _.accuracyM),
        fb("geoHash", _.geoHash),
      ).mapN(Geo.apply)
    }

  final case class UserAgentInfo(
      browser: String,
      browserVersion: String,
      os: String,
      osVersion: String,
      device: String,
      language: String,
      userAgentRaw: String,
      doNotTrack: Boolean,
      robot: Boolean,
  )

  object UserAgentInfo:

    given VCodec[UserAgentInfo] = VCodec.record(
      name = "UserAgentInfo",
      namespace = "dev.constructive.eo.bench",
    ) { fb =>
      (
        fb("browser", _.browser),
        fb("browserVersion", _.browserVersion),
        fb("os", _.os),
        fb("osVersion", _.osVersion),
        fb("device", _.device),
        fb("language", _.language),
        fb("userAgentRaw", _.userAgentRaw),
        fb("doNotTrack", _.doNotTrack),
        fb("robot", _.robot),
      ).mapN(UserAgentInfo.apply)
    }

  final case class PostClick(
      conversionTimestamp: Long,
      orderId: Option[String],
      revenue: Double,
      currency: String,
      funnelStep: Int,
      completed: Boolean,
      attributionWindow: Option[Long],
      landingPage: String,
      referrer: String,
      campaignId: String,
      channel: String,
  )

  object PostClick:

    given VCodec[PostClick] = VCodec.record(
      name = "PostClick",
      namespace = "dev.constructive.eo.bench",
    ) { fb =>
      (
        fb("conversionTimestamp", _.conversionTimestamp),
        fb("orderId", _.orderId),
        fb("revenue", _.revenue),
        fb("currency", _.currency),
        fb("funnelStep", _.funnelStep),
        fb("completed", _.completed),
        fb("attributionWindow", _.attributionWindow),
        fb("landingPage", _.landingPage),
        fb("referrer", _.referrer),
        fb("campaignId", _.campaignId),
        fb("channel", _.channel),
      ).mapN(PostClick.apply)
    }

  final case class Ivt(
      ivtGeneralInvalid: Boolean,
      ivtSophisticated: Boolean,
      ivtBoth: Boolean,
      ivtGenuine: Boolean,
      ivtUndetermined: Boolean,
      ivtScore: Int,
      ivtTag1: Int,
      ivtTag2: Int,
      ivtTag3: Int,
      ivtTag4: Int,
      ivtTag5: Int,
      ivtTag6: Int,
      ivtTag7: Int,
      ivtTag8: Int,
      ivtTag9: Int,
      ivtTag10: Int,
      ivtFlags: Long,
      ivtSchemaVersion: String,
      ivtVendor: String,
      ivtNote: String,
  )

  object Ivt:

    given VCodec[Ivt] = VCodec.record(
      name = "Ivt",
      namespace = "dev.constructive.eo.bench",
    ) { fb =>
      (
        fb("ivtGeneralInvalid", _.ivtGeneralInvalid),
        fb("ivtSophisticated", _.ivtSophisticated),
        fb("ivtBoth", _.ivtBoth),
        fb("ivtGenuine", _.ivtGenuine),
        fb("ivtUndetermined", _.ivtUndetermined),
        fb("ivtScore", _.ivtScore),
        fb("ivtTag1", _.ivtTag1),
        fb("ivtTag2", _.ivtTag2),
        fb("ivtTag3", _.ivtTag3),
        fb("ivtTag4", _.ivtTag4),
        fb("ivtTag5", _.ivtTag5),
        fb("ivtTag6", _.ivtTag6),
        fb("ivtTag7", _.ivtTag7),
        fb("ivtTag8", _.ivtTag8),
        fb("ivtTag9", _.ivtTag9),
        fb("ivtTag10", _.ivtTag10),
        fb("ivtFlags", _.ivtFlags),
        fb("ivtSchemaVersion", _.ivtSchemaVersion),
        fb("ivtVendor", _.ivtVendor),
        fb("ivtNote", _.ivtNote),
      ).mapN(Ivt.apply)
    }

  final case class MavenEntities(
      entityCount: Int,
      sessionEntities: String,
      mavenSegment: String,
      taxonomyVersion: String,
      audienceId: String,
      viewability: Double,
      engagement: Float,
      mrcViewable: Boolean,
      dwellMs: Long,
      qualified: Boolean,
  )

  object MavenEntities:

    given VCodec[MavenEntities] = VCodec.record(
      name = "MavenEntities",
      namespace = "dev.constructive.eo.bench",
    ) { fb =>
      (
        fb("entityCount", _.entityCount),
        fb("sessionEntities", _.sessionEntities),
        fb("mavenSegment", _.mavenSegment),
        fb("taxonomyVersion", _.taxonomyVersion),
        fb("audienceId", _.audienceId),
        fb("viewability", _.viewability),
        fb("engagement", _.engagement),
        fb("mrcViewable", _.mrcViewable),
        fb("dwellMs", _.dwellMs),
        fb("qualified", _.qualified),
      ).mapN(MavenEntities.apply)
    }

  final case class ClickInfo(
      clickId: String,
      servedAt: Long,
      sessionId: String,
      geo: Geo,
      userAgent: UserAgentInfo,
      postClick: PostClick,
      ivt: Ivt,
      mavenEntities: MavenEntities,
      valid: Boolean,
      clickTimestamp: Option[Long],
      sourceIp: String,
      ttlMs: Long,
      retained: Boolean,
  )

  object ClickInfo:

    given VCodec[ClickInfo] = VCodec.record(
      name = "ClickInfo",
      namespace = "dev.constructive.eo.bench",
    ) { fb =>
      (
        fb("clickId", _.clickId),
        fb("servedAt", _.servedAt),
        fb("sessionId", _.sessionId),
        fb("geo", _.geo),
        fb("userAgent", _.userAgent),
        fb("postClick", _.postClick),
        fb("ivt", _.ivt),
        fb("mavenEntities", _.mavenEntities),
        fb("valid", _.valid),
        fb("clickTimestamp", _.clickTimestamp),
        fb("sourceIp", _.sourceIp),
        fb("ttlMs", _.ttlMs),
        fb("retained", _.retained),
      ).mapN(ClickInfo.apply)
    }

  val click = ClickInfo(
    clickId = "ck_9f2b7c31",
    servedAt = 1726000000123L,
    sessionId = "s_8814d0aa",
    geo = Geo("US", "NY", "New York", "10001", 40.7128, -74.006, 25L, "dr5regw3"),
    userAgent = UserAgentInfo(
      "Firefox",
      "119.0",
      "Linux",
      "6.6.0",
      "desktop",
      "en-US",
      "Mozilla/5.0 (X11; Linux x86_64) Gecko/20100101 Firefox/119.0",
      doNotTrack = true,
      robot = false,
    ),
    postClick = PostClick(
      1726000001456L,
      Some("ord_4417"),
      129.99,
      "USD",
      3,
      completed = true,
      Some(7L),
      "/lp/spring",
      "google",
      "cmp_2281",
      "cpc",
    ),
    ivt = Ivt(
      false,
      false,
      false,
      true,
      false,
      87,
      1,
      0,
      2,
      0,
      0,
      1,
      0,
      0,
      3,
      0,
      42L,
      "v2",
      "acme",
      "clean",
    ),
    mavenEntities = MavenEntities(
      12,
      "se1,se2,se3",
      "seg_a",
      "tax7",
      "aud_991",
      0.87,
      0.5f,
      true,
      3400L,
      true,
    ),
    valid = true,
    clickTimestamp = Some(1726000000500L),
    sourceIp = "203.0.113.9",
    ttlMs = 86400000L,
    retained = true,
  )

  val schema = summon[VCodec[ClickInfo]].schema.toOption.get

  /** The filer's REJECTED prototype: every case field's leaf codec resolved once at init, encode =
    * N positional puts of `codec.encode(value)`, nested sub-records through `subCodec.encode` — one
    * level of composition stripped, the rest paid.
    */
  final class PositionalBuilder(schema: org.apache.avro.Schema):
    private val cString = VCodec.string
    private val cLong = VCodec.long
    private val cBool = VCodec.boolean
    private val cGeo = summon[VCodec[Geo]]
    private val cUa = summon[VCodec[UserAgentInfo]]
    private val cPc = summon[VCodec[PostClick]]
    private val cIvt = summon[VCodec[Ivt]]
    private val cMaven = summon[VCodec[MavenEntities]]

    private def enc[A](c: VCodec[A], a: A): Any = c.encode(a).fold(e => throw e.throwable, identity)

    def toRecord(a: ClickInfo): GenericData.Record =
      val r = new GenericData.Record(schema)
      r.put(0, enc(cString, a.clickId))
      r.put(1, enc(cLong, a.servedAt))
      r.put(2, enc(cString, a.sessionId))
      r.put(3, enc(cGeo, a.geo))
      r.put(4, enc(cUa, a.userAgent))
      r.put(5, enc(cPc, a.postClick))
      r.put(6, enc(cIvt, a.ivt))
      r.put(7, enc(cMaven, a.mavenEntities))
      r.put(8, enc(cBool, a.valid))
      a.clickTimestamp match
        case Some(ts) => r.put(9, enc(cLong, ts))
        case None     => r.put(9, null)
      r.put(10, enc(cString, a.sourceIp))
      r.put(11, enc(cLong, a.ttlMs))
      r.put(12, enc(cBool, a.retained))
      r

  /** The hand-maintained builder: one line per leaf, every level, raw values. */
  final class HandBuilder(schema: org.apache.avro.Schema):

    def toRecord(a: ClickInfo): GenericData.Record =
      val r = new GenericData.Record(schema)
      r.put(0, a.clickId)
      r.put(1, a.servedAt)
      r.put(2, a.sessionId)
      r.put(3, geoRecord(a.geo))
      r.put(4, uaRecord(a.userAgent))
      r.put(5, postClickRecord(a.postClick))
      r.put(6, ivtRecord(a.ivt))
      r.put(7, mavenRecord(a.mavenEntities))
      r.put(8, a.valid)
      a.clickTimestamp match
        case Some(ts) => r.put(9, ts)
        case None     => r.put(9, null)
      r.put(10, a.sourceIp)
      r.put(11, a.ttlMs)
      r.put(12, a.retained)
      r

    private def geoRecord(g: Geo): GenericData.Record =
      val r = new GenericData.Record(summon[VCodec[Geo]].schema.toOption.get)
      r.put(0, g.country)
      r.put(1, g.region)
      r.put(2, g.city)
      r.put(3, g.postalCode)
      r.put(4, g.latitude)
      r.put(5, g.longitude)
      r.put(6, g.accuracyM)
      r.put(7, g.geoHash)
      r

    private def uaRecord(u: UserAgentInfo): GenericData.Record =
      val r = new GenericData.Record(summon[VCodec[UserAgentInfo]].schema.toOption.get)
      r.put(0, u.browser)
      r.put(1, u.browserVersion)
      r.put(2, u.os)
      r.put(3, u.osVersion)
      r.put(4, u.device)
      r.put(5, u.language)
      r.put(6, u.userAgentRaw)
      r.put(7, u.doNotTrack)
      r.put(8, u.robot)
      r

    private def postClickRecord(p: PostClick): GenericData.Record =
      val r = new GenericData.Record(summon[VCodec[PostClick]].schema.toOption.get)
      r.put(0, p.conversionTimestamp)
      p.orderId match
        case Some(o) => r.put(1, o)
        case None    => r.put(1, null)
      r.put(2, p.revenue)
      r.put(3, p.currency)
      r.put(4, p.funnelStep)
      r.put(5, p.completed)
      p.attributionWindow match
        case Some(w) => r.put(6, w)
        case None    => r.put(6, null)
      r.put(7, p.landingPage)
      r.put(8, p.referrer)
      r.put(9, p.campaignId)
      r.put(10, p.channel)
      r

    private def ivtRecord(i: Ivt): GenericData.Record =
      val r = new GenericData.Record(summon[VCodec[Ivt]].schema.toOption.get)
      r.put(0, i.ivtGeneralInvalid)
      r.put(1, i.ivtSophisticated)
      r.put(2, i.ivtBoth)
      r.put(3, i.ivtGenuine)
      r.put(4, i.ivtUndetermined)
      r.put(5, i.ivtScore)
      r.put(6, i.ivtTag1)
      r.put(7, i.ivtTag2)
      r.put(8, i.ivtTag3)
      r.put(9, i.ivtTag4)
      r.put(10, i.ivtTag5)
      r.put(11, i.ivtTag6)
      r.put(12, i.ivtTag7)
      r.put(13, i.ivtTag8)
      r.put(14, i.ivtTag9)
      r.put(15, i.ivtTag10)
      r.put(16, i.ivtFlags)
      r.put(17, i.ivtSchemaVersion)
      r.put(18, i.ivtVendor)
      r.put(19, i.ivtNote)
      r

    private def mavenRecord(m: MavenEntities): GenericData.Record =
      val r = new GenericData.Record(summon[VCodec[MavenEntities]].schema.toOption.get)
      r.put(0, m.entityCount)
      r.put(1, m.sessionEntities)
      r.put(2, m.mavenSegment)
      r.put(3, m.taxonomyVersion)
      r.put(4, m.audienceId)
      r.put(5, m.viewability)
      r.put(6, m.engagement)
      r.put(7, m.mrcViewable)
      r.put(8, m.dwellMs)
      r.put(9, m.qualified)
      r

  val vulcanCodec: VCodec[ClickInfo] = summon[VCodec[ClickInfo]]
  val positional: PositionalBuilder = PositionalBuilder(schema)
  val hand: HandBuilder = HandBuilder(schema)

  /** The derived builder (construction unwrapped — the once-cost is not the hot path). */
  val derived: WholeRecordBuilder[ClickInfo] =
    AvroVulcan.recordBuilder[ClickInfo] match
      case b: WholeRecordBuilder[ClickInfo] => b
      case e: Exception                     => throw e

  /** The derived builder installed as the AvroCodec encode — the filer's wiring. */
  val derivedCodec: AvroCodec[ClickInfo] = derived.asAvroCodec
  val derivedRoot = codecPrism[ClickInfo](using derivedCodec)

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class ClickRecordBench extends JmhDefaults:

  import ClickRecordImpls.*

  @Benchmark def encode_vulcanFull: Any = vulcanCodec.encode(click)

  @Benchmark def encode_positional: GenericData.Record = positional.toRecord(click)

  @Benchmark def encode_hand: GenericData.Record = hand.toRecord(click)

  @Benchmark def encode_derived: GenericData.Record = derived.toRecord(click)

  @Benchmark def encode_derivedThroughPrism: Any = derivedRoot.record.reverseGet(click)
