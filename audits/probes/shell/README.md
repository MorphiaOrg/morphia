# Explicit parser compatibility probe

This optional read-only probe compares Rhino 1.9.1 and Acorn 8.19.0 on an already validated owned
document report and every JSON file in the frozen legacy corpus. It performs no downloads,
document checkout, code evaluation, source writes or baseline promotion. Temporary compiled Java
files are deleted after the run. It records exact source/report/corpus/parser identities and
all language groups, including unknown/malformed labels. Syntax acceptance is not normalization.

Acquire candidate artifacts explicitly before running: the Maven `org.mozilla:rhino:1.9.1` JAR
and Acorn 8.19.0's `dist/acorn.js` from an install with scripts disabled. Their hashes are pinned
in `compare.py`. Node is required only for this comparative probe, not the production normalizer.
Python 3.11+ and Java 17+ are required. Pass absolute executable/artifact paths:

```bash
python3 audits/probes/shell/compare.py \
  --documents /path/to/validated-documents.json \
  --source-sha256 9632a61a4f77b83161d81b7da8fc63e11ab94112d7b73c3bcf49da6ced5e165a \
  --rhino-jar /path/to/rhino-1.9.1.jar \
  --acorn-module /path/to/acorn/dist/acorn.js \
  --java-home /path/to/java17 \
  --node /path/to/node > /tmp/shell-parser-comparison.json
```

The coordinator must validate the document report's source origins before this probe. Its
source SHA check is an identity guard, not bundle authentication. A changed legacy fixture fails
hash verification; it is never silently substituted into the frozen input. See the
[design decision](../../../design/operator-audits-shell-literals.md) for results and limitations.

The checked-in [receipt](receipt.json) records the verified experiment with helper hashes and
runtime versions. It is a syntax benchmark, not an accepted operator/example coverage baseline.
