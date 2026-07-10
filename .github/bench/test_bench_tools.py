"""Unit tests for bench_tools.py (stdlib unittest, no dependencies).

Run:  python3 -m unittest discover .github/bench -v
"""

import json
import math
import pathlib
import tempfile
import unittest

import bench_tools as bt


def jmh_entry(fqn, ns, bop=None, params=None, err=2.0):
    e = {
        "benchmark": fqn,
        "params": params or {},
        "primaryMetric": {"score": ns, "scoreError": err, "scoreUnit": "ns/op"},
    }
    if bop is not None:
        e["secondaryMetrics"] = {bt.GC_NORM: {"score": bop, "scoreUnit": "B/op"}}
    return e


def load(entries):
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as f:
        json.dump(entries, f)
        p = pathlib.Path(f.name)
    return bt.load_results(p)


FQN = "dev.constructive.eo.bench.FoldBench.eoSum"
FQN2 = "dev.constructive.eo.bench.FoldBench.mSum"


class Affected(unittest.TestCase):
    def test_leaf_module_maps_to_its_benches_only(self):
        rx = bt.affected(["avro/src/main/scala/Foo.scala"])
        self.assertIn("OrderAvroBench", rx)
        self.assertNotIn("SchemesBench", rx)
        self.assertNotIn("FULL", rx)

    def test_cross_module_bench_included_for_jsoniter(self):
        rx = bt.affected(["jsoniter/src/main/scala/X.scala"])
        self.assertIn("AvroJsonBridgeBench", rx)  # imports jsoniter too

    def test_core_means_full(self):
        self.assertEqual(bt.affected(["core/src/main/scala/Optic.scala"]), "FULL")

    def test_build_sbt_and_project_mean_full(self):
        self.assertEqual(bt.affected(["build.sbt"]), "FULL")
        self.assertEqual(bt.affected(["project/plugins.sbt"]), "FULL")

    def test_bench_harness_means_full(self):
        self.assertEqual(bt.affected(["benchmarks/src/main/scala/B.scala"]), "FULL")

    def test_docs_only_skips(self):
        self.assertEqual(bt.affected(["docs/plans/x.md", "README.md", ".github/workflows/ci.yml"]), "")

    def test_unknown_path_is_conservative_full(self):
        # A new module with no mapping entry must never silently skip (R2).
        self.assertEqual(bt.affected(["msgpack/src/main/scala/M.scala"]), "FULL")

    def test_mixed_leaf_paths_union(self):
        rx = bt.affected(["schemes/src/X.scala", "generics/src/Y.scala"])
        self.assertIn("SchemesBench", rx)
        self.assertIn("GenericsBench", rx)


class ValidateMapping(unittest.TestCase):
    def test_real_repo_is_healthy(self):
        root = pathlib.Path(__file__).resolve().parents[2]
        self.assertEqual(bt.validate_mapping(root), [])

    def test_missing_class_and_path_reported(self):
        with tempfile.TemporaryDirectory() as d:
            root = pathlib.Path(d)
            bench = root / bt.BENCH_SRC
            bench.mkdir(parents=True)
            (bench / "OrderAvroBench.scala").touch()
            errors = bt.validate_mapping(root)
        joined = "\n".join(errors)
        self.assertIn("mapped path does not exist: avro/", joined)
        self.assertIn("SchemesBench", joined)  # class with no source file


class Diff(unittest.TestCase):
    def test_pairs_with_signed_deltas(self):
        base = load([jmh_entry(FQN, 100.0, bop=40.0)])
        head = load([jmh_entry(FQN, 150.0, bop=24.0)])
        d = bt.diff(base, head, {}, None)
        (p,) = d["pairs"]
        self.assertAlmostEqual(p["ns_pct"], 50.0)
        self.assertAlmostEqual(p["bop_pct"], -40.0)
        self.assertAlmostEqual(p["bop_delta"], -16.0)

    def test_params_pair_independently(self):
        base = load(
            [
                jmh_entry(FQN, 10, bop=8, params={"size": "10"}),
                jmh_entry(FQN, 99, bop=80, params={"size": "1000"}),
            ]
        )
        head = load(
            [
                jmh_entry(FQN, 20, bop=8, params={"size": "10"}),
                jmh_entry(FQN, 99, bop=80, params={"size": "1000"}),
            ]
        )
        d = bt.diff(base, head, {}, None)
        pcts = {p["params"]["size"]: p["ns_pct"] for p in d["pairs"]}
        self.assertAlmostEqual(pcts["10"], 100.0)
        self.assertAlmostEqual(pcts["1000"], 0.0)

    def test_new_removed_and_rename_suspect(self):
        base = load([jmh_entry("a.b.OldBench.eoGet", 10, bop=8)])
        head = load([jmh_entry("a.b.NewBench.eoGet", 10, bop=8)])
        d = bt.diff(base, head, {}, None)
        self.assertEqual(len(d["new"]), 1)
        self.assertEqual(len(d["removed"]), 1)
        self.assertEqual(d["rename_suspects"], ["eoGet"])

    def test_missing_gc_metric_pairs_with_none(self):
        base = load([jmh_entry(FQN, 100.0)])
        head = load([jmh_entry(FQN, 100.0)])
        (p,) = bt.diff(base, head, {}, None)["pairs"]
        self.assertIsNone(p["bop_pct"])

    def test_gate_absent_thresholds_is_advisory(self):
        base = load([jmh_entry(FQN, 100, bop=40)])
        head = load([jmh_entry(FQN, 100, bop=400)])
        d = bt.diff(base, head, {}, None)
        self.assertFalse(d["gate"]["enabled"])
        self.assertEqual(d["gate"]["violations"], [])

    def test_gate_fires_beyond_threshold(self):
        base = load([jmh_entry(FQN, 100, bop=40)])
        head = load([jmh_entry(FQN, 100, bop=80)])
        d = bt.diff(base, head, {}, {"bop_regression_pct": 2.0, "bop_min_delta_bytes": 16})
        self.assertEqual(d["gate"]["violations"], ["FoldBench.eoSum"])

    def test_gate_respects_min_delta_floor(self):
        base = load([jmh_entry(FQN, 100, bop=4)])
        head = load([jmh_entry(FQN, 100, bop=8)])  # +100% but only 4 bytes
        d = bt.diff(base, head, {}, {"bop_regression_pct": 2.0, "bop_min_delta_bytes": 16})
        self.assertEqual(d["gate"]["violations"], [])

    def test_zero_base_gives_inf_not_crash(self):
        base = load([jmh_entry(FQN, 100, bop=0.0)])
        head = load([jmh_entry(FQN, 100, bop=8.0)])
        (p,) = bt.diff(base, head, {}, None)["pairs"]
        self.assertTrue(math.isinf(p["bop_pct"]))


class CommentMd(unittest.TestCase):
    def _deltas(self, **kw):
        base = load([jmh_entry(FQN, 100, bop=40)])
        head = load([jmh_entry(FQN, 110, bop=48)])
        return bt.diff(base, head, {"base_sha": "abc1234", "head_sha": "def5678"}, None)

    def test_happy_path_tables_and_provenance(self):
        md = bt.comment_md(self._deltas(), None)
        self.assertIn("Allocation (B/op) — authoritative", md)
        self.assertIn("directional", md)
        self.assertIn("+20.0%", md)  # bop
        self.assertIn("+10.0%", md)  # ns
        self.assertIn("abc1234", md)

    def test_failure_state_is_loud_never_a_pass(self):
        md = bt.comment_md(None, "head: sbt Jmh/run failed to compile")
        self.assertIn("❌", md)
        self.assertIn("explicit failure state", md)
        self.assertNotIn("Allocation", md)

    def test_untrusted_names_are_escaped(self):
        evil = "a.b.XBench.get](http://x)<script>alert(1)</script>|pwn"
        base = load([jmh_entry(evil, 1, bop=1)])
        head = load([jmh_entry(evil, 1, bop=1)])
        md = bt.comment_md(bt.diff(base, head, {}, None), None)
        self.assertNotIn("<script>", md)
        self.assertNotIn("](http://x)", md)

    def test_gate_violation_banner(self):
        base = load([jmh_entry(FQN, 100, bop=40)])
        head = load([jmh_entry(FQN, 100, bop=80)])
        d = bt.diff(base, head, {}, {"bop_regression_pct": 2.0, "bop_min_delta_bytes": 16})
        md = bt.comment_md(d, None)
        self.assertIn("🚨", md)


class BenchmarksMd(unittest.TestCase):
    def test_eo_monocle_paired_row(self):
        res = load([jmh_entry(FQN, 10, bop=8), jmh_entry(FQN2, 20, bop=16)])
        md = bt.benchmarks_md(res, {"source_sha": "abc"})
        row = next(l for l in md.splitlines() if "`Sum`" in l)
        self.assertIn("10", row)
        self.assertIn("20", row)  # both sides on one row

    def test_unpaired_bench_renders_with_dashes(self):
        res = load([jmh_entry("a.b.CapsBench.viaCanGet", 10, bop=8)])
        md = bt.benchmarks_md(res, {})
        self.assertIn("`viaCanGet`", md)
        self.assertIn("—", md)

    def test_missing_gc_metric_fails_loudly(self):
        res = load([jmh_entry(FQN, 10)])  # no -prof gc
        with self.assertRaises(SystemExit):
            bt.benchmarks_md(res, {})

    def test_disclaimer_and_generated_marker(self):
        res = load([jmh_entry(FQN, 10, bop=8)])
        md = bt.benchmarks_md(res, {})
        self.assertIn("Generated file — do not edit", md)
        self.assertIn("directional", md)


class Series(unittest.TestCase):
    def test_single_valid_json_line_with_provenance(self):
        res = load([jmh_entry(FQN, 10.0, bop=8.0)])
        line = bt.series_line(res, {"source_sha": "abc", "profile": "full-f3"})
        self.assertNotIn("\n", line)
        obj = json.loads(line)
        self.assertEqual(obj["source_sha"], "abc")
        self.assertEqual(obj["results"][0]["bop"], 8.0)


class NoiseReport(unittest.TestCase):
    def _aa(self, profile, jitter):
        base = load([jmh_entry(FQN, 100.0, bop=40.0)])
        head = load([jmh_entry(FQN, 100.0 + jitter, bop=40.0)])
        return bt.diff(base, head, {"profile": profile}, None)

    def test_identical_runs_zero_floor(self):
        rep = bt.noise_report([self._aa("p1", 0.0)])
        self.assertIn("0.0%", rep)

    def test_quantiles_over_runs(self):
        rep = bt.noise_report([self._aa("p1", 5.0), self._aa("p1", 10.0)])
        self.assertIn("FoldBench", rep)

    def test_mismatched_profiles_refused(self):
        with self.assertRaises(SystemExit):
            bt.noise_report([self._aa("p1", 1.0), self._aa("p2", 1.0)])


if __name__ == "__main__":
    unittest.main()
