package dev.constructive.eo.demo.grpc

import scala.util.Random

/** A deterministic supply of issues, the stream's content: the same seed gives the same issues. A
  * third of them are of each kind. Text fields carry the characters JSON must escape (quotes,
  * backslashes, line breaks) and non-ASCII text, so whatever reads the bytes meets them.
  */
object Issues:

  /** Issues `1, 2, 3, …` drawn from `seed`. */
  def apply(seed: Long): Iterator[Issue] =
    val rng = new Random(seed)
    Iterator.iterate(1L)(_ + 1).map(issue(rng, _))

  /** The modules a stack frame can be in. */
  val modules: Vector[Module] =
    spaced(
      "payments ledger checkout search auth notifications catalog inventory shipping analytics"
    ).map(Module(_))

  def issue(rng: Random, id: Long): Issue =
    val created = 1_760_000_000_000L + rng.nextLong(100_000_000_000L)
    Issue(
      id = id,
      key = s"${pick(rng, projects)}-$id",
      project = pick(rng, projects),
      title = text(rng, 4, 12),
      description = text(rng, 60, 250),
      status = pick(rng, Status.values),
      priority = pick(rng, Priority.values),
      resolution = some(rng)(pick(rng, resolutions)),
      reporter = person(rng),
      assignee = some(rng)(person(rng)),
      watchers = several(rng, 6)(email(rng)),
      createdAt = created,
      updatedAt = created + rng.nextInt(1_000_000_000),
      resolvedAt = some(rng)(created + rng.nextInt(2_000_000_000)),
      dueAt = some(rng)(created + rng.nextInt(2_000_000_000)),
      estimateMinutes = rng.nextInt(10_000),
      spentMinutes = rng.nextInt(10_000),
      storyPoints = pick(rng, Vector(0.5, 1.0, 2.0, 3.0, 5.0, 8.0, 13.0)),
      sprint = s"Sprint ${rng.nextInt(200)}",
      epic = text(rng, 2, 5),
      fixVersion = version(rng),
      affectedVersions = several(rng, 4)(version(rng)),
      components = several(rng, 4)(pick(rng, components)),
      labels = several(rng, 8)(pick(rng, words)),
      rank = rng.nextLong(),
      parentId = some(rng)(rng.nextLong(1_000_000)),
      subtaskIds = several(rng, 6)(rng.nextLong(1_000_000)),
      links =
        several(rng, 4)(Link(pick(rng, linkKinds), s"https://tracker.example/${rng.nextInt()}")),
      attachments = several(rng, 3)(
        Attachment(s"${pick(rng, words)}.log", rng.nextLong(10_000_000), "text/plain")
      ),
      environment = Environment(
        pick(rng, Vector("linux", "macos", "windows", "android", "ios")),
        pick(rng, Vector("x86_64", "aarch64")),
        Runtime(pick(rng, Vector("jvm", "node", "python", "go")), version(rng)),
      ),
      browser = pick(rng, Vector("firefox", "chrome", "safari", "edge")),
      locale = pick(rng, Vector("en-US", "de-DE", "ja-JP", "es-AR", "fr-FR")),
      timezone = pick(rng, Vector("UTC", "Europe/Berlin", "Asia/Tokyo", "America/Buenos_Aires")),
      region = pick(rng, regions),
      tenantId = hex(rng, 16),
      customerId = hex(rng, 12),
      source = pick(rng, Vector("web", "email", "api", "monitor", "chat")),
      channel = pick(rng, Vector("support", "engineering", "sales", "security")),
      externalUrl = s"https://support.example/tickets/${rng.nextInt(1_000_000)}",
      commitSha = hex(rng, 40),
      branch = s"feature/${pick(rng, words)}-${rng.nextInt(1000)}",
      buildNumber = rng.nextInt(100_000),
      pipelineId = rng.nextLong(1_000_000_000L),
      views = rng.nextInt(10_000),
      commentCount = rng.nextInt(200),
      reopenCount = rng.nextInt(5),
      duplicateOf = Option.when(rng.nextInt(10) == 0)(rng.nextLong(1_000_000)),
      confidential = rng.nextBoolean(),
      security = rng.nextInt(10) == 0,
      flagged = rng.nextBoolean(),
      archived = rng.nextInt(20) == 0,
      slaPolicy = pick(rng, Vector("gold", "silver", "bronze")),
      slaBreached = rng.nextInt(8) == 0,
      cost = rng.nextDouble() * 10_000,
      currency = pick(rng, Vector("USD", "EUR", "JPY", "ARS")),
      score = rng.nextDouble(),
      checksum = hex(rng, 64),
      schemaVersion = 7,
      traceId = hex(rng, 32),
      spanId = hex(rng, 16),
      detail = detail(rng),
      tags = Map.from(several(rng, 5)(pick(rng, words) -> pick(rng, words))),
      checklist = several(rng, 5)(CheckItem(text(rng, 2, 8), rng.nextBoolean())),
      revision = rng.nextInt(1000).toLong,
    )

  def detail(rng: Random): Detail = rng.nextInt(3) match
    case 0 =>
      Detail.Bug(
        component = pick(rng, components),
        build = version(rng),
        reproducible = rng.nextBoolean(),
        trace = Trace(
          exception = pick(rng, exceptions),
          message = text(rng, 3, 12),
          // Sometimes no frames at all: then the policy has no module to look at.
          frames = List.fill(rng.nextInt(12))(
            Frame(
              pick(rng, modules),
              s"${pick(rng, words)}.scala",
              rng.nextInt(2000),
              s"${pick(rng, words)}$$${rng.nextInt(10)}",
            )
          ),
        ),
      )
    case 1 =>
      Detail.Incident(
        severity = pick(rng, Severity.values),
        startedAt = 1_760_000_000_000L + rng.nextLong(100_000_000_000L),
        mitigated = rng.nextBoolean(),
        impact = Impact(
          users = rng.nextLong(1_000_000),
          errorRate = rng.nextDouble(),
          service = Service(pick(rng, services), pick(rng, Tier.values), pick(rng, regions)),
        ),
      )
    case _ =>
      Detail.Request(
        product = pick(rng, components),
        votes = Votes(rng.nextInt(200)),
        quarter = s"${2026 + rng.nextInt(2)}-Q${1 + rng.nextInt(4)}",
        requester = Customer(
          name = text(rng, 1, 3),
          seats = rng.nextInt(5000),
          contract = Contract(
            pick(rng, Plan.values),
            1_760_000_000_000L + rng.nextLong(100_000_000_000L),
            rng.nextDouble() * 1_000_000,
          ),
        ),
      )

  private def person(rng: Random): Person =
    Person(
      text(rng, 2, 3),
      email(rng),
      Team(
        s"team-${pick(rng, words)}",
        email(rng),
        Org(
          pick(rng, Vector("Acme", "Initech", "Globex", "Umbrella")),
          pick(rng, regions),
          hex(rng, 6)
        ),
      ),
    )

  private def email(rng: Random): String = s"${pick(rng, words)}.${rng.nextInt(1000)}@example.com"

  private def version(rng: Random): String =
    s"${rng.nextInt(10)}.${rng.nextInt(30)}.${rng.nextInt(100)}"

  private def hex(rng: Random, digits: Int): String =
    List.fill(digits)("0123456789abcdef".charAt(rng.nextInt(16))).mkString

  /** `min` to `max` words; about one in twenty is text JSON has to escape, or non-ASCII. */
  private def text(rng: Random, min: Int, max: Int): String =
    List
      .fill(min + rng.nextInt(max - min + 1))(
        if rng.nextInt(20) == 0 then pick(rng, unusual) else pick(rng, words)
      )
      .mkString(" ")

  private def spaced(words: String): Vector[String] = words.split(' ').toVector

  private def pick[A](rng: Random, as: collection.IndexedSeq[A]): A = as(rng.nextInt(as.length))
  private def some[A](rng: Random)(a: => A): Option[A] = Option.when(rng.nextBoolean())(a)

  private def several[A](rng: Random, max: Int)(a: => A): List[A] =
    List.fill(rng.nextInt(max + 1))(a)

  private val projects = Vector("PAY", "LED", "WEB", "OPS", "SEC", "MOB")
  private val resolutions = Vector("fixed", "won't fix", "duplicate", "cannot reproduce")
  private val linkKinds = Vector("blocks", "relates", "duplicates", "causes")
  private val regions = Vector("us-east-1", "eu-west-1", "ap-northeast-1", "sa-east-1")
  private val services = Vector("api-gateway", "payments-core", "ledger-db", "search-index", "cdn")
  private val components = Vector("frontend", "backend", "mobile", "infra", "data", "billing")

  private val exceptions = Vector(
    "java.lang.NullPointerException",
    "java.util.concurrent.TimeoutException",
    "java.lang.IllegalStateException",
    "java.io.IOException",
  )

  private val words = spaced(
    "checkout latency timeout retry customer invoice cache deploy rollback queue " +
      "partition replica token session webhook refund ledger balance report export import " +
      "search index shard throttle quota alert page dashboard metric trace span error " +
      "warning config flag release build"
  )

  private val unusual = Vector(
    "\"quoted\"",
    "back\\slash",
    "line\nbreak",
    "tab\there",
    "café",
    "Grüße",
    "naïve",
    "漢字",
    "日本語",
    "🚀",
    "emoji👍",
    "</script>",
    "\u0001control",
  )
