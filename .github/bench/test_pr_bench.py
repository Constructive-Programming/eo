"""Detached benchmark planning tests; no network, Git fetches, or sbt."""

import copy
import json
import os
import pathlib
import tempfile
import unittest
from unittest.mock import Mock, patch

import pr_bench as pb


REPOSITORY = "Constructive-Programming/eo"
HEAD = "a" * 40
BASE = "b" * 40
MERGE_BASE = "c" * 40
PR = {
    "number": 129,
    "state": "open",
    "head": {"sha": HEAD, "repo": {"full_name": REPOSITORY}},
    "base": {"ref": "main", "sha": BASE},
    "labels": [],
}
EVENT = {
    "workflow_run": {
        "event": "pull_request",
        "conclusion": "success",
        "head_repository": {"full_name": REPOSITORY},
        "head_branch": "feature/indexed",
        "head_sha": HEAD,
        "pull_requests": [],
    },
}


class Resolve(unittest.TestCase):
    def setUp(self):
        self.api = Mock(repository=REPOSITORY)
        self.api.pulls.return_value = [copy.deepcopy(PR)]
        self.api.get.return_value = copy.deepcopy(PR)

    def test_resolves_even_when_workflow_run_pr_array_is_empty(self):
        result = pb.resolve("workflow_run", EVENT, self.api, BASE)
        self.assertEqual(result, (PR, "ab", ""))
        self.api.pulls.assert_called_once_with("feature/indexed")

    def test_ignores_push_failed_cancelled_and_fork_ci_without_api_access(self):
        for field, value in [
            ("event", "push"),
            ("conclusion", "failure"),
            ("conclusion", "cancelled"),
            ("head_repository", {"full_name": "someone/eo"}),
            ("head_repository", None),
        ]:
            with self.subTest(field=field, value=value):
                event = copy.deepcopy(EVENT)
                event["workflow_run"][field] = value
                self.assertIsNone(pb.resolve("workflow_run", event, self.api, BASE))
        self.api.pulls.assert_not_called()

    def test_stale_closed_wrong_base_fork_and_deleted_repo_prs_are_rejected(self):
        for field, value in [
            ("state", "closed"),
            ("base", {"ref": "release"}),
            ("head", {"sha": BASE, "repo": {"full_name": REPOSITORY}}),
            ("head", {"sha": HEAD, "repo": {"full_name": "someone/eo"}}),
            ("head", {"sha": HEAD, "repo": None}),
        ]:
            with self.subTest(field=field, value=value):
                pr = copy.deepcopy(PR)
                pr[field] = value
                self.assertFalse(pb.current_pr(pr, REPOSITORY, HEAD))
                self.api.pulls.return_value = [pr]
                self.assertIsNone(pb.resolve("workflow_run", EVENT, self.api, BASE))

    def test_does_not_guess_when_no_pr_or_ambiguous_prs_match(self):
        for candidates in [[], [PR, PR]]:
            self.api.pulls.return_value = candidates
            self.assertIsNone(pb.resolve("workflow_run", EVENT, self.api, BASE))

    def test_manual_ab_resolves_pr_and_preserves_filter_as_data(self):
        result = pb.resolve("workflow_dispatch", {
            "inputs": {"pr": "129", "mode": "ab", "filter": ".*LensBench.*"},
        }, self.api, BASE)
        self.assertEqual(result, (PR, "ab", ".*LensBench.*"))
        self.api.get.assert_called_once_with("pulls/129")

    def test_manual_fork_pr_is_rejected(self):
        self.api.get.return_value["head"]["repo"]["full_name"] = "someone/eo"
        self.assertIsNone(pb.resolve("workflow_dispatch", {
            "inputs": {"pr": "129"},
        }, self.api, BASE))

    def test_manual_invalid_or_missing_pr_fails_clearly(self):
        for number in ["", "0", "-1", "129\nhead=bad", "not-a-number"]:
            with self.subTest(number=number), self.assertRaises(ValueError):
                pb.resolve("workflow_dispatch", {"inputs": {"pr": number}}, self.api, BASE)

    def test_aa_does_not_query_pr_and_rejects_accidental_pr_input(self):
        result = pb.resolve("workflow_dispatch", {"inputs": {"mode": "aa"}}, self.api, BASE)
        self.assertEqual(result, (None, "aa", ""))
        self.api.get.assert_not_called()
        with self.assertRaises(ValueError):
            pb.resolve("workflow_dispatch", {
                "inputs": {"mode": "aa", "pr": "129"},
            }, self.api, BASE)

    def test_paginated_branch_lookup_keeps_same_repo_and_main_filters(self):
        api = pb.GitHub(REPOSITORY)
        with patch.object(api, "get", side_effect=[[PR] * 100, [PR]]) as get:
            self.assertEqual(len(list(api.pulls("feature/indexed"))), 101)
        self.assertEqual(get.call_count, 2)
        self.assertEqual(get.call_args.kwargs, {
            "state": "open", "base": "main",
            "head": "Constructive-Programming:feature/indexed",
            "per_page": 100, "page": 2,
        })


class Plan(unittest.TestCase):
    def planned(self, paths, labels=(), override=""):
        pr = copy.deepcopy(PR)
        pr["labels"] = [{"name": label} for label in labels]
        with patch.object(pb, "git", side_effect=["", MERGE_BASE, "\n".join(paths)]) as git:
            result = pb.plan((pr, "ab", override), BASE)
        git.assert_any_call("fetch", "--no-tags", "origin", HEAD, BASE)
        git.assert_any_call("merge-base", BASE, HEAD)
        return result

    def test_raw_pr_head_and_merge_base_are_immutable(self):
        result = self.planned(["core/src/main/scala/Optic.scala"])
        self.assertEqual(result, {
            "run": "true", "pr": "129", "mode": "ab",
            "head": HEAD, "base": MERGE_BASE, "regex": r".*Bench\..*",
        })

    def test_docs_only_skip_and_module_mapping_are_preserved(self):
        self.assertEqual(self.planned(["site/docs/indexed.md"])["run"], "false")
        result = self.planned(["avro/src/main/scala/AvroCodec.scala"])
        self.assertEqual(result["run"], "true")
        self.assertIn("OrderAvroBench", result["regex"])
        self.assertNotIn("SchemesBench", result["regex"])

    def test_full_label_and_explicit_override_take_precedence(self):
        result = self.planned(["README.md"], labels=["perf:full"])
        self.assertEqual(result["regex"], r".*Bench\..*")
        result = self.planned(["core/Foo.scala"], labels=["perf:full"], override=".*LensBench.*")
        self.assertEqual(result["regex"], ".*LensBench.*")

    def test_aa_runs_same_default_branch_sha_twice_without_git(self):
        with patch.object(pb, "git") as git:
            result = pb.plan((None, "aa", ""), BASE)
        git.assert_not_called()
        self.assertEqual(result["head"], result["base"])
        self.assertEqual(result["pr"], "")
        self.assertEqual(result["run"], "true")

    def test_ineligible_request_does_not_fetch(self):
        with patch.object(pb, "git") as git:
            self.assertEqual(pb.plan(None, BASE), {"run": "false"})
        git.assert_not_called()

    def test_refs_and_shell_fragments_are_not_accepted_as_shas(self):
        for value in ["main", "a" * 39, HEAD + "\n", "-x", "$(echo bad)"]:
            with self.subTest(value=value), self.assertRaises(ValueError):
                pb.checked_sha(value)

    def test_output_injection_is_rejected_before_any_output_is_written(self):
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory) / "outputs"
            for value in ["x\npr=5", "x\rpr=5"]:
                with self.assertRaises(ValueError):
                    pb.write_outputs({"run": "true", "regex": value}, path)
                self.assertFalse(path.exists())
            pb.write_outputs({"run": "true", "regex": ".*LensBench.*"}, path)
            self.assertEqual(path.read_text(), "run=true\nregex=.*LensBench.*\n")


class CommandLine(unittest.TestCase):
    def test_plan_rejects_feature_branch_dispatch_before_resolving_pr(self):
        with tempfile.TemporaryDirectory() as directory:
            event_path = pathlib.Path(directory) / "event.json"
            event_path.write_text(json.dumps({
                "repository": {"default_branch": "main"},
                "inputs": {"mode": "aa"},
            }))
            with patch.dict(os.environ, {
                "GITHUB_REPOSITORY": REPOSITORY,
                "GITHUB_EVENT_PATH": str(event_path),
                "GITHUB_REF": "refs/heads/feature/indexed",
            }), patch("sys.argv", ["pr_bench.py", "plan"]), patch.object(pb, "resolve") as resolve:
                with self.assertRaisesRegex(ValueError, "default branch"):
                    pb.main()
                resolve.assert_not_called()

    def test_final_check_requeries_live_pr_and_drops_changed_or_closed_heads(self):
        for change, expected in [(None, "true"), ("head", "false"), ("closed", "false")]:
            with self.subTest(change=change), tempfile.TemporaryDirectory() as directory:
                output = pathlib.Path(directory) / "output"
                pr = copy.deepcopy(PR)
                if change == "head":
                    pr["head"]["sha"] = BASE
                elif change == "closed":
                    pr["state"] = "closed"
                api = Mock(repository=REPOSITORY)
                api.get.return_value = pr
                with patch.dict(os.environ, {
                    "GITHUB_REPOSITORY": REPOSITORY,
                    "PR_NUMBER": "129",
                    "HEAD_SHA": HEAD,
                    "GITHUB_OUTPUT": str(output),
                }), patch("sys.argv", ["pr_bench.py", "check"]), patch.object(pb, "GitHub", return_value=api):
                    pb.main()
                api.get.assert_called_once_with("pulls/129")
                self.assertEqual(output.read_text(), f"current={expected}\n")


if __name__ == "__main__":
    unittest.main()
