"""Trusted planning and freshness checks for detached PR benchmarks.

Runs only on the default-branch checkout, never alongside PR build code.
The benchmark's artifact is data, not a source of PR numbers or SHAs.
"""

import argparse
import itertools
import json
import os
import pathlib
import re
import subprocess
import urllib.parse
import urllib.request

import bench_tools


class GitHub:
    def __init__(self, repository):
        self.repository = repository

    def get(self, path, **query):
        url = f"{os.environ.get('GITHUB_API_URL', 'https://api.github.com')}/repos/{self.repository}/{path}"
        if query:
            url += "?" + urllib.parse.urlencode(query)
        request = urllib.request.Request(url, headers={
            "Authorization": f"Bearer {os.environ['GH_TOKEN']}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
        })
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)

    def pulls(self, branch):
        # workflow_run.pull_requests can be empty even for same-repo PRs.
        for page in itertools.count(1):
            batch = self.get("pulls", state="open", base="main",
                             head=f"{self.repository.split('/')[0]}:{branch}",
                             per_page=100, page=page)
            yield from batch
            if len(batch) < 100:
                return


def current_pr(pr, repository, head=None):
    """One rule shared by planning and the last check before commenting."""
    return (
        pr.get("state") == "open"
        and pr.get("base", {}).get("ref") == "main"
        and (pr.get("head", {}).get("repo") or {}).get("full_name") == repository
        and (head is None or pr["head"]["sha"] == head)
    )


def resolve(event_name, event, github, workflow_sha):
    """Return (PR or None, mode, filter), or None when no run is eligible."""
    if event_name == "workflow_run":
        run = event["workflow_run"]
        if (run.get("event") != "pull_request" or run.get("conclusion") != "success"
                or (run.get("head_repository") or {}).get("full_name") != github.repository):
            return None
        candidates = [
            pr for pr in github.pulls(run["head_branch"])
            if current_pr(pr, github.repository, run["head_sha"])
        ]
        if len(candidates) != 1:
            return None
        return candidates[0], "ab", ""

    if event_name != "workflow_dispatch":
        raise ValueError(f"Unsupported event: {event_name}")
    inputs = event.get("inputs", {})
    mode = inputs.get("mode", "ab")
    number = inputs.get("pr", "").strip()
    regex = inputs.get("filter", "")
    if mode == "aa":
        if number:
            raise ValueError("A/A calibration uses the default branch; leave pr blank")
        checked_sha(workflow_sha)
        return None, mode, regex
    if mode != "ab" or not number.isdecimal() or int(number) < 1:
        raise ValueError("A/B requires a positive PR number; use mode=aa for calibration")
    pr = github.get(f"pulls/{int(number)}")
    if not current_pr(pr, github.repository):
        return None
    return pr, mode, regex


def checked_sha(value):
    if not re.fullmatch(r"[0-9a-f]{40}", value):
        raise ValueError("Expected an immutable Git SHA")
    return value


def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()


def plan(request, workflow_sha):
    if request is None:
        return {"run": "false"}
    pr, mode, override = request
    head = checked_sha(pr["head"]["sha"] if pr else workflow_sha)
    if mode == "aa":
        base = head
        regex = override or "FULL"
    else:
        target = checked_sha(pr["base"]["sha"])
        git("fetch", "--no-tags", "origin", head, target)
        base = checked_sha(git("merge-base", target, head))
        full = any(label["name"] == "perf:full" for label in pr.get("labels", []))
        regex = override or ("FULL" if full else bench_tools.affected(
            git("diff", "--name-only", f"{base}...{head}").splitlines()))
    return {
        "run": "true" if regex else "false",
        "pr": str(pr["number"]) if pr else "",
        "mode": mode,
        "base": base,
        "head": head,
        "regex": r".*Bench\..*" if regex == "FULL" else regex,
    }


def write_outputs(values, path):
    # Filters are operator input, not executable shell fragments. Reject
    # newlines rather than allowing extra workflow outputs to be injected.
    lines = []
    for key, value in values.items():
        if "\n" in value or "\r" in value:
            raise ValueError("Workflow outputs must be single-line values")
        lines.append(f"{key}={value}\n")
    with pathlib.Path(path).open("a", encoding="utf-8") as output:
        output.writelines(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["plan", "check"])
    args = parser.parse_args()
    github = GitHub(os.environ["GITHUB_REPOSITORY"])
    if args.command == "plan":
        event = json.loads(pathlib.Path(os.environ["GITHUB_EVENT_PATH"]).read_text())
        default_ref = f"refs/heads/{event['repository']['default_branch']}"
        if os.environ["GITHUB_REF"] != default_ref:
            raise ValueError("Dispatch the benchmark workflow from the default branch")
        values = plan(resolve(os.environ["GITHUB_EVENT_NAME"], event, github,
                              os.environ["GITHUB_SHA"]), os.environ["GITHUB_SHA"])
    else:
        number = int(os.environ["PR_NUMBER"])
        head = checked_sha(os.environ["HEAD_SHA"])
        values = {"current": str(current_pr(
            github.get(f"pulls/{number}"), github.repository, head)).lower()}
    write_outputs(values, os.environ["GITHUB_OUTPUT"])
    print(json.dumps(values))


if __name__ == "__main__":
    main()
