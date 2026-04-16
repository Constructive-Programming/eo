# Agent guide

## Project

`cats-eo` — an Existential Optics library for Scala 3, built on top of
[cats](https://typelevel.org/cats/). Built with sbt (`build.sbt`, Scala
`3.5.0`). Sources live under `src/main/scala/eo/`, tests under
`src/test/scala/`.

Main runtime dependency: `org.typelevel:cats-core_3:2.12.0`.
Test-only dependency: `org.typelevel:discipline-specs2_3:2.0.0`.

## JVM API lookups: use `cellar`

When you need to know the signature, members, or source of a symbol in any
dependency on the classpath, use [`cellar`](https://github.com/VirtusLab/cellar)
rather than guessing or scraping docs. It answers from the actual
`.tasty`/classfile shipped to Maven, so signatures are exact.

Install: grab the native binary for your platform from
<https://github.com/VirtusLab/cellar/releases/latest> and drop it on `$PATH`, or
`nix profile install github:VirtusLab/cellar`.

### External queries (work without a build tool)

Hit Maven coordinates directly — preferred in this repo because they do not
require sbt to be resolvable.

```sh
# Signature + members of a symbol
cellar get-external org.typelevel:cats-core_3:2.12.0 cats.Monad

# List what's in a package
cellar list-external org.typelevel:cats-core_3:2.12.0 cats.data

# Substring search across a jar's public API
cellar search-external org.typelevel:cats-core_3:2.12.0 Traverse

# Read the source of a symbol
cellar get-source org.typelevel:cats-core_3:2.12.0 cats.Monad

# Transitive dependency tree / POM metadata
cellar deps org.typelevel:cats-core_3:2.12.0
cellar meta org.typelevel:cats-core_3:2.12.0
```

### Project-aware queries (require `sbt` on `$PATH`)

From the repo root, these auto-detect `build.sbt` and resolve against the
project's classpath. Pass `--module root` (the module name in this build).

```sh
cellar get    --module root cats.Monad
cellar list   --module root cats.data
cellar search --module root Traverse
```

If you change the sbt binary (e.g. `sbtn`), either export
`CELLAR_SBT_BINARY=sbtn` or create `.cellar/cellar.conf` with:

```hocon
sbt { binary = "sbtn" }
```

### Workflow

When uncertain about a symbol, follow this progression:
1. `search` / `search-external` — find the right name
2. `list` / `list-external` — see the neighbours
3. `get` / `get-external` — read the signature
4. `get-source` — only when you need implementation details

Prefer `cellar` over inventing type signatures or over guessing at cats API
shapes. If `cellar` returns nothing useful, fall back to reading the published
Scaladoc or the source on GitHub.
