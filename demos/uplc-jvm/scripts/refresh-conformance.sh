#!/usr/bin/env bash
# Re-vendors the Plutus conformance suite's UPLC evaluation cases into the demo's
# test resources. Only the programs (*.uplc) and their expected results
# (*.uplc.expected) are copied: the demo compares results, not budgets.
#
#   demos/uplc-jvm/scripts/refresh-conformance.sh [plutus-tag]
#
# Keep the tag in step with the plutus release the pinned Scalus version runs its
# own conformance tests against (Scalus 1.3.0 pins 1.63.0.0 in its flake.nix):
# the corpus grows new builtins and semantics with each release.
set -euo pipefail

TAG="${1:-1.63.0.0}"
DEMO="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$DEMO/src/test/resources/plutus-conformance"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

git -c advice.detachedHead=false clone --quiet --depth 1 --branch "$TAG" --filter=blob:none --sparse \
  https://github.com/IntersectMBO/plutus "$WORK/plutus"
git -C "$WORK/plutus" sparse-checkout set --no-cone \
  plutus-conformance/test-cases/uplc/evaluation /LICENSE.md /NOTICE.md
COMMIT="$(git -C "$WORK/plutus" rev-parse HEAD)"

rm -rf "$DEST"
mkdir -p "$DEST/uplc/evaluation"
(
  cd "$WORK/plutus/plutus-conformance/test-cases/uplc/evaluation"
  find . -type f \( -name '*.uplc' -o -name '*.uplc.expected' \) -print0 | LC_ALL=C sort -z |
    tar --null -T - -cf - | tar -xf - -C "$DEST/uplc/evaluation"
)
cp "$WORK/plutus/LICENSE.md" "$DEST/LICENSE.md"
cp "$WORK/plutus/NOTICE.md" "$DEST/NOTICE.md"

CASES="$(find "$DEST/uplc/evaluation" -name '*.uplc' | wc -l | tr -d ' ')"
cat > "$DEST/SOURCE.md" <<EOF
# Vendored: Plutus conformance suite (UPLC evaluation cases)

- Upstream: https://github.com/IntersectMBO/plutus/tree/$TAG/plutus-conformance/test-cases/uplc/evaluation
- Tag: \`$TAG\`, commit \`$COMMIT\`
- Cases: $CASES (\`*.uplc\` programs and their \`*.uplc.expected\` results; budget files omitted)
- License: Apache-2.0, see \`LICENSE.md\` and \`NOTICE.md\` (copied verbatim from upstream)

Regenerate with \`demos/uplc-jvm/scripts/refresh-conformance.sh [tag]\`; do not edit by hand.
EOF

echo "Vendored $CASES cases from plutus $TAG ($COMMIT) into $DEST"
