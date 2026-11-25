#!/usr/bin/env bash
#
# Automated canary for the kindlings macro-expansion timeout (issue #106).
#
# kindlings reads its per-derivation macro budget from
# `-Xmacro-settings:<namespace>.timeout=<value>`. A bare integer is read as SECONDS, a value it
# cannot read is reported and replaced by the 5s default — but a namespace kindlings no longer
# reads (renamed upstream, or typo'd) matches nothing, so every derivation quietly runs on 5s and
# nothing fails. This script is that guard: it compiles throwaway kindlings derivations with the
# REAL configured values rewritten to `1ms` and fails unless the compiler reports
# `timed out after 1ms`. Both channels are covered —
#
#   1. the build-wide `scalacOptions` payload (one derivation per namespace: circe, cats, avro),
#   2. the docs project's `mdocExtraArguments` payload, through mdoc's own `--scalac-options`
#      (mdoc's fence compiler ignores `-Xmacro-settings` delivered any other way).
#
# It also enforces this build's unit policy (a bare integer means SECONDS, so `=500` would be 500
# seconds rather than 500ms) and requires the payload to keep naming all three namespaces.
#
# The logic lives in `project/KindlingsTimeoutCanary.scala`; this wrapper is the single local
# command. CI runs it in the `kindlings-timeout-canary` job of `.github/workflows/quality.yml`.
#
# Proving the canary bites (each must FAIL — run with `set` so build.sbt is untouched):
#
#   # a unitless value (this build's policy)
#   sbt 'set ThisBuild / scalacOptions += "-Xmacro-settings:circeDerivation.timeout=30"' \
#     kindlingsTimeoutCanary
#
#   # a renamed namespace: the canary's 1ms lands on a key kindlings never reads
#   sbt 'set ThisBuild / scalacOptions := Seq("-Xmacro-settings:circeDerivation.timeout=30s,catsDerivation.timeout=30s,avroDerivatio.timeout=30s")' \
#     kindlingsTimeoutCanary
#
set -euo pipefail

root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$root"

# Extra arguments (e.g. `-batch` in CI) go to sbt ahead of the task.
exec sbt "$@" kindlingsTimeoutCanary
