#!/usr/bin/env bash
# Usage: audits/discover.sh <docs-checkout> <out-dir> [--integrate]
#
# Report-only discovery of documented operator examples. In a throwaway worktree of HEAD it
# inventories the checkout's operator pages, diffs that inventory against the committed one,
# generates a candidate with its gaps and, with --integrate, runs the candidate's tests under both
# mappers. It then compares the manual's capabilities with the accepted baseline (coverage.json).
# Everything lands in <out-dir>, with summary.md on top; nothing in this checkout changes and
# nothing is published. Requires a JDK 17 toolchain, uv and Python 3.12 for the pinned parser,
# and, for --integrate, Docker.
set -euo pipefail

[ $# -ge 2 ] || { sed -n 2,9p "$0"; exit 2; }
repository=$(git rev-parse --show-toplevel)
docs=$(cd "$1" && pwd)
mkdir -p "$2"
out=$(cd "$2" && pwd)
integrate=${3:-}
tree="$out/tree"

git -C "$repository" worktree add --detach "$tree" HEAD >/dev/null
trap 'git -C "$repository" worktree remove --force "$tree"' EXIT
cd "$tree"

mvn() { ./mvnw -B -ntp -q "$@"; }
generate() {
    mvn -Daudits -pl :morphia-audits exec:java -Daudits.mainClass=dev.morphia.audits.generate.GenerateMainKt \
        -Dexec.args="$*"
}

python3 audits/probes/form-sources/fetch.py local "$docs" "$out/current.json" "$out/pages"
mvn install -pl :morphia-core -am -DskipTests -Ddeploy.skip=true -Dinvoker.skip=true
mvn -Daudits -pl :morphia-audits compile
generate inventory "$tree" "$out/pages" "$out/current.json"
cp audits/catalog/documented-examples.json "$out/"
git diff -- audits/catalog/documented-examples.json > "$out/inventory.diff"

mvn dependency:build-classpath -pl :morphia-core -Dmdep.outputFile="$out/core-classpath.txt"
generate candidate "$tree" "$out/candidate" "$out/core-classpath.txt"
status=0
if [ "$integrate" = "--integrate" ]; then
    generate integrate "$tree" "$out/candidate" || status=$?
fi

# Capability coverage against the accepted baseline: the operators, contexts and versions the
# manual declares at the checkout's commit, compared with audits/catalog/coverage-plan.json and
# core's API. Exit status 3 (PARTIAL) still writes a report; discovery problems are in it.
partial() { "$@" || [ $? -eq 3 ]; }
python3 -I - "$docs" "$out/sources-request.json" <<'EOF'
import json, subprocess, sys
request = json.load(open("audits/config/manual-baseline-sources.json"))
request["revision"] = subprocess.run(["git", "-C", sys.argv[1], "rev-parse", "HEAD"], check=True,
                                     capture_output=True, text=True).stdout.strip()
request["id"] = "manual-weekly"
request["label"] = "weekly"
json.dump(request, open(sys.argv[2], "w"), indent=2)
EOF
uv sync --frozen --project audits/parser -q
audit() { mvn -Daudits -pl :morphia-audits exec:java -Daudits.mainClass="$1" -Dexec.args="$2"; }
audit dev.morphia.audits.sources.SourcesMainKt "$out/sources-request.json $out/bundle --repository $docs" \
    > "$out/sources.json"
content=$(python3 -I -c 'import json,sys; print(json.load(open(sys.argv[1]))["manifest"]["contentSha256"])' "$out/sources.json")
export MAVEN_OPTS=-Xmx6g
partial audit dev.morphia.audits.discovery.DiscoveryMainKt \
    "$out/bundle $content --parse $tree/audits/parser/.venv/bin/python $tree/audits/parser/adapter.py $tree/audits/catalog/discovery-plan.json" \
    > "$out/discovery.json"
python3 -I -c 'import json,sys; json.dump(json.load(open(sys.argv[1]))["catalog"], open(sys.argv[2], "w"))' \
    "$out/discovery.json" "$out/capability-catalog.json"
mvn org.apache.maven.plugins:maven-dependency-plugin:3.7.1:build-classpath -pl :morphia-core \
    -DincludeScope=compile -Dmdep.outputFile="$out/core-compile.classpath"
audit dev.morphia.audits.api.ApiInventoryMainKt \
    ". $(git rev-parse HEAD) --classpath-file $out/core-compile.classpath --classpath-entry core/target/classes core/src/main/java annotations/src/main/java" \
    > "$out/api.json"
partial audit dev.morphia.audits.coverage.CoverageMainKt \
    "$out/capability-catalog.json $tree/audits/catalog/coverage-plan.json $out/api.json" > "$out/coverage.json"
rm -rf "$out/bundle"

python3 -I - "$out" <<'EOF'
import collections, json, os, sys
out = sys.argv[1]
def load(name):
    path = os.path.join(out, name)
    return json.load(open(path)) if os.path.exists(path) else None
inventory = load("documented-examples.json")
gaps = (load("candidate/gaps.json") or {}).get("gaps", [])
attestation = load("candidate/attestation.json")
candidate = load("candidate/candidate.json")
lines = [f"# Operator example discovery at mongodb/docs `{inventory['commit'][:10]}`", ""]
lines += ["| Measure | Count |", "| --- | --- |"]
lines += [f"| {key} | {value} |" for key, value in sorted(inventory["summary"].items())]
changed = os.path.getsize(os.path.join(out, "inventory.diff")) > 0
lines += ["", "The inventory " + ("**differs** from the committed one; see `inventory.diff`."
                                   if changed else "matches the committed one.")]
skipped = collections.Counter(page["detail"].split(":")[0] for page in inventory["skipped"])
lines += ["", "## Skipped pages", ""] + [f"- {count}: {reason}" for reason, count in skipped.most_common()]
lines += ["", "## Gaps", ""]
lines += [f"- **{kind}**: {count}" for kind, count in collections.Counter(g["kind"] for g in gaps).most_common()]
kinds = {(g["operator"], g["title"]): g["kind"] for g in gaps}
families = json.load(open("audits/catalog/generation-review.json"))["families"]
def state(page, example):
    if page["context"] not in families:
        return "family not generated"
    return kinds.get((page["operator"], example["title"]), "generated")
unowned = [f"{page['context']} {page['operator']}: {example['title'].strip()} ({state(page, example)})"
           for page in inventory["pages"]
           for example in page["examples"] if example["status"] == "UNOWNED"]
lines += ["", f"## Examples without a handwritten owner ({len(unowned)})", "",
          "Generated cases are not owners: generation derives them from these examples on every run.", ""]
lines += [f"- {each}" for each in unowned]
written = [f["path"] for f in candidate["files"] if f["action"] == "WRITE"]
generated = [path for path in written if path.startswith("core/src/test/java/")]
lines += ["", f"The candidate writes {len(written)} files, {len(generated)} of them generated test classes."]
if attestation:
    lines += ["", "Integration: " + ", ".join(f"{mapper} {'passed' if code == 0 else 'FAILED'}"
                                            for mapper, code in attestation["results"].items())]
coverage = load("coverage.json")
if coverage:
    states = collections.Counter(c["api"] for c in coverage["capabilities"])
    lines += ["", f"## Capability coverage ({coverage['state']})", "",
              "| Context | Observed | Accepted minimum | Healthy |", "| --- | --- | --- | --- |"]
    lines += [f"| {c['context']} | {c['observed']} | {c['expectedMinimum']} | {c['healthy']} |"
              for c in coverage["contexts"]]
    lines += ["", "API states: " + ", ".join(f"{k} {v}" for k, v in sorted(states.items()))]
open(os.path.join(out, "summary.md"), "w").write("\n".join(lines) + "\n")
EOF
cat "$out/summary.md"
exit $status
