# Baseline at `bd70cba8d20`

The starting point that game plan items 2.x–4.x (`design/mapping-game-plan.md`) quote their deltas against. The
benchmark code is from plan item 0.2; the Morphia code under test is `master` at `bd70cba8d20`.

- **Machine:** Intel i9-11900H (16 threads), 32 GB, Linux, laptop on AC power. Numbers from CI runners will differ;
  compare ratios, or re-run the baseline on the same machine before quoting a delta.
- **JVM:** Temurin 17.0.17, no VM options. JMH 1.37.
- **Settings:** each benchmark's own annotations (no command-line overrides), `-prof gc`. All three variants ran one
  after another on the same machine.
- **Files:** `jmh-<variant>.json` is JMH's JSON output with the per-iteration raw data removed. `report.md` is
  `jbang .github/BenchmarkReport.java benchmarks/results/baseline-bd70cba8d20`. The report header's remark about
  separate runners applies to CI, not to this run.

Commands, from the repository root:

```bash
./mvnw install -Dbenchmarks -pl :morphia-benchmarks -am -DskipTests -Ddeploy.skip=true -Dinvoker.skip=true
cd benchmarks/target
java -cp morphia-benchmarks.jar org.openjdk.jmh.Main -p variant=reflection -prof gc -rf json -rff jmh-reflection.json
java -cp morphia-benchmarks-critter-aot.jar:morphia-benchmarks.jar org.openjdk.jmh.Main -p variant=critter \
    -prof gc -rf json -rff jmh-critter.json
java -cp morphia-benchmarks.jar org.openjdk.jmh.Main -p variant=critter-runtime -prof gc -rf json \
    -rff jmh-critter-runtime.json
```

## What it shows

Reflection numbers unless stated.

- **Every top-level decode now creates a `DecodeSession` (#4257).** `decodeInCursor` decodes inside one shared session,
  as `MorphiaCursor` does, and saves ~336 B/op on every model. A separate allocation probe on SIMPLE attributed the
  rest of #4257's cost: `peekId` plus session registration ~488 B/doc, the `_t` pre-scan ~392 B, the per-decode
  session ~352 B. Without any of these, SIMPLE decodes in 1,936 B; today it takes 3,168 B.
- **A no-op global listener is expensive (B3).** `ListenerBenchmark` against `CodecBenchmark` for the same models:
  SIMPLE decode 3.2 µs vs 1.35 µs (8.7 KB vs 3.2 KB), NESTED decode 14.6 µs vs 3.5 µs (42 KB vs 8 KB), NESTED encode
  11.7 µs vs 2.4 µs (39 KB vs 2 KB).
- **Schema drift is 3.4× slower:** two numeric fields stored as strings take SIMPLE decode from 1.3 µs to 4.5 µs
  (B1's mark/throw/rewind fallback). A document without `_t` costs ~11% more.
- **Embedding vectors:** a `double[1536]` plus a `String[20]` takes ~72 µs to decode and ~99 µs to encode, with
  77 KB and 137 KB allocated (B7). Under both critter variants this entity is mapped reflectively, because critter
  can't generate models for array properties (game plan 4.8).
- **Mixing entity types in one JVM** (`MixedCodecBenchmark`, all models but ARRAYS) costs 5–11% over the sum of the
  separate per-model runs for reflection and 6–17% for critter (C1's megamorphic call sites).
- **Session copy (D1):** `new MorphiaDatastore(ds)` costs 19 µs and 65 KB for 14 types (critter AOT 29 µs and 86 KB).
  Decoding one entity in the copy adds ~7 µs while its codec is rebuilt.
- **Queries:** translating `profile.address.city` costs ~1.1 µs and **5.2 KB**. Rendering a three-filter query costs
  3.4 µs and 14.8 KB (D2).
- **Package scan (A1):** mapping the models package by scanning takes 222 ms cold. Scanning `.*`, the default, takes
  ~2.0 s and allocates 760 MB on the shaded benchmarks jar, and it ran library static initializers (a JNDI mock
  printed `MockInitialContextFactory static called`).
- **Critter tier caching (C6):** while this baseline was being built, `warmMapping` with `ArraysEntity` in the mapped
  set logged ~298,000 identical WARN-with-stack-trace lines per critter variant. Every new mapper retries the failed
  runtime generation. `MappingBenchmark` now maps the original 11 types, which also keeps its recorded history
  comparable.
