# Contributing to cats-eo

Thanks for your interest in cats-eo. This guide is the day-one starting
point for human contributors. Companion guidance for AI-assisted
contributions lives in [`CLAUDE.md`](./CLAUDE.md).

## Getting set up

The toolchain — JDK, sbt, Scala, scalafmt, scalafix, metals — is
installed via [Coursier](https://get-coursier.io). Full bootstrap (one
shell snippet to install all of it) is documented in
[`CLAUDE.md`](./CLAUDE.md) under "Toolchain"; that section is the
canonical reference and stays current with what CI runs.

In short, you need:

- JDK 17 or JDK 21 (Temurin is fine).
- `sbt` 1.12.x.
- Scala 3.8.x (matches `project/build.properties`).
- `scalafmt` 3.11.x (honours the pin in `.scalafmt.conf`).

Clone the repo, then optionally enable the project git hooks so your
local commits run the same gates CI does:

```sh
.githooks/install.sh
```

See [`CLAUDE.md`](./CLAUDE.md) "Git hooks" for what each hook checks
and how to bypass them when you need to.

## Running the local checks

Before pushing, run the four commands CI gates on:

```sh
sbt scalafmtCheckAll               # formatting
sbt compile                        # full project compile
sbt test                           # core + tests + generics + circe
sbt 'docs/mdoc; docs/laikaSite'    # mdoc-verify every doc fence + render the site
```

If you installed the git hooks (above), `pre-commit` runs the format
check and the docs build automatically when staged files touch
`*.scala` / `*.sbt` / `*.md` / `.scalafmt.conf` / `site/` / `build.sbt`,
and `pre-push` runs the test sweep. CI runs the same set on every PR.

## Submitting a pull request

1. Fork the repo on GitHub.
2. Create a topic branch off `main`.
3. Make your change. Keep commits focused; the project follows a
   conventional-commits-style prefix (`feat(scope):`, `fix(scope):`,
   `docs(scope):`, etc.) — `git log` shows the pattern.
4. Run the four local checks above.
5. Push your branch and open a PR against `main`.

CI gates on the same checks (`scalafmtCheckAll`, `compile`, `test`,
docs build). If any fail, push fixes to the same branch — no need to
re-open the PR.

For substantial design changes, open an issue first so we can sketch
the approach before you spend time on it.

## Running the benchmarks

The `benchmarks/` sub-project is **not** part of the root aggregate, so
`sbt compile` and `sbt test` skip it. Run benchmarks explicitly:

```sh
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 1 -t 1"     # full sweep
sbt "benchmarks/Jmh/run -i 1 -wi 1 -f 1 LensBench.eoGet"   # single bench
```

Full instructions, the EO-only vs. paired-with-Monocle layout, and JMH
caveats live in [`CLAUDE.md`](./CLAUDE.md) "Benchmarks (JMH vs Monocle)"
and in [`benchmarks/README.md`](./benchmarks/README.md).

## Previewing the docs site locally

```sh
sbt 'docs/mdoc; docs/laikaSite'
# Site output:
#   site/target/docs/site/
```

Serve that directory with any static HTTP server, e.g.:

```sh
python3 -m http.server -d site/target/docs/site/ 8080
```

`mdoc` verifies every code fence in `site/docs/*.md` against the live
library, so a docs change that breaks a fence fails the build.

## Coverage

The project's primary quality signal is statement / branch coverage via
[`sbt-scoverage`](https://github.com/scoverage/sbt-scoverage). The
canonical command, output paths, and the rationale for relying on local
runs (rather than a CI-uploaded badge) are documented in
[`CLAUDE.md`](./CLAUDE.md) under "Test-suite quality".

If you add new runtime code, please add the matching tests so coverage
doesn't drift. Pure type-level machinery doesn't need coverage and is
not counted against the target.

## Releasing a new version (maintainer reference)

1. Update [`CHANGELOG.md`](./CHANGELOG.md): rename the `[Unreleased]`
   heading to `[X.Y.Z]` with today's date, and add a fresh
   `[Unreleased]` placeholder above it.
2. Commit with `release: vX.Y.Z`.
3. `git tag vX.Y.Z && git push origin main vX.Y.Z`.
4. The GitHub Actions release workflow triggers on the `v*` tag and
   runs the staged Sonatype Central Portal publish.
5. Watch the workflow; on success the artifacts appear at
   `https://repo1.maven.org/maven2/dev/constructive/cats-eo_3/X.Y.Z/`.

## Manual recovery for a partial Central Portal publish

If the release workflow errors mid-staging — signing failure, namespace
permission lapse, network blip during upload — recover as follows:

1. Sign in at <https://central.sonatype.com/publishing/deployments>.
2. Find the partially-uploaded deployment with status "Failed" or
   "Pending".
3. Click "Drop" to discard it. This clears the staging area.
   Partially-uploaded artifacts in maven-central never become canonical
   because the deployment never transitioned to "Validated &rarr;
   Released".
4. Resolve the underlying error (signing failure, namespace permission
   lapse, etc.). Bump the patch version and push a new tag — there is
   no concept of "rerun the failed publish"; you publish a new version.
5. If the failed deployment was a snapshot, it is silently overwritten
   by the next snapshot publish to the same coords; no manual cleanup
   is needed.
