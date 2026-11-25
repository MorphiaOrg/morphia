# Published-expectation replay

This reuses the [native extraction plan](../scenarios/plan.json), source bundle and coherent owned
350-page document report from the preceding source-grouping experiment. It performs no downloads,
documentation-code execution, MongoDB calls or registry promotion. Synthetic `fixture.*` destinations
use an empty inventory solely to compare proposed descriptive names, not to migrate core tests/data.

Build audits, then supply the compiled classes/dependencies, pinned bundle, document report and a
disposable output directory. The Python 3.11+ coordinator runs project entry points twice, checks
byte identity and emits a provenance receipt. Optional `--prior-names` verifies exact byte identity
with the previous probe's naming proposal. The receipt records every audits source digest, ordered
classpath file/directory-tree identities, Java/Python versions, Git HEAD and dirty status. Source
digests and compiled-runtime digests are separate evidence; the probe does not claim a source digest
alone authenticates the compiled code. Build before running and keep the worktree/runtime frozen.

```sh
python3 audits/probes/published-expectations/replay.py \
  --java "$JAVA_HOME/bin/java" --classpath "$AUDITS_CLASSPATH" \
  --bundle /path/to/bundle \
  --source-sha256 9632a61a4f77b83161d81b7da8fc63e11ab94112d7b73c3bcf49da6ced5e165a \
  --documents /path/to/documents.json --output /tmp/published-probe \
  --prior-names /path/to/prior-names.json
```

All sixteen reference programs now normalize with explicit published constraints; two foreign
variants remain preserved. The previous experiment normalized two programs and rejected fourteen
solely because printed whole numbers did not establish one BSON type. No source constructors or
expected-data files were invented. Descriptive naming proposals remain identical. The checked-in
receipt records this pinned experiment; these counts are not a baseline for inferring absence from
future sources, nor proof of setup/data closure, execution or reference-versus-DSL parity.

Current coordinators emit receipt schema 2: all audits source namespaces, build inputs and the shared
provenance helper are explicit. Runtime/source/file inputs are checked before and after execution.
Older schema-1 receipts retain their historical checkpoint meaning; reruns on newer code should save
a new receipt rather than replace an accepted experiment. Native probes require Python 3.11+.
