#!/usr/bin/env bash
# Usage: audits/discover.sh <docs-checkout> <out-dir> [--integrate]
#
# Report-only discovery of documented operator examples. In a throwaway worktree of HEAD it
# inventories the checkout's operator pages, diffs that inventory against the committed one,
# generates a candidate with its gaps and, with --integrate, runs the candidate's tests under both
# mappers. Everything lands in <out-dir>, with summary.md on top; nothing in this checkout changes
# and nothing is published. Requires a built JDK 17 toolchain and, for --integrate, Docker.
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
open(os.path.join(out, "summary.md"), "w").write("\n".join(lines) + "\n")
EOF
cat "$out/summary.md"
exit $status
