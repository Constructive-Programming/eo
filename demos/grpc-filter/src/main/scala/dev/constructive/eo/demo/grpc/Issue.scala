package dev.constructive.eo.demo.grpc

import scala.util.control.NonFatal

import com.github.plokhotnyuk.jsoniter_scala.core.{
  readFromSubArray,
  JsonReader,
  JsonValueCodec,
  JsonWriter
}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import dev.constructive.eo.jsoniter.JsonSlice

/** One message of the stream: an issue, as an issue tracker publishes it. 64 fields. The 61st,
  * `detail`, is one of three kinds of issue, each with its own fields, nested up to three objects
  * deep (`detail` → `impact` → `service`). The demo never builds one of these from the wire: the
  * source encodes them, and the tests and benchmarks decode them as the baseline the optics are
  * checked and measured against.
  */
final case class Issue(
    id: Long,
    key: String,
    project: String,
    title: String,
    description: String,
    status: Status,
    priority: Priority,
    resolution: Option[String],
    reporter: Person,
    assignee: Option[Person],
    watchers: List[String],
    createdAt: Long,
    updatedAt: Long,
    resolvedAt: Option[Long],
    dueAt: Option[Long],
    estimateMinutes: Int,
    spentMinutes: Int,
    storyPoints: Double,
    sprint: String,
    epic: String,
    fixVersion: String,
    affectedVersions: List[String],
    components: List[String],
    labels: List[String],
    rank: Long,
    parentId: Option[Long],
    subtaskIds: List[Long],
    links: List[Link],
    attachments: List[Attachment],
    environment: Environment,
    browser: String,
    locale: String,
    timezone: String,
    region: String,
    tenantId: String,
    customerId: String,
    source: String,
    channel: String,
    externalUrl: String,
    commitSha: String,
    branch: String,
    buildNumber: Int,
    pipelineId: Long,
    views: Int,
    commentCount: Int,
    reopenCount: Int,
    duplicateOf: Option[Long],
    confidential: Boolean,
    security: Boolean,
    flagged: Boolean,
    archived: Boolean,
    slaPolicy: String,
    slaBreached: Boolean,
    cost: Double,
    currency: String,
    score: Double,
    checksum: String,
    schemaVersion: Int,
    traceId: String,
    spanId: String,
    detail: Detail,
    tags: Map[String, String],
    checklist: List[CheckItem],
    revision: Long,
)

object Issue:
  /** The whole message, every field: what the source writes and the baseline reads. */
  given JsonValueCodec[Issue] = JsonCodecMaker.make

  /** The whole message decoded, if it decodes. jsoniter-scala reports bytes that don't decode by
    * throwing: this is the one place that becomes a value.
    */
  def decode(message: JsonSlice): Option[Issue] =
    try Some(readFromSubArray[Issue](message.array, message.offset, message.until))
    catch case NonFatal(_) => None

/** The three kinds of issue. On the wire, `"type"` names the case: jsoniter-scala writes it as the
  * object's first field.
  */
enum Detail:
  case Bug(component: String, build: String, reproducible: Boolean, trace: Trace)
  case Incident(severity: Severity, startedAt: Long, mitigated: Boolean, impact: Impact)
  case Request(product: String, votes: Votes, quarter: String, requester: Customer)

object Detail:
  given JsonValueCodec[Detail] = JsonCodecMaker.make

/** Which of the three kinds a message is: the classification. Spelled as `Detail`'s cases, so it
  * decodes straight from the `"type"` field.
  */
enum Kind:
  case Bug, Incident, Request

object Kind:
  given JsonValueCodec[Kind] = JsonCodecMaker.make(asString)

  def of(detail: Detail): Kind = detail match
    case _: Detail.Bug      => Bug
    case _: Detail.Incident => Incident
    case _: Detail.Request  => Request

enum Status:
  case Open, Triaged, InProgress, Blocked, Resolved, Closed

object Status:
  given JsonValueCodec[Status] = JsonCodecMaker.make(asString)

enum Priority:
  case P0, P1, P2, P3, P4

object Priority:
  given JsonValueCodec[Priority] = JsonCodecMaker.make(asString)

enum Severity:
  case Sev1, Sev2, Sev3, Sev4

object Severity:
  given JsonValueCodec[Severity] = JsonCodecMaker.make(asString)

/** How much depends on a service. */
enum Tier:
  case Critical, Standard, Internal

object Tier:
  given JsonValueCodec[Tier] = JsonCodecMaker.make(asString)

/** A customer's plan. */
enum Plan:
  case Free, Team, Enterprise

object Plan:
  given JsonValueCodec[Plan] = JsonCodecMaker.make(asString)

/** Codec settings for an enum without fields: its case's name, as a JSON string. */
private inline def asString: CodecMakerConfig = CodecMakerConfig.withDiscriminatorFieldName(None)

/** The code module a stack frame is in. */
opaque type Module = String

object Module:
  def apply(name: String): Module = name

  extension (module: Module) def name: String = module

  given JsonValueCodec[Module] = new JsonValueCodec[Module]:
    def decodeValue(in: JsonReader, default: Module): Module = in.readString(default)
    def encodeValue(module: Module, out: JsonWriter): Unit = out.writeVal(module)
    def nullValue: Module = null.asInstanceOf[Module]

/** How many customers asked for a feature. */
opaque type Votes = Int

object Votes:
  def apply(count: Int): Votes = count

  extension (votes: Votes) def count: Int = votes

  given JsonValueCodec[Votes] = new JsonValueCodec[Votes]:
    def decodeValue(in: JsonReader, default: Votes): Votes = in.readInt()
    def encodeValue(votes: Votes, out: JsonWriter): Unit = out.writeVal(votes)
    def nullValue: Votes = 0

final case class Person(name: String, email: String, team: Team)
final case class Team(name: String, lead: String, org: Org)
final case class Org(name: String, region: String, costCenter: String)
final case class Link(kind: String, target: String)
final case class Attachment(name: String, bytes: Long, mime: String)
final case class Environment(os: String, arch: String, runtime: Runtime)
final case class Runtime(name: String, version: String)
final case class CheckItem(text: String, done: Boolean)

final case class Trace(exception: String, message: String, frames: List[Frame])
final case class Frame(module: Module, file: String, line: Int, function: String)
final case class Impact(users: Long, errorRate: Double, service: Service)
final case class Service(name: String, tier: Tier, region: String)
final case class Customer(name: String, seats: Int, contract: Contract)
final case class Contract(plan: Plan, renewsAt: Long, value: Double)
