import sbt._
import sbt.Keys._

import scala.sys.process.Process
import scala.xml.XML

/** Copy-paste detection (PMD CPD) as a build gate.
  *
  * The codebase carries ~1300 pre-existing duplications at the 25-token threshold (fused-overload
  * families, law-suite scaffolding, JMH headers), so the gate is a ratchet on the CHANGE, not on
  * the tree: `cpdCheck` fails on any duplication that overlaps a line changed since the merge base
  * with [[cpdBase]] (committed, staged, unstaged and untracked `.scala` files alike). On `main`
  * itself the merge base is `HEAD`, so nothing is changed and the check passes.
  *
  * A duplication that is structural by design (one overload per inner optic type, say) can be
  * fenced with `// CPD-OFF` / `// CPD-ON` comments plus a one-line justification.
  *
  * PMD runs in a forked JVM from its own dependency configuration, so its Scala 2.13 / scalameta
  * stack never meets the sbt meta-build's classpath.
  */
object Cpd {

  val CpdTool = config("cpd-tool").hide

  val cpdMinimumTokens = settingKey[Int]("Smallest duplication, in tokens, CPD reports.")

  val cpdBase = settingKey[String](
    "Git ref the change is measured against (env CPD_BASE, default origin/main)."
  )

  val cpdCheck = taskKey[Unit](
    "Fail if CPD finds a duplication overlapping lines changed since the merge base with cpdBase."
  )

  private val pmdVersion = "7.27.0"

  final case class Occurrence(path: String, begin: Int, end: Int)
  final case class Duplication(tokens: Int, occurrences: Seq[Occurrence])

  def settings: Seq[Setting[_]] = Seq(
    ivyConfigurations += CpdTool,
    libraryDependencies ++= Seq(
      "net.sourceforge.pmd" % "pmd-cli" % pmdVersion % CpdTool,
      "net.sourceforge.pmd" % "pmd-scala_2.13" % pmdVersion % CpdTool,
    ),
    cpdMinimumTokens := 25,
    cpdBase := sys.env.getOrElse("CPD_BASE", "origin/main"),
    cpdCheck := {
      val log = streams.value.log
      val root = (ThisBuild / baseDirectory).value
      val min = cpdMinimumTokens.value
      val baseRef = cpdBase.value
      val toolCp = Classpaths.managedJars(CpdTool, classpathTypes.value, update.value).map(_.data)
      val out = target.value / "cpd"
      IO.createDirectory(out)

      val sources = scalaSources(root)
      val fileList = out / "files.txt"
      IO.writeLines(fileList, sources.map(f => (root / f).getAbsolutePath))
      val report = out / "cpd.xml"
      val args = Seq(
        "-cp",
        toolCp.mkString(java.io.File.pathSeparator),
        "net.sourceforge.pmd.cli.PmdCli",
        "cpd",
        "--language",
        "scala",
        "--minimum-tokens",
        min.toString,
        "--format",
        "xml",
        "--no-fail-on-violation",
        "--file-list",
        fileList.getAbsolutePath,
        "--report-file",
        report.getAbsolutePath,
      )
      val exit = Fork.java(ForkOptions().withJavaHome(javaHome.value), args)
      if (exit != 0) sys.error(s"CPD exited with $exit")

      val dups = parse(report, root)
      val base = git(root, "merge-base", "HEAD", baseRef)
        .headOption
        .getOrElse(
          sys.error(
            s"cpdCheck: no merge base between HEAD and '$baseRef' (fetch it, or set CPD_BASE)"
          )
        )
      val changed = changedLines(root, base)
      val offending = dups.filter(_.occurrences.exists { o =>
        changed.get(o.path).exists(lines => (o.begin to o.end).exists(lines))
      })

      log.info(
        s"CPD: ${dups.size} duplications of >= $min tokens in ${sources.size} files; " +
          s"${offending.size} touch lines changed since $baseRef (${base.take(10)})."
      )
      if (offending.nonEmpty) {
        offending.sortBy(-_.tokens).foreach { d =>
          log.error(
            s"${d.tokens} tokens: " + d
              .occurrences
              .map(o => s"${o.path}:${o.begin}-${o.end}")
              .mkString(" | ")
          )
        }
        sys.error(
          s"cpdCheck: ${offending.size} duplication(s) involve changed lines. Extract the shared code, " +
            "or fence a by-design repeat with // CPD-OFF / // CPD-ON and a justification. " +
            s"Full report: $report"
        )
      }
    },
  )

  /** Tracked and untracked (not ignored) Scala sources under any module's `src/main` / `src/test`.
    */
  private def scalaSources(root: File): Seq[String] =
    git(root, "ls-files", "--cached", "--others", "--exclude-standard", "--", "*.scala")
      .filter(_.matches(".*/src/(main|test)/.*"))
      .filter(f => (root / f).exists)

  private def parse(report: File, root: File): Seq[Duplication] = {
    val prefix = root.getAbsolutePath + java.io.File.separator
    (XML.loadFile(report) \\ "duplication").map { d =>
      val tokens = (d \ "@tokens").text.toInt
      val lines = (d \ "@lines").text.toInt
      val occurrences = (d \ "file").map { f =>
        val begin = (f \ "@line").text.toInt
        val end =
          Option((f \ "@endline").text).filter(_.nonEmpty).map(_.toInt).getOrElse(begin + lines - 1)
        Occurrence((f \ "@path").text.stripPrefix(prefix), begin, end)
      }
      Duplication(tokens, occurrences)
    }
  }

  /** Per file, the line numbers (in the working tree) added or changed since `base`; untracked
    * files count as entirely new.
    */
  private def changedLines(root: File, base: String): Map[String, Set[Int]] = {
    val Hunk = """@@ -\S+ \+(\d+)(?:,(\d+))? @@.*""".r
    val fromDiff = git(root, "diff", "-U0", "--no-color", base, "--", "*.scala")
      .foldLeft((Option.empty[String], Map.empty[String, Set[Int]])) {
        case ((_, acc), line) if line.startsWith("+++ b/") => (Some(line.drop(6)), acc)
        case ((Some(file), acc), Hunk(start, count))       =>
          val n = Option(count).fold(1)(_.toInt)
          val added = (start.toInt until start.toInt + n).toSet
          (Some(file), acc.updated(file, acc.getOrElse(file, Set.empty[Int]) ++ added))
        case (state, _) => state
      }
      ._2
    val untracked = git(root, "ls-files", "--others", "--exclude-standard", "--", "*.scala")
      .map(f => f -> (1 to IO.readLines(root / f).size).toSet)
    fromDiff ++ untracked
  }

  private def git(root: File, args: String*): Seq[String] =
    Process("git" +: args, root).!!.linesIterator.toVector

}
