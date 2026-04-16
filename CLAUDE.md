# Agent guide

## Project

`cats-eo` — an Existential Optics library for Scala 3, built on top of
[cats](https://typelevel.org/cats/). Sources under `src/main/scala/eo/`, tests
under `src/test/scala/`. Scala `3.5.0` via sbt (`build.sbt`); `project/build.properties`
pins sbt `1.6.2`, which requires **JDK 17** (it does not parse cleanly on
JDK 21).

Runtime dependency: `org.typelevel:cats-core_3:2.12.0`.
Test-only: `org.typelevel:discipline-specs2_3:2.0.0`.

## Toolchain

Every tool below is installed via [Coursier](https://get-coursier.io). One-shot
bootstrap:

```sh
# Coursier launcher (native)
curl -fLo /usr/local/bin/cs \
  https://github.com/coursier/coursier/releases/latest/download/cs-x86_64-pc-linux.gz \
  && gunzip -f /usr/local/bin/cs && chmod +x /usr/local/bin/cs

# A JVM that can run sbt 1.6.2 (project pin)
cs java --jvm temurin:17 --setup        # writes JAVA_HOME into ~/.profile

# Scala dev tools
cs install --install-dir /usr/local/bin sbt scala scalafmt scalafix metals metals-mcp
```

After `cs java --setup`, open a fresh shell (or `source ~/.profile`) so
`JAVA_HOME` points at Temurin 17. The project will not build on JDK 21.

Installed versions in this environment: `sbt 1.6.2`, `scala-runner 1.12.4`
(Scala `3.8.3` by default, project uses `3.5.0`), `scalafmt 3.11.0` (honours
the `version` pin in `.scalafmt.conf`), `scalafix 0.14.6`, `metals 1.6.7`,
`metals-mcp 1.6.7`.

### Day-to-day commands

```sh
sbt compile                           # full project compile
sbt test                              # run the discipline-specs2 laws
sbt "~compile"                        # watch compile
scalafmt                              # format in place per .scalafmt.conf
scalafmt --check                      # CI-style verify
scalafix --rules RemoveUnused         # example rewrite
scala -e 'println(1 + 2)'             # quick REPL eval
```

## Metals MCP (stdio)

`metals-mcp` (new in v1.6.7) is registered as a project-local MCP server in
[`.mcp.json`](./.mcp.json):

```json
{
  "mcpServers": {
    "metals": {
      "type": "stdio",
      "command": "metals-mcp",
      "args": ["--workspace", ".", "--transport", "stdio"]
    }
  }
}
```

When Claude Code (or any MCP client) starts in this repo, it spawns
`metals-mcp` over stdio and exposes these tools to the agent:

| Tool | Use for |
|------|---------|
| `compile-file`, `compile-module`, `compile-full` | Fast feedback on a change |
| `test` | Run a specific test class / method (`testClass` required) |
| `glob-search`, `typed-glob-search` | Find a symbol by name across workspace + deps |
| `inspect` | Dump full signature, members, docstrings of a symbol |
| plus `find-dep`, `list-modules`, `import-build`, `file-decode`, … | see `tools/list` |

On first use Metals runs `sbt bloopInstall`, creates `.bloop/` and `.metals/`
(both `.gitignored`), and indexes the project. Subsequent calls are cached.

If you prefer the HTTP transport instead, regenerate the config with:

```sh
metals-mcp --workspace . --client claude-code   # writes HTTP .mcp.json, starts server
```

Metals knows about the current project graph, so prefer `glob-search` /
`inspect` for symbols **inside this project** (optics, accessors, instances),
and use `cellar` (below) for symbols **published to Maven** when you want to
answer without spinning up a build.

## JVM API lookups: `cellar`

[`cellar`](https://github.com/VirtusLab/cellar) answers signature, package,
and source queries straight from the `.tasty`/classfile a dependency ships to
Maven. Use it as the first line of investigation for cats, discipline, or any
other library — it is more reliable than guessing at API shapes and faster
than booting metals for external deps.

Install: native binary from
<https://github.com/VirtusLab/cellar/releases/latest>, or
`nix profile install github:VirtusLab/cellar`.

### External queries (no build tool needed)

```sh
cellar get-external     org.typelevel:cats-core_3:2.12.0 cats.Monad
cellar list-external    org.typelevel:cats-core_3:2.12.0 cats.data
cellar search-external  org.typelevel:cats-core_3:2.12.0 Traverse
cellar get-source       org.typelevel:cats-core_3:2.12.0 cats.Monad
cellar deps             org.typelevel:cats-core_3:2.12.0
cellar meta             org.typelevel:cats-core_3:2.12.0
```

### Project-aware queries (uses sbt)

From the repo root, auto-detect `build.sbt` and resolve against the project's
classpath. The module is named `root` in `build.sbt`.

```sh
cellar get    --module root cats.Monad
cellar list   --module root cats.data
cellar search --module root Traverse
```

### Workflow

When uncertain about a symbol:
1. `search` / `search-external` — locate the right name
2. `list`   / `list-external`  — see neighbours in the package
3. `get`    / `get-external`   — read the signature
4. `get-source`                — only when implementation details matter

Prefer `metals`-mcp for intra-project symbols (it knows your sources and local
overrides), `cellar` for dependency symbols (no build required, works offline
against the coursier cache).
