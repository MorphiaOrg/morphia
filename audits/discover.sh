#!/usr/bin/env bash
# Usage: audits/discover.sh <docs-checkout> <out-dir> [--test]
#
# Report-only discovery against the head of a mongodb/docs checkout. In a throwaway worktree of
# HEAD it bundles and parses the checkout's operator pages with the pinned parser, runs extraction
# (which inventories the pages and writes a scenario for every new example), and, with --test, runs
# core's operator tests under both mappers against the result. It then writes the findings the
# Publish job files: missing Morphia API, new examples, examples with no scenario, failing tests.
# Everything lands in <out-dir>, with summary.md on top; nothing in this checkout changes and
# nothing is published. Requires a JDK 17 toolchain, uv and Python 3.12 for the pinned parser, and,
# for --test, Docker.
set -euo pipefail

[ $# -ge 2 ] || { sed -n 2,11p "$0"; exit 2; }
repository=$(git rev-parse --show-toplevel)
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
# Exit status 3 means the step finished and reported what it couldn't do.
partial() { "$@" || [ $? -eq 3 ]; }
export MAVEN_OPTS=-Xmx8g

mvn install -pl :morphia-core -am -DskipTests -Ddeploy.skip=true -Dinvoker.skip=true
mvn -Daudits -pl :morphia-audits compile
uv sync --frozen --project audits/parser -q
cp audits/catalog/documented-examples.json "$out/committed-inventory.json"

# The pinned source bundle of the checkout's manual, at the checkout's commit.
python3 -I - "$docs" "$out/sources-request.json" <<'EOF'
import json, subprocess, sys
request = json.load(open("audits/config/manual-baseline-sources.json"))
request["revision"] = subprocess.run(["git", "-C", sys.argv[1], "rev-parse", "HEAD"], check=True,
                                     capture_output=True, text=True).stdout.strip()
request["id"] = "manual-weekly"
request["label"] = "weekly"
json.dump(request, open(sys.argv[2], "w"), indent=2)
EOF
audit dev.morphia.audits.sources.SourcesMainKt "$out/sources-request.json $out/bundle --repository $docs" \
    > "$out/sources.json"
content=$(python3 -I -c 'import json,sys; print(json.load(open(sys.argv[1]))["manifest"]["contentSha256"])' "$out/sources.json")

pages=$(python3 -I - "$out/sources.json" <<'EOF'
import json, re, sys
manifest = json.load(open(sys.argv[1]))["manifest"]
source = manifest["request"]["projectRoot"] + "/source/"
print(" ".join(sorted(r["path"][len(source):] for r in manifest["resources"]
                      if re.fullmatch(re.escape(source) + r"reference/operator/(aggregation|query|update)/[^/]+\.txt", r["path"]))))
EOF
)
# shellcheck disable=SC2086
partial audit dev.morphia.audits.documents.DocumentsMainKt \
    "$out/bundle $content $tree/audits/parser/.venv/bin/python $tree/audits/parser/adapter.py $pages" \
    > "$out/documents.json"
partial audit dev.morphia.audits.extraction.ExtractMainKt \
    "$tree $out/bundle $content $out/documents.json" > "$out/extraction.txt"
rm -rf "$out/bundle"
cp audits/catalog/documented-examples.json "$out/documented-examples.json"
git add -A -N
git diff -- audits/catalog/documented-examples.json > "$out/inventory.diff"
git diff -- core/src/test/resources > "$out/scenarios.diff"

log=-
if [ "$test" = "--test" ]; then
    log="$out/audit.log"
    : > "$log"
    for mapper in critter reflection; do
        mvn test -pl :morphia-core -Dmorphia.mapper=$mapper -Ddeploy.skip=true -Dinvoker.skip=true \
            -Dsurefire.failIfNoSpecifiedTests=false \
            -Dtest='dev.morphia.test.aggregation.expressions.*,dev.morphia.test.aggregation.stages.*,dev.morphia.test.query.filters.*,dev.morphia.test.query.updates.*,ExampleOwnersTest' \
            >> "$log" 2>&1 || echo "$mapper: operator tests failed" >> "$out/test-status.txt"
    done
fi
audit dev.morphia.audits.publish.PublishMainKt "findings $tree $out/committed-inventory.json $log" \
    > "$out/findings.json"

python3 -I - "$out" "$tree" <<'EOF'
import collections, json, os, sys
out, tree = sys.argv[1], sys.argv[2]
inventory = json.load(open(os.path.join(out, "documented-examples.json")))
findings = json.load(open(os.path.join(out, "findings.json")))
lines = [f"# Operator example discovery at mongodb/docs `{inventory['commit'][:10]}`", ""]
lines += ["| Measure | Count |", "| --- | --- |"]
lines += [f"| {key} | {value} |" for key, value in sorted(inventory["summary"].items())]
changed = os.path.getsize(os.path.join(out, "inventory.diff")) > 0
lines += ["", "The inventory " + ("**differs** from the committed one; see `inventory.diff`."
                                   if changed else "matches the committed one.")]
written = [line for line in open(os.path.join(out, "extraction.txt")) if line.startswith("Wrote ")]
lines += ["", written[0].strip() + "; see `scenarios.diff`." if written else "Extraction wrote nothing."]
lines += ["", "## Findings", ""]
lines += [f"- **{kind}**: {count}" for kind, count in collections.Counter(f["type"] for f in findings).most_common()]
status = os.path.join(out, "test-status.txt")
if os.path.exists(status):
    lines += ["", "Operator tests: " + open(status).read().strip()]
open(os.path.join(out, "summary.md"), "w").write("\n".join(lines) + "\n")
EOF
cat "$out/summary.md"
