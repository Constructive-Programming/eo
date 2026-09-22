package dev.constructive.eo.avro.vulcan

import java.time.Instant

import cats.syntax.all.*
import _root_.vulcan.Codec as VCodec

// ---- Top-level so the vulcan record codecs and the derived builders see plain classfiles. ----

enum TrafficClass:
  case Organic, Paid, Social, Referral

object TrafficClass:

  given VCodec[TrafficClass] = VCodec.enumeration(
    name = "TrafficClass",
    namespace = "dev.constructive.eo.avro.vulcan",
    symbols = Seq("Organic", "Paid", "Social", "Referral"),
    encode = _.toString,
    decode = symbol => Right(TrafficClass.valueOf(symbol)),
  )

final case class Geo(
    country: String,
    region: String,
    city: String,
    latitude: Double,
    longitude: Double
)

object Geo:

  given VCodec[Geo] = VCodec.record(name = "Geo", namespace = "dev.constructive.eo.avro.vulcan") {
    fb =>
      (
        fb("country", _.country),
        fb("region", _.region),
        fb("city", _.city),
        fb("latitude", _.latitude),
        fb("longitude", _.longitude),
      ).mapN(Geo.apply)
  }

final case class UserAgentInfo(
    browser: String,
    browserVersion: String,
    os: String,
    device: String,
    language: String,
    doNotTrack: Boolean,
    robot: Boolean,
)

object UserAgentInfo:

  given VCodec[UserAgentInfo] = VCodec.record(
    name = "UserAgentInfo",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    (
      fb("browser", _.browser),
      fb("browserVersion", _.browserVersion),
      fb("os", _.os),
      fb("device", _.device),
      fb("language", _.language),
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
)

object PostClick:

  given VCodec[PostClick] = VCodec.record(
    name = "PostClick",
    namespace = "dev.constructive.eo.avro.vulcan",
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
    ).mapN(PostClick.apply)
  }

/** Twenty same-flavoured flags — the filer's "Ivt alone has 20 fields" leg. */
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
    namespace = "dev.constructive.eo.avro.vulcan",
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
    pixelHash: Array[Byte],
    entityCount: Int,
    sessionEntities: String,
    mavenSegment: String,
    taxonomyVersion: String,
    viewability: Double,
    engagement: Float,
    mrcViewable: Boolean,
    trafficClass: TrafficClass,
)

object MavenEntities:

  given VCodec[MavenEntities] = VCodec.record(
    name = "MavenEntities",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    (
      fb("pixelHash", _.pixelHash),
      fb("entityCount", _.entityCount),
      fb("sessionEntities", _.sessionEntities),
      fb("mavenSegment", _.mavenSegment),
      fb("taxonomyVersion", _.taxonomyVersion),
      fb("viewability", _.viewability),
      fb("engagement", _.engagement),
      fb("mrcViewable", _.mrcViewable),
      fb("trafficClass", _.trafficClass),
    ).mapN(MavenEntities.apply)
  }

/** The filer's ClickInfo shape, scaled to the fixtures: nested sub-records, a nullable sub-record,
  * nullable primitives, a logical-type leaf (Instant) and enum / bytes leaves — every arm of the
  * builder's dispatch table in one wire record.
  */
final case class ClickInfo(
    clickId: String,
    servedAt: Instant,
    sessionId: String,
    geo: Geo,
    userAgent: UserAgentInfo,
    postClick: Option[PostClick],
    ivt: Ivt,
    mavenEntities: MavenEntities,
    valid: Boolean,
    clickTimestamp: Option[Long],
)

object ClickInfo:

  given VCodec[ClickInfo] = VCodec.record(
    name = "ClickInfo",
    namespace = "dev.constructive.eo.avro.vulcan",
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
    ).mapN(ClickInfo.apply)
  }

/** Codec whose field list is REORDERED against the case class — name resolution must map by NAME,
  * never by declaration index (issue #105's doctrine, on the build side).
  */
final case class Reordered(alpha: Int, beta: String)

object Reordered:

  given VCodec[Reordered] = VCodec.record(
    name = "Reordered",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    (fb("beta", _.beta), fb("alpha", _.alpha)).mapN((beta, alpha) => Reordered(alpha, beta))
  }

/** Codec whose schema names are snake_cased against the case class — the normalised-name rung. */
final case class Snakey(userId: Int, userName: String)

object Snakey:

  given VCodec[Snakey] = VCodec.record(
    name = "Snakey",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    (fb("user_id", _.userId), fb("user_name", _.userName)).mapN(Snakey.apply)
  }

/** A schema-only computed column: `derived` has no case field; the codec fills it on encode, the
  * builder leaves it at its in-record default, and the codec's decode ignores it.
  */
final case class WithComputed(base: Int)

object WithComputed:

  given VCodec[WithComputed] = VCodec.record(
    name = "WithComputed",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    // `derived` is a schema-only column: nullable (the accessor's Option summons the union codec),
    // so the builder's null default stays decodable and round-trips.
    (fb("base", _.base), fb("derived", (c: WithComputed) => Some(c.base * 2): Option[Int]))
      .mapN((base, _) => WithComputed(base))
  }

/** A leaf type with NO vulcan codec in scope — the macro's missing-leaf refusal. */
final class NoCodecLeaf(val s: String)

final case class WithNoCodec(name: String, opaque: NoCodecLeaf)

object WithNoCodec:

  // The ROOT codec exists; the case-class field `opaque` has none — the macro must be the one to
  // refuse it.
  given VCodec[WithNoCodec] = VCodec.record(
    name = "WithNoCodec",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    fb("name", _.name).map(n => WithNoCodec(n, NoCodecLeaf("")))
  }

// ---- Construction-failure fixtures ---------------------------------------------------------------

/** `beta`'s column is renamed `zeta` — beyond normalisation, so the builder must refuse. */
final case class Renamed(alpha: Int, beta: String)

object Renamed:

  given VCodec[Renamed] = VCodec.record(
    name = "Renamed",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    (fb("alpha", _.alpha), fb("zeta", _.beta)).mapN((alpha, _) => Renamed(alpha, ""))
  }

/** `Option` case field over a NON-nullable schema column — the codec lies about optionality. */
final case class OptMismatch(x: Option[Int])

object OptMismatch:

  given VCodec[OptMismatch] = VCodec.record(
    name = "OptMismatch",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    fb("x", _.x.getOrElse(0)).map(x => OptMismatch(Some(x)))
  }

/** Case-class case field whose column is a STRING — the codec flattens it; no record to recurse. */
final case class FlatInner(v: Int)

object FlatInner:

  given VCodec[FlatInner] = VCodec.record(
    name = "FlatInner",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    fb("v", _.v).map(FlatInner.apply)
  }

final case class RecMis(inner: FlatInner)

object RecMis:

  given VCodec[RecMis] = VCodec.record(
    name = "RecMis",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    fb("inner", _.inner.v.toString).map(s => RecMis(FlatInner(s.length)))
  }

/** LONG case field over an INT column — the builder never widens silently. */
final case class LongField(x: Long)

object LongField:

  given VCodec[LongField] = VCodec.record(
    name = "LongField",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    fb("x", _.x.toInt).map(x => LongField(x.toLong))
  }

/** One schema column reachable by two normalised spellings — the ambiguity sentinel must refuse. */
final case class Ambig(USERID: Int, other: String)

object Ambig:

  given VCodec[Ambig] = VCodec.record(
    name = "Ambig",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    (fb("userId", _.USERID), fb("user_id", _.USERID), fb("other", _.other))
      .mapN((u, _, o) => Ambig(u, o))
  }

/** Two case fields claiming ONE schema column — the injectivity half of all-or-nothing. */
final case class Collide(aCol: Int, a_col: Int)

object Collide:

  given VCodec[Collide] = VCodec.record(
    name = "Collide",
    namespace = "dev.constructive.eo.avro.vulcan",
  ) { fb =>
    (fb("a_col", _.aCol), fb("b", _.a_col)).mapN((a, _) => Collide(a, a))
  }

/** Self-recursive shape for the runtime knot — derived via a HAND-AUTHORED shape (derive needs no
  * codec, and vulcan's eager record codec cannot reference itself from a plain given).
  */
final case class Node(value: Int, next: Option[Node])
