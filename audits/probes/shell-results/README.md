# Pinned printed-result replay

The `$abs` section in the pinned manual has three ordinary code blocks: insert, aggregate and four
newline-separated result documents. `selection.json` declares reviewed roles by exact code digest;
the request fixture preserves those code strings unchanged. These are source-selection declarations,
not an automatic section-assembly policy. Native extraction still reports three unassigned blocks.

After compiling audits, provide the same verified bundle and coherent 350-page owned report used
by the preceding probes. The coordinator first invokes native extraction to verify those artifacts,
then requires a unique digest/provenance/text match for every declared block, runs the request twice
and records source/runtime identities. It also retains ordinary SHELL's syntax rejection for the same
output. No documentation commands, MongoDB operations, downloads, promotions or issue writes occur.

```sh
python3 audits/probes/shell-results/replay.py \
  --java "$JAVA_HOME/bin/java" --classpath "$AUDITS_CLASSPATH" \
  --bundle /path/to/bundle --documents /path/to/documents.json \
  --output /tmp/shell-results-probe
```

The source bundle content pin is `9632a61a4f77b83161d81b7da8fc63e11ab94112d7b73c3bcf49da6ced5e165a`;
the owned parse-input pin is `08992fea5b191dde64035beeba3524d02e020f9621e28f7f349b0e106f173675`.
Java/Python versions, ordered compiled classpath hashes, all audits source hashes and build inputs
are recorded separately. Keep them frozen during the replay; source digests alone do not identify
compiled code. The shared receipt helper includes the isolated lexer adapter outside `dev.morphia`.

`receipt.json` describes this normalization experiment. `native-io-receipt.json` separately repeats
the prior 16-reference/two-foreign IO experiment using the new explicit output grammar and checks
that all descriptive naming proposals remain byte-identical. Neither receipt is a behavior baseline,
dataset-closure attestation or approval to replace existing tests/data. Section/setup ownership,
the `main` label recovery and actual descriptive-name backport remain later work.

Current coordinators emit receipt schema 2: all audits source namespaces, build inputs and the shared
provenance helper are explicit. Runtime/source/file inputs are checked before and after execution.
Older schema-1 receipts retain their historical checkpoint meaning; reruns on newer code should save
a new receipt rather than replace an accepted experiment. Native probes require Python 3.11+.
