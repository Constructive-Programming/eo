# Benchmark CI pipeline — operator runbook

Origin: [`docs/plans/2026-07-10-001-feat-benchmark-ci-pipeline-plan.md`](../../docs/plans/2026-07-10-001-feat-benchmark-ci-pipeline-plan.md)
(and the requirements brainstorm it links). Doctrine in one line: **B/op
(`-prof gc` allocation norm) is deterministic on shared runners and is the
only metric that may ever gate; ns/op is directional advice.**

## The pieces

| Piece | What it does |
|---|---|
| `bench_tools.py` | All pipeline logic (mapping, diff, rendering). `python3 -m unittest discover .github/bench` runs its suite; both workflows run it as their first step. |
| `bench-pr.yml` | Same-job A/B on same-repo PRs touching mapped paths: merge-base run, head run, sticky comment. `perf:full` label ⇒ full suite. `workflow_dispatch mode=aa` ⇒ noise calibration. |
| `bench-sweep.yml` | Nightly-if-changed + release-tag full sweep. One atomic bot commit to main (`[skip ci]`) carrying `BENCHMARKS.md` + an append to `site/laika-static/bench/series.jsonl`; attaches `jmh-results-<tag>.json` to releases. |
| `site/laika-static/bench/index.html` | Static history chart, shipped in-tree; Laika copies it (and the series) verbatim into the docs site, so it serves at `/bench/` on the existing Cloudflare Pages deployment — refreshed at each site deploy (preview per main push, production per `v*` tag). |
| `thresholds.json` | **Absent = advisory (current state).** Present = the B/op gate is live. |

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

## Calibration and flipping the gate (advisory → gating)

1. Dispatch **Benchmark A/B** with `mode=aa` (same ref twice) at least 3
   times; each posts a noise report to the run summary and artifact.
2. B/op floor must be ~0. If it isn't, the reduced profile has too little
   warmup for C2/escape analysis — raise `-wi` in `JMH_FLAGS` /
   `JMH_PROFILE` in bench-pr.yml and recalibrate.
3. Commit `.github/bench/thresholds.json`, e.g.
   `{"bop_regression_pct": 1.0, "bop_min_delta_bytes": 16}` — quantile
   data from the reports, not guesses.
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

- `bench-pr.yml` executes PR code ⇒ same-repo PRs only, token holds only
  `contents: read` + `pull-requests: write`; the sticky-comment action is
  SHA-pinned. Fork PRs get no run/comment (deploy-site.yml caveat).
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
