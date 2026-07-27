#!/usr/bin/env python3
"""Benchmark CI pipeline tool — all logic for the bench-pr / bench-sweep
workflows lives here so it can be unit-tested (test_bench_tools.py).

Stdlib only. Subcommands:

  affected          changed file paths (stdin, one per line) -> JMH include
                    regex on stdout, the literal FULL, or nothing (skip)
  validate-mapping  fail loudly when MODULE_BENCHES has drifted from the
                    bench sources on disk
  diff              pair base/head JMH JSON results -> deltas.json
                    (gates on B/op only when thresholds.json exists)
  comment-md        deltas.json -> sticky PR comment markdown
  benchmarks-md     sweep JMH JSON -> BENCHMARKS.md content
  append-series     sweep JMH JSON -> one JSONL series line
  noise-report      N A/A deltas.json -> per-family noise-floor table

Doctrine (see docs/brainstorms/2026-07-10-benchmark-ci-pipeline-requirements.md):
B/op (-prof gc, gc.alloc.rate.norm) is deterministic on shared runners and
is the only metric that may ever gate; ns/op is directional advice.
"""

from __future__ import annotations

import argparse
import html
import json
import math
import pathlib
import re
import sys

# --- path -> bench mapping -------------------------------------------------
#
# Keyed by module source dir; values are bench CLASSES whose code paths
# exercise that module (derived from the bench files' imports, not their
# names — e.g. JsoniterBench also parses with circe).
# core/ is depended on by every module, so core/ (and the harness itself)
# means FULL. Unknown, non-ignored paths conservatively mean FULL too: a
# new module without a mapping entry must never silently skip benchmarks.

MODULE_BENCHES = {
    "avro/": ["AvroBytesBench", "AvroJsonBridgeBench", "AvroVulcanBench", "OrderAvroBench"],
    "circe/": ["JsoniterBench", "OpticBuildBench", "OrderCirceBench", "PlatedBench"],
    "jsoniter/": [
        "AvroJsonBridgeBench",
        "JsoniterBench",
        "OrderAvroBench",
        "OrderJsoniterBench",
    ],
    "schemes/": ["SchemesBench"],
    "generics/": ["GenericsBench"],
    "zio/": ["ZioDiBench"],
    "kyo/": ["KyoDiBench"],
}

FULL_TRIGGERS = ("core/", "benchmarks/", "build.sbt", "project/")

# Paths with no runtime perf surface. `.github/` is here: workflow/tool
# changes are exercised by the tool's own tests, not by benchmarks.
IGNORED_PREFIXES = (
    "docs/",
    "site/",
    "tests/",
    "laws/",
    ".github/",
    ".githooks/",
    "README.md",
    "CHANGELOG.md",
    "CONTRIBUTING.md",
    "CLAUDE.md",
    "BENCHMARKS.md",
    "LICENSE",
    ".gitignore",
    ".scalafmt.conf",
    ".scalafix.conf",
    ".mcp.json",
)

BENCH_SRC = "benchmarks/src/main/scala/dev/constructive/eo/bench"
# This JMH's JSON emits the gc profiler key WITHOUT the legacy "·" prefix
# (verified against a real `-prof gc -rf json` run); older JMH used "·gc.…".
# Accept both so a toolchain bump can't silently kill the gate metric.
GC_NORM = "gc.alloc.rate.norm"
GC_NORM_KEYS = (GC_NORM, "·gc.alloc.rate.norm")


def affected(paths: list[str]) -> str:
    """Return a JMH include regex, 'FULL', or '' (no benches affected)."""
    classes: set[str] = set()
    for p in paths:
        p = p.strip()
        if not p:
            continue
        if p.startswith(FULL_TRIGGERS):
            return "FULL"
        for prefix, benches in MODULE_BENCHES.items():
            if p.startswith(prefix):
                classes.update(benches)
                break
        else:
            if not p.startswith(IGNORED_PREFIXES):
                # Unknown territory: refuse to guess narrow. See mapping note.
                return "FULL"
    if not classes:
        return ""
    return "(" + "|".join(sorted(re.escape(c) for c in classes)) + ")\\..*"


def validate_mapping(repo_root: pathlib.Path) -> list[str]:
    """Return a list of drift errors (empty = healthy)."""
    errors = []
    bench_dir = repo_root / BENCH_SRC
    if not bench_dir.is_dir():
        return [f"bench source dir missing: {BENCH_SRC}"]
    on_disk = {p.stem for p in bench_dir.glob("*Bench.scala")}
    for prefix, benches in MODULE_BENCHES.items():
        if not (repo_root / prefix).exists():
            errors.append(f"mapped path does not exist: {prefix}")
        for b in benches:
            if b not in on_disk:
                errors.append(f"mapped bench class has no source file: {b}")
    return errors


# --- JMH JSON handling -----------------------------------------------------


def load_results(path: pathlib.Path) -> dict:
    """JMH -rf json output -> {key: entry}; key pairs FQN + params."""
    out = {}
    for e in json.loads(path.read_text()):
        params = e.get("params") or {}
        key = (e["benchmark"], tuple(sorted(params.items())))
        pm = e["primaryMetric"]
        err = pm.get("scoreError")
        sm = e.get("secondaryMetrics") or {}
        gc = next((sm[k] for k in GC_NORM_KEYS if k in sm), None)
        bop = gc["score"] if gc is not None else None
        out[key] = {
            "benchmark": e["benchmark"],
            "params": params,
            "ns": pm["score"],
            "ns_err": err if isinstance(err, (int, float)) and not math.isnan(err) else None,
            "ns_unit": pm.get("scoreUnit", "ns/op"),
            "bop": bop,
        }
    return out


def _pct(base: float | None, head: float | None) -> float | None:
    if base is None or head is None:
        return None
    if base == 0:
        return None if head == 0 else math.inf
    return (head - base) / base * 100.0


def _short(fqn: str) -> str:
    parts = fqn.split(".")
    return ".".join(parts[-2:]) if len(parts) >= 2 else fqn


def diff(base: dict, head: dict, provenance: dict, thresholds: dict | None) -> dict:
    pairs, new, removed = [], [], []
    for key in sorted(set(base) | set(head), key=str):
        b, h = base.get(key), head.get(key)
        if b and h:
            pairs.append(
                {
                    "benchmark": h["benchmark"],
                    "params": h["params"],
                    "ns_base": b["ns"],
                    "ns_head": h["ns"],
                    "ns_pct": _pct(b["ns"], h["ns"]),
                    "ns_unit": h["ns_unit"],
                    "bop_base": b["bop"],
                    "bop_head": h["bop"],
                    "bop_pct": _pct(b["bop"], h["bop"]),
                    "bop_delta": None
                    if (b["bop"] is None or h["bop"] is None)
                    else h["bop"] - b["bop"],
                }
            )
        elif h:
            new.append({"benchmark": h["benchmark"], "params": h["params"]})
        else:
            removed.append({"benchmark": b["benchmark"], "params": b["params"]})

    method = lambda e: e["benchmark"].rsplit(".", 1)[-1]  # noqa: E731
    removed_methods = {method(e) for e in removed}
    suspects = sorted(
        {method(e) for e in new if method(e) in removed_methods}
    )

    gate = {"enabled": False, "violations": []}
    if thresholds:
        pct_lim = thresholds.get("bop_regression_pct", 1.0)
        min_bytes = thresholds.get("bop_min_delta_bytes", 16)
        gate["enabled"] = True
        gate["thresholds"] = {"bop_regression_pct": pct_lim, "bop_min_delta_bytes": min_bytes}
        for p in pairs:
            if (
                p["bop_pct"] is not None
                and p["bop_delta"] is not None
                and p["bop_pct"] > pct_lim
                and p["bop_delta"] > min_bytes
            ):
                gate["violations"].append(_short(p["benchmark"]))

    return {
        "provenance": provenance,
        "pairs": pairs,
        "new": new,
        "removed": removed,
        "rename_suspects": suspects,
        "gate": gate,
    }


# --- rendering -------------------------------------------------------------


def esc(s: str) -> str:
    """Benchmark-derived strings are untrusted (R3): neuter markdown/html.

    Backtick-fencing alone would do; stripping link/table syntax too is
    defense in depth for renderers that mishandle code spans.
    """
    neutered = (
        str(s).replace("`", "").replace("|", "/").replace("[", "(").replace("]", ")")
    )
    return "`" + html.escape(neutered, quote=True) + "`"


def _fmt(v: float | None, digits: int = 1) -> str:
    if v is None:
        return "—"
    if math.isinf(v):
        return "∞"
    return f"{v:,.{digits}f}"


def _fmt_pct(v: float | None) -> str:
    if v is None:
        return "—"
    if math.isinf(v):
        return "+∞%"
    return f"{v:+.1f}%"


def _params_str(params: dict) -> str:
    return ",".join(f"{k}={v}" for k, v in sorted(params.items())) or "-"


def _provenance_footer(prov: dict) -> str:
    bits = [f"{k}: {esc(v)}" for k, v in prov.items() if v]
    return "<sub>" + " · ".join(bits) + "</sub>\n"


def comment_md(deltas: dict | None, failure: str | None) -> str:
    lines = ["## Benchmark A/B", ""]
    if failure is not None:
        lines += [
            "### ❌ benchmark job failed",
            "",
            f"> {html.escape(failure)}",
            "",
            "_No comparison available — this is an explicit failure state,"
            " not a pass._",
            "",
        ]
        if deltas and deltas.get("provenance"):
            lines.append(_provenance_footer(deltas["provenance"]))
        return "\n".join(lines)

    assert deltas is not None
    pairs = deltas["pairs"]
    by_bop = sorted(
        pairs,
        key=lambda p: abs(p["bop_pct"]) if p["bop_pct"] not in (None, math.inf) else math.inf,
        reverse=True,
    )

    if deltas["gate"]["enabled"] and deltas["gate"]["violations"]:
        lines += [
            "### 🚨 B/op gate: "
            + ", ".join(esc(v) for v in deltas["gate"]["violations"]),
            "",
        ]

    lines += ["### Allocation (B/op) — authoritative", ""]
    lines += ["| Benchmark | params | base | head | Δ |", "|---|---|---:|---:|---:|"]
    head_rows, rest = by_bop[:20], by_bop[20:]
    for p in head_rows:
        lines.append(
            f"| {esc(_short(p['benchmark']))} | {esc(_params_str(p['params']))} "
            f"| {_fmt(p['bop_base'])} | {_fmt(p['bop_head'])} | {_fmt_pct(p['bop_pct'])} |"
        )
    if rest:
        lines += ["", "<details><summary>"
                  f"{len(rest)} more benchmarks</summary>", ""]
        lines += ["| Benchmark | params | base | head | Δ |", "|---|---|---:|---:|---:|"]
        for p in rest:
            lines.append(
                f"| {esc(_short(p['benchmark']))} | {esc(_params_str(p['params']))} "
                f"| {_fmt(p['bop_base'])} | {_fmt(p['bop_head'])} | {_fmt_pct(p['bop_pct'])} |"
            )
        lines += ["", "</details>"]
    lines.append("")

    lines += [
        "<details><summary>Timing (ns/op) — directional only, same-VM but"
        " shared runner</summary>",
        "",
        "| Benchmark | params | base | head | Δ |",
        "|---|---|---:|---:|---:|",
    ]
    for p in sorted(
        pairs,
        key=lambda p: abs(p["ns_pct"]) if p["ns_pct"] not in (None, math.inf) else math.inf,
        reverse=True,
    ):
        lines.append(
            f"| {esc(_short(p['benchmark']))} | {esc(_params_str(p['params']))} "
            f"| {_fmt(p['ns_base'])} | {_fmt(p['ns_head'])} | {_fmt_pct(p['ns_pct'])} |"
        )
    lines += ["", "</details>", ""]

    if deltas["new"]:
        lines.append(
            "**New (head only, not diffed):** "
            + ", ".join(esc(_short(e["benchmark"])) for e in deltas["new"])
        )
    if deltas["removed"]:
        lines.append(
            "**Removed (base only):** "
            + ", ".join(esc(_short(e["benchmark"])) for e in deltas["removed"])
        )
    if deltas["rename_suspects"]:
        lines.append(
            "⚠️ **Possible rename** (removed+new pair — baseline lost): "
            + ", ".join(esc(m) for m in deltas["rename_suspects"])
        )
    if deltas["new"] or deltas["removed"] or deltas["rename_suspects"]:
        lines.append("")

    lines.append(_provenance_footer(deltas.get("provenance", {})))
    return "\n".join(lines)


_EO_M = re.compile(r"^(eo|m)(?=[A-Z0-9])")


def benchmarks_md(results: dict, prov: dict) -> str:
    """R7: per-class tables, eo vs Monocle side by side where paired."""
    if not results:
        raise SystemExit("benchmarks-md: no results in sweep JSON")
    by_class: dict[str, list[dict]] = {}
    missing_gc = [e["benchmark"] for e in results.values() if e["bop"] is None]
    if missing_gc:
        raise SystemExit(
            "benchmarks-md: sweep is missing gc.alloc.rate.norm for "
            f"{len(missing_gc)} benchmarks (run with -prof gc): {missing_gc[:3]}"
        )
    for e in results.values():
        cls = e["benchmark"].rsplit(".", 2)[-2]
        by_class.setdefault(cls, []).append(e)

    lines = [
        "# Benchmarks",
        "",
        "> **Generated file — do not edit.** Written by the bench-sweep",
        "> workflow (see `.github/bench/`). eo vs"
        " [Monocle](https://www.optics.dev/Monocle/) on JMH.",
        ">",
        "> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the",
        "> authoritative, run-to-run comparable metric; ns/op is",
        "> directional** and not comparable across runs/VMs. The usual JMH",
        '> disclaimer applies: "the numbers below are just data".',
        "",
        _provenance_footer(prov),
        "",
    ]
    for cls in sorted(by_class):
        entries = by_class[cls]
        lines += [f"## {cls}", ""]
        lines += [
            "| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |",
            "|---|---|---:|---:|---:|---:|",
        ]
        rows: dict[tuple, dict] = {}
        for e in entries:
            m = e["benchmark"].rsplit(".", 1)[-1]
            side = None
            if _EO_M.match(m):
                side = "eo" if m.startswith("eo") else "m"
                stem = _EO_M.sub("", m)
            else:
                stem = m
            rk = (stem, _params_str(e["params"]))
            row = rows.setdefault(rk, {})
            row[side or "eo"] = e
        for (stem, params), row in sorted(rows.items()):
            eo, mo = row.get("eo"), row.get("m")

            def cell(e, metric):
                if e is None:
                    return "—"
                if metric == "ns":
                    err = f" ± {_fmt(e['ns_err'])}" if e["ns_err"] is not None else ""
                    return f"{_fmt(e['ns'])}{err}"
                return _fmt(e["bop"])

            lines.append(
                f"| {esc(stem)} | {esc(params)} | {cell(eo, 'ns')} | {cell(mo, 'ns')} "
                f"| {cell(eo, 'bop')} | {cell(mo, 'bop')} |"
            )
        lines.append("")
    return "\n".join(lines)


def series_line(results: dict, prov: dict) -> str:
    entry = {
        **prov,
        "results": [
            {
                "b": _short(e["benchmark"]),
                "p": _params_str(e["params"]),
                "ns": e["ns"],
                "ns_err": e["ns_err"],
                "bop": e["bop"],
            }
            for _, e in sorted(results.items(), key=lambda kv: str(kv[0]))
        ],
    }
    return json.dumps(entry, separators=(",", ":"), ensure_ascii=False)


def noise_report(deltas_list: list[dict]) -> str:
    if not deltas_list:
        raise SystemExit("noise-report: no A/A deltas provided")
    profiles = {json.dumps(d.get("provenance", {}).get("profile"), sort_keys=True) for d in deltas_list}
    if len(profiles) > 1:
        raise SystemExit(
            "noise-report: refusing to aggregate A/A runs from different "
            f"measurement profiles: {sorted(profiles)} (R5: calibration is "
            "bound to the profile)"
        )
    fam: dict[str, dict[str, list[float]]] = {}
    for d in deltas_list:
        for p in d["pairs"]:
            cls = p["benchmark"].rsplit(".", 2)[-2]
            f = fam.setdefault(cls, {"ns": [], "bop": []})
            if p["ns_pct"] is not None and not math.isinf(p["ns_pct"]):
                f["ns"].append(abs(p["ns_pct"]))
            if p["bop_pct"] is not None and not math.isinf(p["bop_pct"]):
                f["bop"].append(abs(p["bop_pct"]))

    def q(vals, frac):
        if not vals:
            return None
        s = sorted(vals)
        return s[int(frac * (len(s) - 1))]

    lines = [
        f"# A/A noise floor ({len(deltas_list)} run(s))",
        "",
        "Profile-bound (R5): "
        + json.dumps(deltas_list[0].get("provenance", {}).get("profile")),
        "",
        "| Family | |ns/op Δ| p50 | p90 | max | |B/op Δ| p50 | p90 | max |",
        "|---|---:|---:|---:|---:|---:|---:|",
    ]
    for cls in sorted(fam):
        f = fam[cls]
        lines.append(
            f"| {esc(cls)} "
            f"| {_fmt(q(f['ns'], .5))}% | {_fmt(q(f['ns'], .9))}% | {_fmt(max(f['ns']) if f['ns'] else None)}% "
            f"| {_fmt(q(f['bop'], .5), 3)}% | {_fmt(q(f['bop'], .9), 3)}% | {_fmt(max(f['bop']) if f['bop'] else None, 3)}% |"
        )
    lines += [
        "",
        "B/op max should be ~0. If it isn't, the reduced profile has too "
        "little warmup for C2/escape analysis — bump `-wi` before any gate.",
    ]
    return "\n".join(lines)


# --- CLI -------------------------------------------------------------------


def _prov_args(sp):
    for a in ("source-sha", "base-sha", "head-sha", "date", "jdk", "runner", "jmh-params", "profile"):
        sp.add_argument(f"--{a}", default=None)


def _prov(ns) -> dict:
    keys = ("source_sha", "base_sha", "head_sha", "date", "jdk", "runner", "jmh_params", "profile")
    return {k: getattr(ns, k) for k in keys if getattr(ns, k, None)}


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(prog="bench_tools")
    sub = ap.add_subparsers(dest="cmd", required=True)

    sub.add_parser("affected", help="paths on stdin -> regex|FULL|''")

    vp = sub.add_parser("validate-mapping")
    vp.add_argument("--repo-root", default=".")

    dp = sub.add_parser("diff")
    dp.add_argument("base")
    dp.add_argument("head")
    dp.add_argument("-o", "--out", required=True)
    dp.add_argument("--thresholds", default=".github/bench/thresholds.json")
    _prov_args(dp)

    cp = sub.add_parser("comment-md")
    cp.add_argument("deltas", nargs="?")
    cp.add_argument("--failure", default=None)

    bp = sub.add_parser("benchmarks-md")
    bp.add_argument("sweep")
    _prov_args(bp)

    sp = sub.add_parser("append-series")
    sp.add_argument("sweep")
    _prov_args(sp)

    np_ = sub.add_parser("noise-report")
    np_.add_argument("deltas", nargs="+")

    ns = ap.parse_args(argv)

    if ns.cmd == "affected":
        print(affected(sys.stdin.read().splitlines()))
        return 0

    if ns.cmd == "validate-mapping":
        errors = validate_mapping(pathlib.Path(ns.repo_root))
        for e in errors:
            print(f"mapping drift: {e}", file=sys.stderr)
        return 1 if errors else 0

    if ns.cmd == "diff":
        tpath = pathlib.Path(ns.thresholds)
        thresholds = json.loads(tpath.read_text()) if tpath.exists() else None
        d = diff(
            load_results(pathlib.Path(ns.base)),
            load_results(pathlib.Path(ns.head)),
            _prov(ns),
            thresholds,
        )
        pathlib.Path(ns.out).write_text(json.dumps(d, indent=1))
        if d["gate"]["violations"]:
            print("B/op gate violations: " + ", ".join(d["gate"]["violations"]), file=sys.stderr)
            return 3
        return 0

    if ns.cmd == "comment-md":
        deltas = json.loads(pathlib.Path(ns.deltas).read_text()) if ns.deltas else None
        if deltas is None and ns.failure is None:
            raise SystemExit("comment-md: need deltas.json and/or --failure")
        print(comment_md(deltas, ns.failure))
        return 0

    if ns.cmd == "benchmarks-md":
        print(benchmarks_md(load_results(pathlib.Path(ns.sweep)), _prov(ns)))
        return 0

    if ns.cmd == "append-series":
        print(series_line(load_results(pathlib.Path(ns.sweep)), _prov(ns)))
        return 0

    if ns.cmd == "noise-report":
        print(noise_report([json.loads(pathlib.Path(p).read_text()) for p in ns.deltas]))
        return 0

    raise AssertionError(ns.cmd)


if __name__ == "__main__":
    sys.exit(main())
