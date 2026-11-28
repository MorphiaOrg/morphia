#!/usr/bin/env bash
# Usage: audits/bin/discover.sh <docs-checkout> <out-dir> [--test]
#
# Report-only discovery against the head of a mongodb/docs checkout. In a throwaway worktree of
# HEAD it bundles and parses the checkout's operator pages with the pinned parser, runs extraction
# (which inventories the pages, writes a scenario for every new example, and refreshes each scenario
# whose example changed upstream), and, with --test, runs
# core's operator tests against the result. It then writes the findings the
# Publish job files (missing Morphia API, new examples, failing tests), and fails while any in-scope
# example has no scenario or exclusion; those are listed in missing-scenarios.txt, not filed.
# Everything lands in <out-dir>, with summary.md on top; nothing in this checkout changes and
# nothing is published. Requires a JDK 17 toolchain, uv and Python 3.12 for the pinned parser, and,
# for --test, Docker.
set -euo pipefail

[ $# -ge 2 ] || { sed -n 2,11p "$0"; exit 2; }
# Paths are anchored at this script, not the working directory.
here=$(cd "$(dirname "$0")" && pwd)
repository=$(cd "$here/../.." && pwd)
docs=$(cd "$1" && pwd)
mkdir -p "$2"
out=$(cd "$2" && pwd)
test=${3:-}
tree="$out/tree"

git -C "$repository" worktree add --detach "$tree" HEAD >/dev/null
trap 'git -C "$repository" worktree remove --force "$tree"' EXIT
cd "$tree"

mvn() { ./mvnw -B -ntp -q "$@"; }
audit() { mvn -Daudits -pl :morphia-audits exec:java -Daudits.mainClass="$1" -Dexec.args="$2"; }
export MAVEN_OPTS=-Xmx8g

mvn install -pl :morphia-core -am -DskipTests -Ddeploy.skip=true -Dinvoker.skip=true
# This checkout's extract.sh, run against the worktree: the worktree holds only committed files.
AUDITS_LABEL=weekly AUDITS_REPOSITORY="$tree" "$here/extract.sh" "$docs" "$out" > /dev/null
cp audits/catalog/documented-examples.json "$out/documented-examples.json"
git add -A -N
git diff -- audits/catalog/documented-examples.json > "$out/inventory.diff"
git diff -- core/src/test/resources > "$out/scenarios.diff"

log=-
if [ "$test" = "--test" ]; then
    log="$out/audit.log"
    : > "$log"
    # One run under the default mapper: the scenarios run against plain Document collections, so
    # the mapper doesn't change what they check, and build.yml covers mapper parity.
    mvn test -pl :morphia-core -Ddeploy.skip=true -Dinvoker.skip=true \
        -Dsurefire.failIfNoSpecifiedTests=false \
        -Dtest='dev/morphia/test/aggregation/expressions/*,dev/morphia/test/aggregation/stages/*,dev/morphia/test/query/filters/*,dev/morphia/test/query/updates/*,ExampleOwnersTest' \
        >> "$log" 2>&1 || {
        # -q hides Maven's own failure banner; the findings read this marker instead.
        echo "[DISCOVERY] the operator tests exited non-zero" >> "$log"
        echo "operator tests failed" >> "$out/test-status.txt"
    }
fi
audit dev.morphia.audits.publish.PublishMainKt "findings $tree $out/committed-inventory.json $log $out/extraction.txt" \
    > "$out/findings.json"

python3 -I - "$out" "$tree" <<'EOF'
import collections, json, os, sys
out, tree = sys.argv[1], sys.argv[2]
inventory = json.load(open(os.path.join(out, "documented-examples.json")))
report = json.load(open(os.path.join(out, "findings.json")))
findings = report["findings"]
missing = report.get("missingScenarios", [])
open(os.path.join(out, "missing-scenarios.txt"), "w").write("".join(line + "\n" for line in missing))
lines = [f"# Operator example discovery at mongodb/docs `{inventory['commit'][:10]}`", ""]
lines += ["| Measure | Count |", "| --- | --- |"]
lines += [f"| {key} | {value} |" for key, value in sorted(inventory["summary"].items())]
changed = os.path.getsize(os.path.join(out, "inventory.diff")) > 0
lines += ["", "The inventory " + ("**differs** from the committed one; see `inventory.diff`."
                                   if changed else "matches the committed one.")]
report = [line.strip() for line in open(os.path.join(out, "extraction.txt"))
          if line.startswith(("Wrote ", "Refreshed "))]
lines += ["", ("; ".join(report) + "; see `scenarios.diff`.") if report else "Extraction wrote nothing."]
lines += ["", "## Findings", ""]
lines += [f"- **{kind}**: {count}" for kind, count in collections.Counter(f["type"] for f in findings).most_common()]
# Examples with no scenario fail the run; they are listed here, not filed as issues.
lines += ["", f"## Examples with no scenario or exclusion ({len(missing)})", ""]
lines += [f"- {line}" for line in missing]
status = os.path.join(out, "test-status.txt")
if os.path.exists(status):
    lines += ["", "Operator tests: " + open(status).read().strip()]
open(os.path.join(out, "summary.md"), "w").write("\n".join(lines) + "\n")
EOF
cat "$out/summary.md"
# The run fails while any in-scope example has no scenario or exclusion; the Publish job still
# files the other findings.
missing=$(grep -c . "$out/missing-scenarios.txt" || true)
if [ "$missing" -gt 0 ]; then
    echo "$missing documented examples have no scenario or exclusion; see missing-scenarios.txt" >&2
    exit 1
fi
