# Mapper Profiling Workflow — Initial Plan

Goal: a dedicated GitHub Actions workflow, separate from `build.yml`, that profiles the
reflection mapper and the critter mapper in parallel jobs and keeps the results over time, so
we can see how each one changes as we tune it.

## What exists today

- `build.yml` already runs `mapper: [reflection, critter]` in its Server/Driver/JDK/Coverage
  matrices, but only to check that tests pass. Timings are mixed in with ~30 other jobs, and
  test time is mostly MongoDB I/O, so the numbers mean little.
- The mapper is picked with `-Dmorphia.mapper=reflection|critter`. Surefire passes it to
  `TestBase`, which builds `ReflectiveMapper` or `CritterMapper`.
- `TestBase` caches one mapped "template" `Mapper` per config. That's good for test speed, but
  it hides mapping and startup cost from any timing based on the test suite.
- Critter has two ways to get models: AOT, where `critter-maven` generates them at build time,
  and runtime generation through Gizmo as the fallback. These perform differently, so they
  should be measured separately.
- There is no JMH or other benchmark setup anywhere in the repo.

## Proposed design

### 1. New module: `benchmarks/` (JMH, Maven, never deployed)

- A Maven subproject with `jmh-core` and the annotation processor. It builds a shaded
  `benchmarks.jar`, with `maven.deploy.skip`, `jreleaser`, and dokka all turned off.
- Its own small set of entity models: a flat POJO, a nested/embedded graph, collections and
  maps, polymorphic/discriminated types, `@Reference`, and a lifecycle-callback entity. The
  `critter-maven` plugin runs over them so AOT models are in the jar.
- A `@Param mapper` with values `REFLECTION`, `CRITTER` and possibly `CRITTER_RUNTIME` (see the
  open questions). Each CI job runs just one value via `-p mapper=…`.
- Benchmark groups:

  | Group | Mode | What it measures |
  |---|---|---|
  | `MappingStartup` | single-shot, many forks | cold `Mapper` creation plus mapping N entities, including class generation for critter |
  | `Encode` | throughput / avgt | entity → `BsonDocument` through the codec registry, with no database |
  | `Decode` | throughput / avgt | `BsonDocument` → entity, with no database |
  | `PropertyAccess` | avgt | get/set through `PropertyModel` (critter's generated accessors vs reflection) |
  | `RoundTrip` *(optional)* | avgt | save and find against a Testcontainers MongoDB, for an end-to-end sanity check |

- The Encode, Decode and PropertyAccess benchmarks skip the database, so they measure mapper
  code rather than network and server noise.
- Profilers: `-prof gc` always (allocation rate per op), and `-prof async` (async-profiler
  flame graphs) as an option.

### 2. New workflow: `.github/workflows/mapper-profiling.yml`

```
Build ──► Profile (matrix: mapper = reflection, critter[, critter-runtime])  ──► Compare
          fail-fast: false, runs in parallel
```

- **Triggers:** `push` to `master`, `workflow_dispatch` (inputs: JMH include regex,
  forks/iterations, whether to enable async-profiler), and maybe a nightly `schedule`. PRs only
  through a label or dispatch, so normal PR CI doesn't slow down.
- **Concurrency:** group `mapper-profiling-${{ github.ref }}`, so it never cancels or blocks
  `build.yml`.
- **Build job:** `./mvnw install -DskipTests -Ddeploy.skip=true -Dinvoker.skip=true -pl
  :morphia-benchmarks -am`, then uploads `benchmarks.jar` as an artifact. Both mapper jobs run
  the exact same bytecode.
- **Profile job** (one per mapper):
  - Downloads the jar and runs
    `java -jar benchmarks.jar -p mapper=$MAPPER -rf json -rff jmh-$MAPPER.json -prof gc`.
  - Separately runs `MappingStartup` in single-shot mode with JFR enabled
    (`-jvmArgsAppend -XX:StartFlightRecording=…`).
  - Uploads the JMH JSON, the JFR files and any flame graphs as artifacts.
  - Writes a Markdown table to `$GITHUB_STEP_SUMMARY`.
- **Compare job:** `needs: Profile`. Downloads both JSON files and uses a small JBang script
  (in the style of `.github/BuildMatrix.java`) to render a side-by-side table with a
  critter/reflection ratio and error bars in the run summary.
- **History:** `benchmark-action/github-action-benchmark` keeps a time series per mapper on a
  `gh-pages` (or `benchmark-data`) branch. That gives trend charts and optional
  "regressed > X%" alerts or commit comments. It needs `contents: write` on that job only.

### 3. Supplementary signal from the existing test suite (cheap, phase 1b)

- An optional job that runs `./mvnw verify -pl :morphia-core -Dmorphia.mapper=$MAPPER` with JFR
  attached through surefire `argLine`. It collects per-class times from the surefire XML into
  the summary.
- This is coarse and noisy because the database dominates, but it costs almost no new code and
  can catch large regressions early.

## Noise and how to handle it

- GitHub-hosted runners are shared VMs, so expect 5–10% run-to-run variance.
- **Parallel jobs land on different machines.** Comparing reflection to critter within one run
  therefore includes machine-to-machine variance. Mitigations:
  1. Track each mapper against **its own history**. This is the main purpose of the trend
     charts.
  2. Show the cross-mapper ratio with error bars and treat it as approximate.
  3. Optionally add a "same-runner" job that runs both mappers back to back on one VM, for a
     cleaner head-to-head.
- Pin the JDK (Temurin 17, plus 21 later if wanted). Use several forks with fixed
  warmup/measurement iterations. Keep the default run under about 20 minutes per job.
- Later, if the trends are still too noisy, use a self-hosted or larger runner.

## Phased rollout

1. **Phase 1:** `benchmarks` module with Encode/Decode/MappingStartup for reflection and
   critter (AOT); the workflow with Build → Profile matrix → Compare; artifacts and step
   summary.
2. **Phase 2:** history and charts on `gh-pages` with regression alerts; add a
   `critter-runtime` leg.
3. **Phase 3:** async-profiler flame graphs, the RoundTrip benchmark against MongoDB, the
   same-runner head-to-head job, and an optional PR-label trigger.

## Decisions (settled)

1. **Triggers:** manual only (`workflow_dispatch`) to start.
2. **Critter legs:** three parallel legs: `reflection`, `critter` (AOT models from
   `critter-maven`), and `critter-runtime` (Gizmo runtime generation). The runtime path may not
   stay long term, but we want data on it first.
3. **History storage:** a dedicated `benchmark-data` branch, set by `HISTORY_BRANCH` in the
   workflow. We first agreed on `gh-pages`, but `gh-pages` in this repo *is* the morphia.dev
   documentation site (`CNAME` = `morphia.dev`, updated by the docs build). Data written there
   would be public under morphia.dev and could be wiped by the next docs push. The new branch
   is not served anywhere. `github-action-benchmark` writes the same `data.js` and `index.html`
   to it, and the branch can be moved or published later.
4. **Database:** in-memory only. Phase 1 measures entity → BSON → entity and nothing that
   touches MongoDB. `RoundTrip` is dropped.
5. **Models:** a dedicated model set in the `benchmarks` module.

## Phase 1 implementation notes

- The module is `benchmarks/` (`morphia-benchmarks`). It is only in the reactor with
  `-Dbenchmarks`, so the regular build never compiles it.
- Two jars come out of the build:
  - `morphia-benchmarks.jar`: the shaded JMH uber-jar, built from the plain (unwoven) classes.
    The `reflection` and `critter-runtime` legs use it on its own.
  - `morphia-benchmarks-critter-aot.jar`: the `critter-maven` output, i.e. the generated models
    plus the woven entity classes. The `critter` leg puts it *in front of* the uber-jar on the
    classpath, so the AOT classes shadow the plain ones.
- Each benchmark state checks at setup that its variant really got the models it expects: AOT
  classes for `critter`, `CritterClassLoader` classes for `critter-runtime`, and no fallback to
  reflection in either critter variant. A misconfigured leg fails loudly instead of quietly
  measuring the wrong thing.

## Findings from the first local runs

- **critter-maven skips AOT generation** in two cases, and falls back to runtime generation:
  - embedded types without an `@Id` ("entity requires runtime property discovery"), which in
    practice means every embedded type;
  - types that inherit private fields (e.g. `Circle extends Shape` with a private
    `Shape.color`).

  So the `critter` leg is really a hybrid: top-level entities are AOT and everything they embed
  is generated at runtime. The models stay realistic on purpose. The tiers are printed per run
  ("Model tiers" in the job summary).
- **Critter decode looks much slower than reflection** (local, short warmed run, JDK 21):

  | decode | reflection | critter (AOT) |
  |---|---:|---:|
  | SIMPLE | ~2.2 µs, 3.1 KB/op | ~25 µs, 46 KB/op |
  | NESTED | ~11.7 µs, 29 KB/op | ~62 µs, 137 KB/op |

  The allocation numbers are very stable, which points to something allocated on every decode in
  the critter path. Encode is roughly even between the two. This needs confirming on CI, then
  investigating, probably with the JFR recordings the workflow uploads.
