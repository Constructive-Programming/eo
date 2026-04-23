#!/usr/bin/env bash
# install.sh — point this repo's git at `.githooks/` so the committed
# pre-commit / pre-push scripts run automatically. Run once per clone.

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

chmod +x .githooks/pre-commit .githooks/pre-push
git config core.hooksPath .githooks

echo "[.githooks] installed — git will now use \`.githooks/pre-commit\` and"
echo "[.githooks] \`.githooks/pre-push\`. Bypass any one commit with"
echo "[.githooks] \`git commit --no-verify\` / \`git push --no-verify\`."
