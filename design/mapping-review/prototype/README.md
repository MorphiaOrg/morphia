# No-EntityModel upper-bound prototype

Hand-written code of the shape a per-entity codec generator would emit (option O1 in
`../findings-E-no-entitymodel.md`), plus an interpretive "flat immutable runtime model" codec
(an O2/O3 simulation) to separate "generate code" from "just flatten the model".

Everything is compiled against the shaded benchmarks jar in `../baseline/` (morphia-core, the driver, JMH and
the benchmark models), so it measures the exact same Morphia build as the baseline run.

## Files

| File | What |
|---|---|
| `src/.../handwritten/SimpleEntityCodec.java` | Ideal generated codec for `SimpleEntity`: straight-line writes, string `switch` on decode, no boxing, `static final VarHandle`s (JIT-constant, equivalent to direct `getfield`/`putfield`) |
| `src/.../handwritten/NestedEntityCodec.java`, `AddressCodec.java`, `ProfileCodec.java` | Same for `NestedEntity`; embedded codecs are called directly (no registry, no child-context indirection, no discriminator pre-scan) |
| `src/.../handwritten/CodecSupport.java` | The small shared runtime library a generator would target: lenient numeric reads, the cold "decode as Object + `Conversions.convert`" fallback, subtype hand-off |
| `src/.../handwritten/FlatModelCodec.java` | O2/O3 simulation: no `EntityModel`, flat arrays + `MethodHandle` accessors + precomputed codecs + a name→slot map, ONE shared interpretive loop |
| `src/.../handwritten/Codecs.java` | Factory used by the checks and the benchmark |
| `src/.../handwritten/Verify.java` | `main()`: byte-for-byte equality with Morphia's encoder + decode interchangeability (both directions), for storeNulls on/off, sparse/null entities, "foreign" documents (shuffled order, unknown fields, int64→int, int32→double/long, explicit nulls, missing `_t`, single value→list), and `_id` generation |
| `src/.../handwritten/HandwrittenCodecBenchmark.java` | JMH class mirroring `CodecBenchmark` (same annotations, buffers, contexts, samples); `@Param impl = morphia, flat, handwritten`; setup fails unless the encoding is byte-identical to Morphia's |
| `classes/` | Compiled output, including the JMH-generated classes and `META-INF/BenchmarkList` |

## Paths used below

```bash
S=/tmp/claude-0/-home-user-morphia/f1d311ce-d37b-5cef-ab0a-28d1953de78c/scratchpad
P=$S/no-entitymodel
JAR=$S/baseline/morphia-benchmarks.jar
AOT=$S/baseline/morphia-benchmarks-critter-aot.jar
M=$HOME/.m2/repository
```

## Rebuild (already done; only needed after editing sources)

```bash
PP=$M/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar:$M/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar:$M/net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar:$M/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar
rm -rf $P/classes && mkdir $P/classes
javac -proc:full -processorpath $PP -d $P/classes -cp $JAR $P/src/dev/morphia/benchmarks/handwritten/*.java
```

## Correctness check (fast, no timing)

```bash
java -cp $P/classes:$JAR dev.morphia.benchmarks.handwritten.Verify                 # reflection + critter-runtime
java -cp $P/classes:$AOT:$JAR dev.morphia.benchmarks.handwritten.Verify critter    # critter AOT models
```

Last result: `152 checks, 0 failures` and `76 checks, 0 failures`.

## Run the benchmark (not run by me)

Same settings as `../run-bench.sh`. The hand-written codecs cover `SIMPLE` and `NESTED` only, so restrict
`model` when selecting `CodecBenchmark` in the same run. JMH merges the two `BenchmarkList`s, so both
benchmarks are visible on this classpath (`-l` lists them).

```bash
# reflection: Morphia (both benchmarks), flat-model simulation, hand-written; one JVM invocation, same settings
java -cp $P/classes:$JAR org.openjdk.jmh.Main 'CodecBenchmark|HandwrittenCodecBenchmark' \
  -p model=SIMPLE,NESTED -p variant=reflection \
  -f 2 -wi 4 -i 5 -w 1 -r 1 -prof gc -rf json -rff $S/results/handwritten-codec-reflection.json \
  > $S/results/handwritten-codec-reflection.log 2>&1

# critter AOT as the Morphia baseline (the AOT jar must come before the benchmarks jar)
java -cp $P/classes:$AOT:$JAR org.openjdk.jmh.Main 'CodecBenchmark|HandwrittenCodecBenchmark' \
  -p model=SIMPLE,NESTED -p variant=critter \
  -f 2 -wi 4 -i 5 -w 1 -r 1 -prof gc -rf json -rff $S/results/handwritten-codec-critter.json \
  > $S/results/handwritten-codec-critter.log 2>&1
```

Only the hand-written/flat codecs (fewer parameter combinations):

```bash
java -cp $P/classes:$JAR org.openjdk.jmh.Main HandwrittenCodecBenchmark -prof gc
```

What to read off the results:

- `morphia` vs `handwritten`: the most a generated codec can gain, per document (time and `gc.alloc.rate.norm`).
- `morphia` vs `flat`: what flattening the model gets without code generation (O2/O3).
- `flat` vs `handwritten`: what code generation adds on top (monomorphic call sites, no boxing, constant field access).

Caveats: the hand-written decoders keep Morphia's semantics for the cases `Verify` covers. They do not implement
`@AlsoLoad` (that would be extra `case` labels), lifecycle callbacks, or references. `FlatModelCodec` reads
`@Id`/`@Property` directly and is only meant for these two models.
