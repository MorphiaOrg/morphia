#!/usr/bin/env bash
# Usage: audits/bin/extract.sh <docs-checkout> <out-dir>
#
# Runs extraction in the repository this script belongs to, against the docs checkout's HEAD: it
# bundles and parses the checkout's operator pages with the pinned parser, then inventories them
# into audits/catalog/documented-examples.json, writes a scenario for every new example and
# refreshes each scenario whose example changed upstream. The working tree changes in place; review
# it with git diff. Intermediate reports land in <out-dir>: committed-inventory.json (the
# inventory before the run), sources.json, documents.json and extraction.txt.
#
# AUDITS_LABEL names the source bundle (default: local). AUDITS_REPOSITORY is the repository to
# extract in (default: the one this script belongs to, wherever it's run from). Requires a JDK 17
# toolchain, and uv, which provides the parser's pinned Python.
set -euo pipefail

[ $# -eq 2 ] || { sed -n 2,13p "$0" >&2; exit 2; }
docs=$(cd "$1" && pwd)
mkdir -p "$2"
out=$(cd "$2" && pwd)
label=${AUDITS_LABEL:-local}
tree=$(cd "${AUDITS_REPOSITORY:-$(dirname "$0")/../..}" && pwd)
cd "$tree"

mvn() { ./mvnw -B -ntp -q "$@"; }
audit() { mvn -Daudits -pl :morphia-audits exec:java -Daudits.mainClass="$1" -Dexec.args="$2"; }
# Exit status 3 means the step finished and reported what it couldn't do.
partial() { "$@" || [ $? -eq 3 ]; }
export MAVEN_OPTS=${MAVEN_OPTS:--Xmx8g}

mvn -Daudits -pl :morphia-audits compile
uv sync --frozen --project audits/parser -q
cp audits/catalog/documented-examples.json "$out/committed-inventory.json"

# The pinned source bundle of the checkout's manual, at the checkout's commit.
python3 -I - "$docs" "$out/sources-request.json" "$label" <<'EOF'
import json, subprocess, sys
request = json.load(open("audits/config/manual-baseline-sources.json"))
request["revision"] = subprocess.run(["git", "-C", sys.argv[1], "rev-parse", "HEAD"], check=True,
                                     capture_output=True, text=True).stdout.strip()
request["id"] = "manual-" + sys.argv[3]
request["label"] = sys.argv[3]
json.dump(request, open(sys.argv[2], "w"), indent=2)
EOF
rm -rf "$out/bundle"
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
    "$tree $out/bundle $content $out/documents.json --refresh $out/committed-inventory.json" \
    > "$out/extraction.txt"
rm -rf "$out/bundle"
grep -E '^(Wrote|Refreshed|NOT EXTRACTED|[0-9]+ documented)' "$out/extraction.txt" || true
