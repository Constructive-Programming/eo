#!/usr/bin/env python3
"""Regenerate the data tables on site/docs/quality-assurance.md.

Three tables, each written between a `<!-- BEGIN GENERATED: <id> -->` /
`<!-- END GENERATED: <id> -->` marker pair so the hand-authored prose around
them is preserved:

  - matrix   : the composition matrix as a pass (✓) / fail (✗) map, parsed
               straight from tests/.../CompositionMatrixSpec.scala (the spec is
               the single source of truth — every `typeChecks(...) must beTrue`
               is a cell that composes, every `must beFalse` a void-by-design
               cell).
  - coverage : per-package statement % and branch % with their BC/SC ratio,
               computed from the scoverage AGGREGATE report by counting
               <statement> elements directly (matches scoverage's own
               definition; doesn't trust the rate attributes).
  - mutation : per-module stryker4s results (killed / survived / no-coverage /
               compile-error / score) from each module's latest
               target/stryker4s-report/<ts>/report.json (schema v2).

Run after `sbt coverageAll` and `sbt mutationAll` (see build.sbt aliases). Both
the developer and .github/workflows/quality.yml invoke this. Idempotent:
re-running with the same inputs yields no diff.

    python3 site/tools/gen-qa-report.py            # rewrite the page in place
    python3 site/tools/gen-qa-report.py --check     # exit 1 if it would change
"""
import glob
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
PAGE = os.path.join(ROOT, "site", "docs", "quality-assurance.md")
SPEC = os.path.join(
    ROOT, "tests", "src", "test", "scala", "dev", "constructive", "eo",
    "CompositionMatrixSpec.scala",
)

# Outer/inner family order — matches the spec's row order and the optics.md
# taxonomy. `trav` = Traversal, `affold` = AffineFold.
FAMILIES = [
    "iso", "lens", "prism", "optional", "trav",
    "getter", "affold", "fold", "modify", "review", "unfold",
]

# Modules stryker mutates. value = (on-disk module dir, human label, note).
# The note is the Notes-column caveat; for modules that can't be scored it
# doubles as the "why" shown when no report.json exists. If a report later
# appears, its numbers take over and the note still annotates the row.
MUTATION_MODULES = [
    ("core", "core", "Scored against the cross-module suite in `tests/`, task-borrowed into core's Test scope by `mutationAll`."),
    ("laws", "laws", "Borrowed `tests/` suite; the negative fixtures in `UnlawfulFixturesSpec` keep the law-weakening mutants dead — see prose."),
    ("generics", "generics", "Macro code: it expands at compile time, so mutants leave no runtime footprint for the test run to cover."),
    ("schemes", "schemes", ""),
    ("schemes-laws", "schemes-laws", "Recursion-scheme laws (hylo fusion so far; more expected). Like `laws`, mutating it probes whether the law spec notices a corrupted law."),
    ("circe", "circe", ""),
    ("avro", "avro", "Not scored: stryker's forked test-runner fails to initialise in the sandbox (the specs pass under plain `sbt test`)."),
    ("jsoniter", "jsoniter", "Not scored: instrumenting `PathParser.parseField` blows the JVM 64 KB method-size limit — one giant byte-cursor method, un-mutatable in place."),
]


def splice(page: str, marker: str, body: str) -> str:
    """Replace the content between BEGIN/END GENERATED markers for `marker`."""
    begin = f"<!-- BEGIN GENERATED: {marker} -->"
    end = f"<!-- END GENERATED: {marker} -->"
    pat = re.compile(re.escape(begin) + r".*?" + re.escape(end), re.DOTALL)
    if not pat.search(page):
        raise SystemExit(f"marker pair for '{marker}' not found in {PAGE}")
    # The blank lines are load-bearing: Laika only recognises the pipe table
    # as a table if it starts its own block — glued to the marker line it
    # parses as paragraph continuation text.
    return pat.sub(f"{begin}\n\n{body}\n\n{end}", page)


# --------------------------------------------------------------------------
# matrix
# --------------------------------------------------------------------------
def gen_matrix() -> str:
    src = open(SPEC, encoding="utf-8").read()
    # Cell titles read `"<outer> ∘ <inner> ..." >>`; the verdict is the first
    # `typeChecks("...andThen...") must beTrue|beFalse` inside the cell block.
    cell_re = re.compile(r'"(\w+)\s*∘\s*(\w+)[^"]*"\s*>>')
    verdict_re = re.compile(r"must\s+(beTrue|beFalse)")
    cells = {}
    matches = list(cell_re.finditer(src))
    for i, m in enumerate(matches):
        outer, inner = m.group(1), m.group(2)
        end = matches[i + 1].start() if i + 1 < len(matches) else len(src)
        block = src[m.end():end]
        v = verdict_re.search(block)
        if v:
            cells[(outer, inner)] = v.group(1) == "beTrue"

    head = "| outer ∘ inner | " + " | ".join(FAMILIES) + " |"
    sep = "|" + "---|" * (len(FAMILIES) + 1)
    rows = [head, sep]
    n_pass = n_fail = 0
    for o in FAMILIES:
        line = [f"**{o}**"]
        for i in FAMILIES:
            ok = cells.get((o, i))
            if ok is None:
                line.append("·")
            elif ok:
                line.append("✓")
                n_pass += 1
            else:
                line.append("✗")
                n_fail += 1
        rows.append("| " + " | ".join(line) + " |")
    legend = (
        f"\n*✓ composes import-free at the strength shown in the "
        f"[optic taxonomy](optics.md); ✗ does not compile (void by design — "
        f"building through a read-only optic, reading through a write-only one, "
        f"etc.). {n_pass} composing / {n_fail} void cells, pinned by "
        f"`CompositionMatrixSpec`.*"
    )
    return "\n".join(rows) + "\n" + legend


# --------------------------------------------------------------------------
# coverage
# --------------------------------------------------------------------------
def find_scoverage_xml() -> str:
    pats = [
        os.path.join(ROOT, "target", "scala-*", "scoverage-report", "scoverage.xml"),
    ]
    hits = []
    for p in pats:
        hits += glob.glob(p)
    if not hits:
        return ""
    return max(hits, key=os.path.getmtime)


def gen_coverage() -> str:
    xml = find_scoverage_xml()
    if not xml:
        return ("> _No scoverage aggregate report found. Run `sbt coverageAll`, "
                "then re-run `site/tools/gen-qa-report.py`._")
    root = ET.parse(xml).getroot()
    rows = [
        "| Package | Statements | Stmt&nbsp;% | Branches | Branch&nbsp;% | BC/SC |",
        "|---|--:|--:|--:|--:|--:|",
    ]
    packages = root.find("packages")
    pkgs = packages.findall("package") if packages is not None else []
    for pkg in sorted(pkgs, key=lambda p: p.get("name", "")):
        name = pkg.get("name", "")
        stmts = pkg.findall(".//statement")
        total = len(stmts)
        if total == 0:
            continue
        invoked = sum(1 for s in stmts if int(s.get("invocation-count", "0")) > 0)
        bstmts = [s for s in stmts if s.get("branch", "false") == "true"]
        btotal = len(bstmts)
        binvoked = sum(1 for s in bstmts if int(s.get("invocation-count", "0")) > 0)
        srate = 100.0 * invoked / total
        if btotal:
            brate = 100.0 * binvoked / btotal
            bcell = f"{binvoked}/{btotal}"
            bpct = f"{brate:.1f}%"
            ratio = f"{brate / srate:.2f}" if srate else "—"
        else:
            bcell, bpct, ratio = "—", "—", "—"
        rows.append(
            f"| `{name}` | {invoked}/{total} | {srate:.1f}% | {bcell} | {bpct} | {ratio} |"
        )
    return "\n".join(rows)


# --------------------------------------------------------------------------
# mutation
# --------------------------------------------------------------------------
def latest_report(module_dir: str) -> str:
    pat = os.path.join(ROOT, module_dir, "target", "stryker4s-report", "*", "report.json")
    hits = glob.glob(pat)
    return max(hits, key=os.path.getmtime) if hits else ""


# circe/avro/jsoniter sbt ids map to these on-disk directories.
MODULE_DIR = {
    "core": "core", "laws": "laws", "generics": "generics", "schemes": "schemes",
    "schemes-laws": "schemes-laws",
    "circe": "circe", "avro": "avro", "jsoniter": "jsoniter",
}


def gen_mutation() -> str:
    rows = [
        "| Module | Killed | Timeout | Survived | No&nbsp;cov | Compile&nbsp;err | Score (total) | Score (covered) | Notes |",
        "|---|--:|--:|--:|--:|--:|--:|--:|---|",
    ]
    any_report = False
    for proj, label, note in MUTATION_MODULES:
        rep = latest_report(MODULE_DIR[proj])
        if not rep:
            why = note if note else "_no report — run `sbt mutationAll`_"
            rows.append(f"| `{label}` | — | — | — | — | — | — | — | {why} |")
            continue
        any_report = True
        data = json.load(open(rep, encoding="utf-8"))
        counts = {}
        for f in data.get("files", {}).values():
            for m in f.get("mutants", []):
                counts[m["status"]] = counts.get(m["status"], 0) + 1
        killed = counts.get("Killed", 0)
        survived = counts.get("Survived", 0)
        nocov = counts.get("NoCoverage", 0)
        timeout = counts.get("Timeout", 0)
        cerr = counts.get("CompileError", 0)
        detected = killed + timeout
        scored = detected + survived + nocov
        covered = detected + survived
        total_score = f"{100.0 * detected / scored:.1f}%" if scored else "—"
        cov_score = f"{100.0 * detected / covered:.1f}%" if covered else "—"
        rows.append(
            f"| `{label}` | {killed} | {timeout} | {survived} | {nocov} | {cerr} | "
            f"{total_score} | {cov_score} | {note} |"
        )
    if not any_report:
        return ("> _No stryker4s reports found. Run `sbt mutationAll`, then "
                "re-run `site/tools/gen-qa-report.py`._")
    return "\n".join(rows)


def main() -> int:
    check = "--check" in sys.argv[1:]
    page = open(PAGE, encoding="utf-8").read()
    out = page
    out = splice(out, "matrix", gen_matrix())
    out = splice(out, "coverage", gen_coverage())
    out = splice(out, "mutation", gen_mutation())
    if out == page:
        print("quality-assurance.md already up to date.")
        return 0
    if check:
        print("quality-assurance.md is OUT OF DATE (run gen-qa-report.py).")
        return 1
    open(PAGE, "w", encoding="utf-8").write(out)
    print(f"Wrote {PAGE}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
