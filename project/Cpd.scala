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
  * A duplication whose every occurrence is a single line is not gated: that is a restated signature
  * (an override, a given implementing its capability) or a long type, which the language makes
  * repeat and nothing can extract.
  *
  * PMD's `CPD-OFF` / `CPD-ON` suppression comments are banned: `cpdCheck` fails if any Scala source
  * outside `project/` carries one. Extract the shared code instead.
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

  private val Suppression = "CPD-(OFF|ON)".r

  private val CallHead = """(new\s+)?[\w.]+(\[[^\]]*\])?\(""".r
  private val NamedArgument = """[a-z]\w*\s*=(?!=)\s*\S.*""".r
  private val Closer = """[)\]]+,?""".r

  /** A line that opens a method or given: modifiers, then `def` / `given`. */
  private val Signature =
    """^\s*((override|inline|final|transparent|abstract|private(\[\w+\])?|protected(\[\w+\])?)\s+)*(def|given)\b""".r

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

      val fenced =
        git(root, "ls-files", "--cached", "--others", "--exclude-standard", "--", "*.scala")
          .filterNot(_.startsWith("project/"))
          .filter(f => (root / f).exists)
          .flatMap { f =>
            IO.readLines(root / f).zipWithIndex.collect {
              case (line, i) if Suppression.findFirstIn(line).isDefined => s"$f:${i + 1}"
            }
          }
      if (fenced.nonEmpty) {
        fenced.foreach(at => log.error(s"CPD suppression comment: $at"))
        sys.error(
          s"cpdCheck: ${fenced.size} CPD-OFF / CPD-ON comment(s). Suppression is not allowed: " +
            "extract the shared code instead."
        )
      }

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
      // A duplication is exempt when one of its occurrences is at most a restated signature plus
      // one body line: an override or a capability given must repeat the signature of what it
      // implements, and a short body often sits on its own line under it. Concretely, an
      // occurrence with at most two code lines (blank and comment lines aside), one of them a
      // `def` / `given` signature, or with a single code line (a long type). There is nothing to
      // extract there; duplicated logic is longer and stays gated.
      val source = scala.collection.mutable.Map.empty[String, Vector[String]]
      def codeLines(o: Occurrence): Vector[String] =
        source
          .getOrElseUpdate(o.path, IO.readLines(root / o.path).toVector)
          .slice(o.begin - 1, o.end)
          .filter(l => l.trim.nonEmpty && !isCommentLine(l))
      def signatureSized(o: Occurrence): Boolean = {
        val code = codeLines(o)
        code.size <= 1 || (code.size == 2 && code.exists(l => Signature.findFirstIn(l).isDefined))
      }
      // An occurrence made only of skeleton lines — signatures, constructor / factory call
      // openings, one-line named arguments, closing brackets — is the shape of an overload family
      // (one `andThen` per inner optic, each `Optional.fused(getOrModify = …, to = …)`), whose
      // one-line kernels differ per carrier. A `match` / `case` block or a statement is not
      // skeleton, so multi-line logic stays gated.
      def skeleton(o: Occurrence): Boolean = codeLines(o).forall { l =>
        val t = l.trim
        Signature.findFirstIn(l).isDefined || CallHead.pattern.matcher(t).matches ||
        NamedArgument.pattern.matcher(t).matches || Closer.pattern.matcher(t).matches
      }
      val offending = dups.filter(d =>
        !d.occurrences.exists(o => signatureSized(o) || skeleton(o)) && d.occurrences.exists { o =>
          changed.get(o.path).exists(lines => (o.begin to o.end).exists(lines))
        }
      )

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
          s"cpdCheck: ${offending.size} duplication(s) involve changed lines. Extract the shared code. " +
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
    * files count as entirely new. Comment-only lines are skipped: CPD's tokens are code, so a doc
    * comment reflow (a rename, a typo fix) cannot introduce duplication — but it can sit inside a
    * pre-existing duplication's line span, and counting it would fail the ratchet for a pure
    * documentation edit.
    */
  private def changedLines(root: File, base: String): Map[String, Set[Int]] = {
    val Hunk = """@@ -\S+ \+(\d+)(?:,(\d+))? @@.*""".r
    val fromDiff = git(root, "diff", "-U0", "--no-color", base, "--", "*.scala")
      .foldLeft((Option.empty[String], Map.empty[String, Set[Int]])) {
        case ((_, acc), line) if line.startsWith("+++ b/") => (Some(line.drop(6)), acc)
        case ((Some(file), acc), Hunk(start, count))       =>
          val n = Option(count).fold(1)(_.toInt)
          val lines = IO.readLines(root / file)
          val added = (start.toInt until start.toInt + n)
            .filter(i => !isCommentLine(lines(i - 1)))
            .toSet
          (Some(file), acc.updated(file, acc.getOrElse(file, Set.empty[Int]) ++ added))
        case (state, _) => state
      }
      ._2
    val untracked = git(root, "ls-files", "--others", "--exclude-standard", "--", "*.scala")
      .map(f => f -> (1 to IO.readLines(root / f).size).toSet)
    fromDiff ++ untracked
  }

  /** A line that carries no CPD token: a `//` line comment or the `*`-prefixed body of a block
    * comment (scaladoc included). Scala has no expression that starts with `*`, so the check is
    * exact.
    */
  private def isCommentLine(line: String): Boolean = {
    val t = line.trim
    t.startsWith("//") || t.startsWith("/*") || t.startsWith("*")
  }

  private def git(root: File, args: String*): Seq[String] =
    Process("git" +: args, root).!!.linesIterator.toVector

}
