# Benchmark CI pipeline — operator runbook

Origin: [`docs/plans/2026-07-10-001-feat-benchmark-ci-pipeline-plan.md`](../../docs/plans/2026-07-10-001-feat-benchmark-ci-pipeline-plan.md)
(and the requirements brainstorm it links). Doctrine in one line: **B/op
(`-prof gc` allocation norm) is deterministic on shared runners and is the
calibrated regression signal; ns/op is directional advice. PR benchmarks
are informational comments, never merge gates.**

## The pieces

| Piece | What it does |
|---|---|
| `bench_tools.py` | Mapping, diff, rendering. `python3 -m unittest discover .github/bench` runs its suite and the PR-planning tests; both workflows run it as their first step. |
| `pr_bench.py` | Trusted PR resolution, immutable SHA planning, and freshness check before posting. |
| `bench-pr.yml` | After successful PR CI, runs on the default branch: same-VM merge-base vs exact PR-head comparison, then sticky comment. No benchmark check is attached to the PR head. `perf:full` ⇒ full suite. Manual A/B reruns and A/A calibration remain available. |
| `bench-sweep.yml` | Nightly-if-changed + release-tag full sweep. One atomic bot commit to main (`[skip ci]`) carrying `BENCHMARKS.md` + an append to `site/laika-static/bench/series.jsonl`; attaches `jmh-results-<tag>.json` to releases. |
| `site/laika-static/bench/index.html` | Static history chart, shipped in-tree; Laika copies it (and the series) verbatim into the docs site, so it serves at `/bench/` on the existing Cloudflare Pages deployment — refreshed at each site deploy (preview per main push, production per `v*` tag). |
| `thresholds.json` | Absent = no calibrated allocation warnings (current state). Present = highlight B/op threshold breaches in the comment, still non-blocking. The local `diff` CLI retains exit 3 for callers that want a gate. |

## Automatic reports and manual reruns

`workflow_run` listens for a successful **Continuous Integration** run
whose event was `pull_request`. Push CI runs, fork PRs, closed PRs, stale
head SHAs, and PRs not targeting `main` are ignored. GitHub sometimes omits
`workflow_run.pull_requests`, so resolution uses the same-repository head
branch and verifies the exact triggering head SHA against the live PR.

Planning and publishing use the workflow's default-branch SHA. Measurement
uses the exact PR head (not a moving branch or synthetic merge ref), compared
with its merge-base against the PR base. Both runs still share one VM and
the existing reduced JMH profile. Docs/site/tests-only changes skip unless
`perf:full` is set. Adding that label **after** CI completes does not trigger
another workflow; dispatch a rerun:

```sh
gh workflow run bench-pr.yml --ref main -f pr=129
gh workflow run bench-pr.yml --ref main -f pr=129 -f filter='.*LensBench.*'
gh workflow run bench-pr.yml --ref main -f mode=aa
```

Use the default branch in the Actions UI too. A/A benchmarks that branch's
workflow SHA twice and writes a noise report to the summary and artifact;
leave `pr` blank. Dispatching the workflow from a feature branch is rejected
to avoid attaching a long-running check to that branch.

The comment includes its head SHA and a link to the benchmark run. Immediately
before posting, the publisher rechecks that the PR is open, same-repository,
targets `main`, and still has that SHA. Obsolete results remain in Actions
artifacts but cannot overwrite the current revision's comment. Different head
revisions can finish independently; duplicate measurements of the same PR/head
cancel one another. Cancelled measurements do not publish a failure comment.

Bench/build/tooling failures produce an explicit ❌ comment and a failed
**detached** Actions run; threshold breaches only highlight the result.
Neither blocks the PR. This workflow must first be on the default branch for
`workflow_run` or manual dispatch to work. Existing PR-head checks from older
workflow runs remain historical records.

## Running the tool locally

```sh
python3 -m unittest discover .github/bench            # tool self-test
python3 .github/bench/bench_tools.py validate-mapping # mapping drift check
git diff --name-only main...HEAD \
  | python3 .github/bench/bench_tools.py affected     # -> JMH regex | FULL | ''
python3 .github/bench/bench_tools.py diff base.json head.json -o deltas.json \
  --base-sha X --head-sha Y --profile "pr:-i3-wi2-f1-t1-gc"   # exit 3 = gate
python3 .github/bench/bench_tools.py comment-md deltas.json   # PR comment md
python3 .github/bench/bench_tools.py benchmarks-md sweep.json --source-sha X …
python3 .github/bench/bench_tools.py append-series sweep.json --source-sha X …
python3 .github/bench/bench_tools.py noise-report a.json b.json  # A/A floors
```

`base.json`/`head.json`/`sweep.json` are JMH `-rf json` output (run with
`-prof gc` or the B/op column is empty and `benchmarks-md` refuses).
Provenance flags are shared: `--source-sha --base-sha --head-sha --date
--jdk --runner --jmh-params --profile`; `--help` on any subcommand lists
what it takes.

## Maintaining the path→bench mapping

`MODULE_BENCHES` in `bench_tools.py` maps leaf module dirs to the bench
classes that exercise them (**by import**, not by name — e.g.
`JsoniterBench` also parses with circe). `core/`, `build.sbt`, `project/`
and `benchmarks/` mean the full suite. Unknown non-ignored paths
conservatively mean the full suite, so a forgotten mapping can cost time
but never silently skip. When adding a module or bench class: update the
dict; `validate-mapping` (run by both workflows) fails loudly on stale
entries, and the unit tests pin the semantics.

## Calibration and enabling allocation warnings

1. Dispatch **Benchmark A/B** from `main` with `mode=aa` (same SHA twice) at least 3
   times; each posts a noise report to the run summary and artifact.
2. B/op floor must be ~0. If it isn't: first pin the fork JVM's
   allocation ergonomics — adaptive TLAB sizing makes `gc.alloc.rate.norm`
   count environment-dependent retire waste (measured on PR #116:
   ±78-81% A/A swings on `PlatedBench.visitorUniverseJson n=4096`
   unpinned, ±0.0% with `-jvmArgsAppend -XX:-ResizeTLAB` plus a fixed
   `-Xms`/`-Xmx`; fork counts do NOT fix it — `-f 3` unpinned still
   swung ±11-41%). Then raise `-wi` in `JMH_FLAGS` / `JMH_PROFILE` in
   bench-pr.yml and recalibrate. Changing the flags changes the profile —
   see step 4.
3. Commit `.github/bench/thresholds.json`, e.g.
   `{"bop_regression_pct": 1.0, "bop_min_delta_bytes": 16}` — quantile
   data from the reports, not guesses. This enables warnings, not PR gating.
4. **Profile binding (R5):** any change to `JMH_PROFILE`, the JDK, or the
   runner image invalidates calibration — delete `thresholds.json` in the
   same PR and recalibrate before restoring it.

## Sweep budget (R14)

The sweep runs single-job (`timeout-minutes: 350`). Record the measured
wall time of the first real sweeps here; if it approaches ~5h, shard by
bench-class groups across a matrix and merge the JSON shards before
`append-series` (design note in the plan). Any parameter reduction must be
recorded in the provenance (`--jmh-params` / `--profile`).

- 2026-07-10: not yet measured — first sweep pending.

## Credentials and trust boundaries (R15)

- `bench-pr.yml` accepts same-repo PRs only. Its planning job reads Git
  objects with a read-only token and never executes PR code. The measurement
  job executes PR code with only `contents: read`, no persisted checkout
  credentials, and restore-only caches (no PR cache saves on the default
  branch). It uploads raw JMH JSON only.
- A separate publisher checks out trusted default-branch tooling, renders
  that JSON as data, and holds `pull-requests: write` for the SHA-pinned
  sticky-comment action. PR number and SHAs come from the trusted plan, not
  the artifact; artifacts cannot supply executable tooling or publication
  targets. Threshold configuration also comes from that trusted checkout.
  Fork PRs get no run/comment.
- `bench-sweep.yml` executes trusted main/tag code only and is the sole
  holder of `contents: write` and the sole writer of the series file.
- Bot pushes use the plain `GITHUB_TOKEN` (proven by
  update-readme-version.yml). If branch protection ever blocks it, switch
  to a fine-grained PAT or GitHub App with contents:write and add it as a
  protection bypass actor — do not weaken protection repo-wide.

## One-time repo setup

- [ ] Create the `perf:full` label (`gh label create perf:full ...`).

No Pages setup needed: the chart and series ride the existing docs-site
deployment (Laika copies `site/laika-static/` verbatim; deploy-site.yml
ships it to Cloudflare Pages), serving at `https://eo.constructive.dev/bench/`.
