import sbt._
import sbt.Keys._

import java.io.File
import scala.sys.process.{Process, ProcessLogger}

/** Automated canary for the kindlings macro-expansion timeout (issue #106).
  *
  * kindlings reads a per-derivation macro budget from
  * `-Xmacro-settings:<namespace>.timeout=<value>` (`DerivationTimeout`). The value forms it accepts
  * are `<int>` — read as SECONDS, so `=500` is 500 seconds, not 500ms — and `<int><unit>` (`ms` /
  * `s` / `m` plus their long spellings); anything else is REPORTED (`<ns>.timeout: unrecognized
  * format ...`) and replaced by the 5s default. The silent failure mode is therefore the KEY, not
  * the value: a namespace kindlings no longer reads — renamed upstream, or typo'd — matches
  * nothing, so every derivation quietly runs on the 5s default.
  *
  * Nothing else in the build fails when that happens. This task is the guard. It reads the REAL
  * configured options out of sbt, then — for each channel — compiles a throwaway kindlings
  * derivation with the configured values replaced by `1ms` and asserts the compiler fails with
  * `timed out after 1ms`:
  *
  *   1. the build-wide `scalacOptions` channel: one live derivation per namespace
  *      (`circeDerivation` → `KindlingsCodecAsObject.derived`, `catsDerivation` → `Show.derived` /
  *      `Eq.derived`, `avroDerivation` → `AvroEncoder.derived` and friends), plus a clean compile
  *      of all three at the configured budget;
  *   2. the docs `mdocExtraArguments` channel (`--scalac-options`), run through mdoc's own CLI,
  *      because mdoc's fence compiler ignores `-Xmacro-settings` that reach it via
  *      `mdoc.properties` (a 1ms canary never fires through that route).
  *
  * The value spellings are checked structurally as well: this build's policy is that every timeout
  * carries a unit (a bare integer's SECONDS reading is too easy to misread), and a payload that
  * omits one of the three namespaces fails instead of silently dropping a channel. A final probe
  * asserts an unrecognized value is still REPORTED, so the build comment cannot rot into a claim
  * about silence that kindlings no longer honours.
  *
  * Everything runs from jars only (`externalDependencyClasspath` plus the compiler's own jars), so
  * the canary never compiles the project. Run it with `sbt kindlingsTimeoutCanary` or
  * `scripts/kindlings-timeout-canary.sh`; CI runs it in the `kindlings-timeout-canary` job of
  * `.github/workflows/quality.yml`.
  */
object KindlingsTimeoutCanary {

  val kindlingsTimeoutCanary = taskKey[Unit](
    "Compile throwaway kindlings derivations at a 1ms macro budget to prove the configured " +
      "-Xmacro-settings timeout keys are live, on both the scalacOptions and the mdoc channels."
  )

  /** kindlings' `DerivationTimeout.DurationPattern`, verbatim: the unit-bearing spellings it
    * accepts (a bare integer is also accepted, as SECONDS — see the class comment).
    */
  private val DurationPattern =
    """^\s*(\d+)\s*(ms|millis|milliseconds|s|seconds?|m|minutes?)\s*$""".r

  private val TimeoutSuffix = ".timeout"
  private val MacroSettings = "-Xmacro-settings:"
  private val TimeoutMarker = "timed out after 1ms"

  /** One kindlings derivation namespace and the smallest source that forces a derivation through
    * it. The mapping is deliberately pinned here: a kindlings bump that renames a namespace makes
    * the `1ms` value land on a key nobody reads, the derivation compiles, and the canary fails.
    */
  final private case class Namespace(name: String, fileName: String, source: String)

  private val namespaces: Seq[Namespace] = Seq(
    Namespace(
      "circeDerivation",
      "CanaryCirce.scala",
      """|import hearth.kindlings.circederivation.KindlingsCodecAsObject
         |import io.circe.Codec
         |
         |object CanaryCirce:
         |  final case class Person(name: String, age: Int)
         |
         |  given Codec.AsObject[Person] = KindlingsCodecAsObject.derived
         |""".stripMargin
    ),
    Namespace(
      "catsDerivation",
      "CanaryCats.scala",
      """|import cats.{Eq, Show}
         |import hearth.kindlings.catsderivation.extensions.*
         |
         |object CanaryCats:
         |  final case class Person(name: String, age: Int)
         |
         |  given Eq[Person] = Eq.derived
         |  given Show[Person] = Show.derived
         |""".stripMargin
    ),
    Namespace(
      "avroDerivation",
      "CanaryAvro.scala",
      """|import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
         |
         |object CanaryAvro:
         |  final case class Person(name: String, age: Int)
         |
         |  given AvroEncoder[Person] = AvroEncoder.derived
         |  given AvroDecoder[Person] = AvroDecoder.derived
         |  given AvroSchemaFor[Person] = AvroSchemaFor.derived
         |""".stripMargin
    ),
  )

  /** The mdoc page the fence channel is exercised on: the same avro derivation, inside an `mdoc`
    * fence (mdoc wraps a fence's content in its own object, so the body is object-free).
    */
  private val mdocPage: String =
    """|# kindlings timeout canary
       |
       |```scala mdoc
       |import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
       |
       |final case class Person(name: String, age: Int)
       |
       |given AvroEncoder[Person] = AvroEncoder.derived
       |given AvroDecoder[Person] = AvroDecoder.derived
       |given AvroSchemaFor[Person] = AvroSchemaFor.derived
       |```
       |""".stripMargin

  /** Run the canary. All arguments come from `build.sbt`, which is where the project references
    * (`avroIntegration`, `docs`, `tests`) are in scope.
    *
    * @param scalacOptions
    *   the build-wide channel, read from a real project so the inherited `ThisBuild` value is the
    *   one the compiler actually sees.
    * @param mdocExtraArguments
    *   the fence channel, as the docs project passes it to mdoc.
    * @param compilerJars
    *   the Scala compiler's own jars (run classpath only).
    * @param classpath
    *   the user classpath the canary sources compile against; jars only, so nothing is built.
    */
  def run(
      log: Logger,
      base: File,
      scalacOptions: Seq[String],
      mdocExtraArguments: Seq[String],
      compilerJars: Seq[File],
      classpath: Seq[File]
  ): Unit = {
    val work = base / "target" / "kindlings-timeout-canary"
    IO.delete(work)
    IO.createDirectory(work)

    val userCp = classpath.map(_.getAbsolutePath).distinct
    val userCpString = userCp.mkString(File.pathSeparator)
    val runCpString =
      (compilerJars.map(_.getAbsolutePath) ++ userCp).distinct.mkString(File.pathSeparator)
    val java = new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath

    // ---- 1. Both channels must be spelled with a unit kindlings accepts -----------------------
    val buildPayloads = macroSettingPayloads(scalacOptions)
    validateChannel("avroIntegration / Compile / scalacOptions", buildPayloads)

    val mdocPayloads = mdocScalacOptionPayloads(mdocExtraArguments)
    validateChannel("docs / mdocExtraArguments (--scalac-options)", mdocPayloads)

    // ---- 2. The scalacOptions channel, live --------------------------------------------------
    val canaryOptions = withoutSemanticdb(scalacOptions)
    val sources = namespaces.map { ns =>
      val file = work / "src" / ns.fileName
      IO.write(file, ns.source)
      ns -> file
    }

    val configuredOut = work / "out" / "configured"
    val configured =
      dotc(java, work, runCpString, userCpString, configuredOut, canaryOptions, sources.map(_._2))
    if (configured.exit != 0) {
      fail(
        "the canary sources do not compile at the CONFIGURED kindlings budget, so a 1ms failure " +
          "could not be told apart from a broken canary. Fix the source/classpath first.",
        configured
      )
    }
    log.info(s"[kindlings-canary] configured budget compiles: ${summarise(canaryOptions)}")

    sources.foreach {
      case (ns, file) =>
        val options =
          withTimeouts(canaryOptions, (name, value) => if (name == ns.name) "1ms" else value)
        val result = dotc(
          java,
          work,
          runCpString,
          userCpString,
          work / "out" / s"1ms-${ns.name}",
          options,
          Seq(file)
        )
        if (result.exit == 0) {
          fail(
            s"`${ns.name}` is DEAD: a derivation compiled with `${ns.name}.timeout=1ms` (every other " +
              "setting unchanged). kindlings no longer reads that namespace, so the build's budget " +
              "is silently the 5s default. Update the -Xmacro-settings payload in build.sbt (and the " +
              "ThisBuild/scalacOptions comment) to the namespace the pinned kindlings actually uses.",
            result
          )
        }
        if (!result.output.contains(TimeoutMarker)) {
          fail(
            s"`${ns.name}.timeout=1ms` made the derivation fail, but not with `$TimeoutMarker` — " +
              "kindlings' timeout diagnostics changed. Re-read DerivationTimeout and update the canary.",
            result
          )
        }
        log.info(s"[kindlings-canary] ${ns.name}.timeout=1ms fires `$TimeoutMarker`")
    }

    // ---- 3. An unrecognized value must still be REPORTED, not swallowed ----------------------
    // kindlings reports an unparseable value and falls back to its 5s default (fatal under this
    // build's -Werror). That is the one place the build's comment could rot: if a kindlings release
    // starts swallowing it silently, a key that never matches would be the only silent failure left
    // and the comment would be lying. Probe it instead of trusting it.
    val avroSource = sources.find(_._1.name == "avroDerivation").map(_._2).getOrElse {
      fail("internal: no avroDerivation canary source")
    }
    val unrecognized = withTimeouts(canaryOptions, (_, _) => "1x")
    val reported = dotc(
      java,
      work,
      runCpString,
      userCpString,
      work / "out" / "unrecognized",
      unrecognized,
      Seq(avroSource)
    )
    val mentionsKey = reported.output.contains("avroDerivation.timeout")
    val mentionsDefault = reported.output.toLowerCase.contains("default of")
    if (!mentionsKey || !mentionsDefault) {
      fail(
        "kindlings no longer REPORTS an unrecognized timeout value: expected a diagnostic naming " +
          "`avroDerivation.timeout` and the default it falls back to. The build comment about " +
          "silent misconfiguration is stale — re-read DerivationTimeout and update the comment.",
        reported
      )
    }
    log.info("[kindlings-canary] an unrecognized value is reported (5s default), not swallowed")

    // ---- 4. The mdoc fence channel, live -----------------------------------------------------
    val mdocIn = work / "mdoc" / "in"
    IO.write(mdocIn / "canary.md", mdocPage)

    val mdocFired = mdocRun(
      java,
      work,
      runCpString,
      userCpString,
      mdocIn,
      work / "mdoc" / "out-1ms",
      mdocPayloads
        .map(payload => MacroSettings + rewritePayload(payload, (_, _) => "1ms"))
        .mkString(" ")
    )
    if (mdocFired.exit == 0) {
      fail(
        "the docs `mdocExtraArguments` channel did not deliver `timeout=1ms` to mdoc's fence " +
          "compiler: the canary fence compiled clean. Either the payload lost its " +
          "`--scalac-options` pairing or mdoc stopped honouring it.",
        mdocFired
      )
    }
    if (!mdocFired.output.contains(TimeoutMarker)) {
      fail(
        s"mdoc failed, but not with `$TimeoutMarker` — the fence channel is broken for another " +
          "reason. Compiler output:",
        mdocFired
      )
    }
    log.info(s"[kindlings-canary] mdocExtraArguments channel fires `$TimeoutMarker`")

    val mdocConfigured = mdocRun(
      java,
      work,
      runCpString,
      userCpString,
      mdocIn,
      work / "mdoc" / "out-configured",
      mdocPayloads.map(MacroSettings + _).mkString(" ")
    )
    if (mdocConfigured.exit != 0) {
      fail(
        "the canary fence does not compile at the CONFIGURED mdoc budget. Compiler output:",
        mdocConfigured
      )
    }
    log.info(
      s"[kindlings-canary] mdoc configured budget compiles: ${summarise(mdocPayloads.map(MacroSettings + _))}"
    )

    log.info("[kindlings-canary] OK — both channels are live at the configured namespace keys")
  }

  // ---- option plumbing ---------------------------------------------------------------------

  /** The `-Xmacro-settings:` payloads (the text after the colon) of a scalac option list. */
  private def macroSettingPayloads(options: Seq[String]): Seq[String] =
    options.collect {
      case option if option.startsWith(MacroSettings) => option.substring(MacroSettings.length)
    }

  /** The `-Xmacro-settings:` payloads that follow a `--scalac-options` argument, in order. A
    * payload that is present but not paired with the flag is invisible to mdoc, which is exactly
    * the misconfiguration this channel must catch.
    */
  private def mdocScalacOptionPayloads(args: Seq[String]): Seq[String] =
    args
      .zipWithIndex
      .collect { case ("--scalac-options", i) if i + 1 < args.length => args(i + 1) }
      .flatMap { value =>
        macroSettingPayloads(Seq(value))
      }

  /** Validate one channel: at least one timeout is configured, every configured value carries a
    * duration unit, and every namespace the canary knows how to exercise is present.
    */
  private def validateChannel(where: String, payloads: Seq[String]): Unit = {
    if (payloads.isEmpty) {
      fail(s"no `-Xmacro-settings:` payload found in $where — the channel is unconfigured.")
    }
    val timeouts = payloads.flatMap(timeoutValues)
    if (timeouts.isEmpty) {
      fail(s"$where carries a `-Xmacro-settings:` payload with no `<namespace>.timeout` entry.")
    }
    timeouts.foreach {
      case (namespace, value) =>
        if (!DurationPattern.pattern.matcher(value).matches()) {
          fail(
            s"$where configures `$namespace.timeout=$value` without a duration unit. This build " +
              "requires one: kindlings reads a bare integer as SECONDS (`=500` would be 500 seconds, " +
              "not 500ms). Spell it `30s` / `120s` / `1ms`."
          )
        }
    }
    val configured = timeouts.map(_._1).toSet
    val missing = namespaces.map(_.name).filterNot(configured)
    if (missing.nonEmpty) {
      fail(
        s"$where does not configure ${missing.mkString(", ")}. The canary exercises " +
          s"${namespaces.map(_.name).mkString(", ")}; a namespace that is no longer intended must " +
          "be removed from the canary's namespace list too, deliberately."
      )
    }
  }

  /** `(namespace, value)` pairs of every `<namespace>.timeout=<value>` entry in a payload. */
  private def timeoutValues(payload: String): Seq[(String, String)] =
    payload.split(",").toSeq.filter(_.nonEmpty).flatMap { entry =>
      entry.indexOf('=') match {
        case -1 => None
        case i  =>
          val key = entry.substring(0, i).trim
          val value = entry.substring(i + 1).trim
          if (key.endsWith(TimeoutSuffix))
            Some(key.substring(0, key.length - TimeoutSuffix.length) -> value)
          else None
      }
    }

  /** Rewrite every `<namespace>.timeout=<value>` entry in a payload through `f`. */
  private def rewritePayload(payload: String, f: (String, String) => String): String =
    payload
      .split(",", -1)
      .map { entry =>
        entry.indexOf('=') match {
          case -1 => entry
          case i  =>
            val key = entry.substring(0, i).trim
            if (key.endsWith(TimeoutSuffix)) {
              key + "=" + f(
                key.substring(0, key.length - TimeoutSuffix.length),
                entry.substring(i + 1).trim
              )
            } else entry
        }
      }
      .mkString(",")

  private def withTimeouts(options: Seq[String], f: (String, String) => String): Seq[String] =
    options.map { option =>
      if (option.startsWith(MacroSettings))
        MacroSettings + rewritePayload(option.substring(MacroSettings.length), f)
      else option
    }

  /** The real flags minus SemanticDB: the canary has no use for scalafix metadata and its target
    * directory belongs to the project being compiled, not to a throwaway source.
    */
  private def withoutSemanticdb(options: Seq[String]): Seq[String] = {
    @annotation.tailrec
    def loop(in: List[String], acc: List[String]): List[String] = in match {
      case Nil                               => acc.reverse
      case "-Xsemanticdb" :: rest            => loop(rest, acc)
      case "-semanticdb-target" :: _ :: rest => loop(rest, acc)
      case option :: rest                    => loop(rest, option :: acc)
    }
    loop(options.toList, Nil)
  }

  private def summarise(options: Seq[String]): String =
    options
      .filter(_.startsWith(MacroSettings))
      .map(_.substring(MacroSettings.length))
      .mkString(" + ")

  // ---- process plumbing --------------------------------------------------------------------

  final private case class Result(exit: Int, output: String)

  private def dotc(
      java: String,
      cwd: File,
      runCp: String,
      compileCp: String,
      out: File,
      options: Seq[String],
      sources: Seq[File]
  ): Result = {
    IO.createDirectory(out)
    val args =
      Seq(
        "-cp",
        runCp,
        "dotty.tools.dotc.Main",
        "-classpath",
        compileCp,
        "-d",
        out.getAbsolutePath
      ) ++
        options ++ sources.map(_.getAbsolutePath)
    run(java, args, cwd)
  }

  private def mdocRun(
      java: String,
      cwd: File,
      runCp: String,
      classpath: String,
      in: File,
      out: File,
      scalacOptions: String
  ): Result = {
    IO.delete(out)
    IO.createDirectory(out)
    val args = Seq(
      "-cp",
      runCp,
      "mdoc.Main",
      "--in",
      in.getAbsolutePath,
      "--out",
      out.getAbsolutePath,
      "--classpath",
      classpath,
      "--scalac-options",
      scalacOptions
    )
    run(java, args, cwd)
  }

  private def run(java: String, args: Seq[String], cwd: File): Result = {
    val captured = new StringBuilder
    val logger = ProcessLogger(
      line => { captured.append(line).append('\n'); () },
      line => { captured.append(line).append('\n'); () }
    )
    val exit = Process(java +: args, cwd).!(logger)
    Result(exit, captured.result())
  }

  /** Fail with the compiler output attached: sbt prints a [[MessageOnlyException]] without a stack
    * trace, so the log stays readable in CI.
    */
  private def fail(message: String): Nothing =
    throw new MessageOnlyException(s"kindlings timeout canary: $message")

  private def fail(message: String, result: Result): Nothing = {
    val output = result.output.trim
    val detail =
      if (output.isEmpty) s"(the compiler exited ${result.exit} with no output — it compiled clean)"
      else output.split("\n").takeRight(30).mkString("\n")
    throw new MessageOnlyException(
      s"kindlings timeout canary: $message\n--- compiler output ---\n$detail"
    )
  }

}
