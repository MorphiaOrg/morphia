# No-EntityModel upper-bound prototype

Hand-written code of the shape a per-entity codec generator would emit (option O1 in
`../findings-E-no-entitymodel.md`), plus an interpretive "flat immutable runtime model" codec
(an O2/O3 simulation) to separate "generate code" from "just flatten the model".

Everything is compiled against the shaded benchmarks jar (morphia-core, the driver, JMH and the benchmark
models), so it measures the exact same Morphia build as `CodecBenchmark`. These sources are not part of the
Maven build.

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

## Paths used below

```bash
# from the repository root, after:
#   ./mvnw install -Dbenchmarks -pl :morphia-benchmarks -am -DskipTests -Ddeploy.skip=true -Dinvoker.skip=true
P=design/mapping-review/prototype
JAR=benchmarks/target/morphia-benchmarks.jar
AOT=benchmarks/target/morphia-benchmarks-critter-aot.jar
OUT=$(mktemp -d)          # compiled classes and results
M=$HOME/.m2/repository
```

## Build

```bash
PP=$M/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar:$M/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar:$M/net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar:$M/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar
mkdir -p $OUT/classes
javac -processor org.openjdk.jmh.generators.BenchmarkProcessor -processorpath $PP -d $OUT/classes -cp $JAR $P/src/dev/morphia/benchmarks/handwritten/*.java
```

## Correctness check (fast, no timing)

```bash
java -cp $OUT/classes:$JAR dev.morphia.benchmarks.handwritten.Verify                 # reflection + critter-runtime
java -cp $OUT/classes:$AOT:$JAR dev.morphia.benchmarks.handwritten.Verify critter    # critter AOT models
```

Result on `4d48014`: `168 checks, 0 failures` and `84 checks, 0 failures`. The review reported 152 and 76. Checks
added afterwards cover the flat codec's id generation and schema drift (a string where a boolean or int is
expected).

After the review, both prototype codecs gained the decode-as-Object + `Conversions` fallback for non-native BSON
types, which Morphia already has. The flat codec chooses per slot with a bitmask of BSON types, as in B1. A
1-fork re-run on `4d48014` (different machine, so compare ratios, not absolute times) shows that this did not
change its standing:

| decode | SIMPLE | NESTED |
|---|---|---|
| morphia | 1226 ± 239 ns (3168 B) | 3142 ± 51 ns (7928 B) |
| flat | 507 ± 11 ns (1104 B) | 1512 ± 32 ns (3200 B) |

Morphia's allocation is higher than in the review (2240 / 6920 B); see game plan item 0.2.

## Run the benchmark

The hand-written codecs cover `SIMPLE` and `NESTED` only, so restrict
`model` when selecting `CodecBenchmark` in the same run. JMH merges the two `BenchmarkList`s, so both
benchmarks are visible on this classpath (`-l` lists them).

```bash
# reflection: Morphia (both benchmarks), flat-model simulation, hand-written; one JVM invocation, same settings
java -cp $OUT/classes:$JAR org.openjdk.jmh.Main 'CodecBenchmark|HandwrittenCodecBenchmark' \
  -p model=SIMPLE,NESTED -p variant=reflection \
  -f 2 -wi 4 -i 5 -w 1 -r 1 -prof gc -rf json -rff $OUT/handwritten-codec-reflection.json \
  > $OUT/handwritten-codec-reflection.log 2>&1

# critter AOT as the Morphia baseline (the AOT jar must come before the benchmarks jar)
java -cp $OUT/classes:$AOT:$JAR org.openjdk.jmh.Main 'CodecBenchmark|HandwrittenCodecBenchmark' \
  -p model=SIMPLE,NESTED -p variant=critter \
  -f 2 -wi 4 -i 5 -w 1 -r 1 -prof gc -rf json -rff $OUT/handwritten-codec-critter.json \
  > $OUT/handwritten-codec-critter.log 2>&1
```

Only the hand-written/flat codecs (fewer parameter combinations):

```bash
java -cp $OUT/classes:$JAR org.openjdk.jmh.Main HandwrittenCodecBenchmark -prof gc
```

What to read off the results:

- `morphia` vs `handwritten`: the most a generated codec can gain, per document (time and `gc.alloc.rate.norm`).
- `morphia` vs `flat`: what flattening the model gets without code generation (O2/O3).
- `flat` vs `handwritten`: what code generation adds on top (monomorphic call sites, no boxing, constant field access).

Caveats: the hand-written decoders keep Morphia's semantics for the cases `Verify` covers. They do not implement
`@AlsoLoad` (that would be extra `case` labels), lifecycle callbacks, or references. `FlatModelCodec` reads
`@Id`/`@Property` directly and is only meant for these two models.
