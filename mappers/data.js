window.BENCHMARK_DATA = {
  "lastUpdate": 1795459360387,
  "repoUrl": "https://github.com/MorphiaOrg/morphia",
  "entries": {
    "Mapper: reflection": [
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "cc32cd505db2989982b1bfe1004f58698c4fa5fd",
          "message": "Fix critter decode building a new Conversions per property (#4338)\n\n## Problem\nCritter-mapped entities decoded about 10× slower than reflective ones\nand allocated about 15× more per decode. The new mapper benchmarks\n(#4336) surfaced this.\n\nJFR showed almost all the time and allocation inside\n`Conversions.<init>`, called from `PropertyModel.setValue`:\n\n```java\nConversions c = conversions != null ? conversions\n        : new Conversions(Thread.currentThread().getContextClassLoader());\n```\n\nReflective property models get `.conversions(mapper.getConversions())`\nfrom `FieldDiscovery` / `MethodDiscovery`. Critter's generated property\nmodels never had it set. Every property set during decode therefore\nrebuilt the whole conversions registry: a few dozen map entries and\nlambdas.\n\n## Fix\n`CritterEntityModel.configureProperties(mapper)` now also sets\n`property.conversions(mapper.getConversions())`. That method already\nruns at the end of every generated entity-model constructor, AOT and\nruntime alike. Existing AOT output picks up the fix without\nregeneration.\n\n## Results\nIn-memory JMH benchmarks from #4336, JDK 21, short warmed runs:\n\n| decode | reflection | critter before | critter after |\n|---|---:|---:|---:|\n| SIMPLE | 2.6 µs, 3,080 B/op | 25 µs, 46,089 B/op | 2.4 µs, 3,144 B/op\n|\n| NESTED | 12.1 µs, 28,944 B/op | 62 µs, 136,562 B/op | 12.0 µs, 28,864\nB/op |\n\nCOLLECTIONS, POLYMORPHIC and LIFECYCLE also now match reflection for\nboth critter AOT and critter runtime. Encode was never affected.\n\n## Testing\n- Full `morphia-core` suite (Testcontainers MongoDB 8.0.0):\n- `-Dmorphia.mapper=critter`: 1,274 tests, 0 failures, 0 errors, 17\nskipped.\n- `-Dmorphia.mapper=reflection`: 1,274 tests, 0 failures, 0 errors, 17\nskipped.\n- `spotless:apply` makes no changes.\n\n🤖 Generated with [Claude Code](https://claude.com/claude-code)\n\nhttps://claude.ai/code/session_01MExceZHKD4HFFCDa6tXn8g\n\nCo-authored-by: Claude <noreply@anthropic.com>",
          "timestamp": "2026-10-04T01:02:07Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/cc32cd505db2989982b1bfe1004f58698c4fa5fd"
        },
        "date": 1791082000986,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 884.0998608277247,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 5316.671475155827,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 15727.321269263017,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 10762.224970590509,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 722.1365534780847,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 954.8966847670395,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 3054.9946841635415,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 9411.647659901346,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 6446.113584374877,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 852.8012661943634,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"reflection\"} )",
            "value": 58.29638400101929,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"reflection\"} )",
            "value": 146.9839048,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "6853f62f6b5e4968779a8476a3736cecb71402d1",
          "message": "Create the critter class loader lazily and drop its ByteBuddy base class (#4344)\n\n## Problem\n\nAfter #4342 and #4343, every benchmark model loads from AOT, but\ncritter's `coldStart` was still about 12% slower than reflection. The\ncause was `CritterMapper`'s constructor, which always creates a\n`CritterClassLoader`, even when every model is pre-generated and the\nloader is never used.\n\n`CritterClassLoader` extended ByteBuddy's\n`ByteArrayClassLoader.ChildFirst`, so creating one initialized a large\npart of ByteBuddy on every cold start. Diffing the classes loaded by a\ncold `mappedMapper` call against reflection showed 79 ByteBuddy classes,\nplus JDK dynamic proxies and classes like `Process`, `Console` and\n`ResourceBundle` that nothing else needs. Constructing a\n`CritterClassLoader` alone cost about 43–60 ms in a fresh JVM.\n\n## Fix\n\n- **Lazy loader:** `CritterMapper` creates its `CritterClassLoader` the\nfirst time a model has to be generated at runtime. Copies of a mapper\nshare one holder, so they still share one loader.\n- **No ByteBuddy base class:** `CritterClassLoader` is now a plain\n`ClassLoader`. It keeps the behavior that generated code relies on:\n- registered classes and `dev.morphia.critter.*` classes load\nchild-first; everything else goes to the parent\n- a class's bytes are released once it is defined, as with ByteBuddy's\ndefault `LATENT` persistence\n- `getResource`/`getResourceAsStream` return `null` for the `.class`\nfile of a registered or child-defined class; `getResources` still lists\nthe parent's copy, as before\n\nByteBuddy is still a dependency because `ReferenceCodec` uses it for\nlazy reference proxies.\n\n`CritterClassLoaderTest` pins this contract. I wrote it first and\nconfirmed it passes against the old ByteBuddy-based loader, then against\nthe new one.\n\nOne behavior change: the no-arg-loader constructor used to pass the\n`CritterClassLoader` to `AbstractMapper` as the mapper's class loader,\nwhich is used for `Conversions`, `DiscriminatorLookup` and package\nscanning. It now passes the context class loader directly. Most classes\nresolve the same way, because the old loader delegated them to the\nparent. `dev.morphia.critter.*` classes looked up by name are the\nexception: the old loader would have returned a second copy of those,\ndistinct from the mapped class.\n\n## Results\n\nJMH `MappingBenchmark.coldStart`, 20 forks:\n\n| | master | this PR |\n|---|---:|---:|\n| critter | 224.8 ± 8.1 ms | **197.5 ± 4.5 ms** |\n| critter-runtime | 241.4 ± 5.3 ms | 229.6 ± 19.1 ms |\n| reflection | 133.8 ± 4.1 ms | n/a |\n\nOn master alone most of the critter gap is the types that still fall\nback to runtime generation, which #4342 and #4343 fix. With all three\nchanges combined, critter's `coldStart` measured 125.4 ± 1.5 ms against\n128.6 ± 2.7 ms for reflection. A control run without the lazy loader\nmeasured 160.9 ± 16.8 ms. That combined measurement used the lazy loader\nwith the old ByteBuddy base class; once the loader is never created, the\nbase class makes no difference there.\n\n## Testing\n\n- New `CritterClassLoaderTest`: passes against both the old and new\nloader.\n- Full `morphia-core` suite with regenerated AOT test models and\n`-Dmorphia.mapper=critter`: 1279 run, 0 failures. The 33 AOT-skipped\ntest entities go through runtime generation, so this exercises the new\nloader.\n- Full `morphia-core` suite with `-Dmorphia.mapper=reflection`: 1279\nrun, 0 failures.\n- critter-maven unit tests (5) and invoker ITs (3) pass.",
          "timestamp": "2026-10-04T22:40:04Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/6853f62f6b5e4968779a8476a3736cecb71402d1"
        },
        "date": 1791154340862,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1502.2520990320213,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 8137.362519138843,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 22474.84117851645,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 16701.58986772008,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 1067.4266426017903,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1253.0481293494531,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 4179.061675083916,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 12516.706595303864,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 8221.729036301937,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 1310.5536485669552,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"reflection\"} )",
            "value": 82.37285138559352,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"reflection\"} )",
            "value": 231.3253822,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "0a5812f93e9bff88cdb6e7a51d3712fec43d349d",
          "message": "Test critter's pre-generated models in pull-request CI (#4370)\n\nAdds a **Critter (AOT)** job to the Pull Requests workflow, so pull\nrequests exercise critter's pre-generated models and not only runtime\ngeneration.\n\n### Why\n- **Today's PR job:** the shared workflow behind `Build` runs a plain\n`install -Dmorphia.mapper=critter`. Core's build doesn't run\ncritter-maven, so there are no pre-generated models and critter\ngenerates every model at runtime. That's how #4366's AOT record tests\nfailed in PR CI.\n- **Push builds:** branches pushed to this repo also run `build.yml`.\nThat workflow generates the models and tests against them, so same-repo\nPRs did get AOT coverage there.\n- **Forks:** pull requests from forks only run this workflow, so they\nnever tested the AOT path.\n\n### What the job does\nIt mirrors `build.yml`:\n1. `install -DskipTests`\n2. `generate-test-models` in `core`\n3. `surefire:test -Dmorphia.mapper=critter`, which doesn't recompile.\nRecompiling would replace the woven classes and leave stale models\n(`NoSuchMethodError: __readXxx`).\n\nThe existing `Build` job is unchanged. It keeps running critter with\nruntime generation for every model, which, together with `Coverage\nReport (critter)` in `build.yml`, is the only full-suite run of the\nruntime tier. The two jobs cover the two critter tiers.\n\n### Verified locally\n- The workflow parses.\n- I ran the job's three commands as written: `BUILD SUCCESS` across the\nreactor.\n- Core ran 1313 tests against the generated models (0 failures, 16\nskipped). The `Aot*` record fixtures got their models, and their\nAOT-tier checks passed.\n\nThis PR's own CI runs the new job, since `pull_request` uses the\nworkflow from the PR.\n\nThe new job isn't a required check. If it should block merges, add\n`Critter (AOT)` to master's required status checks.\n\nFixes #4369",
          "timestamp": "2026-10-06T04:17:28Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/0a5812f93e9bff88cdb6e7a51d3712fec43d349d"
        },
        "date": 1791260729131,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1221.7358540110286,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 3532.9530469818756,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 11928.474910322686,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 6563.963782833551,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 1023.6527167304172,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 900.5459493369652,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2909.4881795491874,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 9671.261438539419,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 5084.53870736479,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 963.1744298268067,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"reflection\"} )",
            "value": 77.08749902087719,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"reflection\"} )",
            "value": 212.98954829999997,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "30554c3d4561e2db62ccbe1b45bd138b69a48bee",
          "message": "Cache entity codecs in a ConcurrentHashMap (plan item 2.8) (#4397)\n\nPlan item **2.8** (review D8). This completes the Phase 2 fixes. 2.5\nships with 3.8, as the plan says.\n\n## The bug\n`MorphiaCodecProvider.codecs` was a plain `HashMap`, and `get` mutated\nit on every miss. With concurrent first use of an entity type, a\n`HashMap` can be corrupted. Even when it isn't, every thread that missed\nbuilt its own `MorphiaCodec` and stored it, overwriting the others, so\ncallers ended up holding different codec instances for the same type.\n\n## Change\n`codecs` is a `ConcurrentHashMap`. On a miss, `get` builds the codec and\nstores it with `putIfAbsent`. If another thread stored one first, it\nreturns that one, so every caller shares a single codec per type.\n\nIt doesn't use `computeIfAbsent`, as the plan says: building a codec\nlooks up other codecs, and those lookups can land back in this provider,\nwhich isn't allowed inside a `ConcurrentHashMap` compute.\n\n## Tests\n`CodecHarnessTest.concurrentCodecLookupsShareOneCodec` runs under both\nmappers. For 20 fresh providers, 8 threads released together call\n`provider.get(HarnessScalars.class, registry)`, and the test asserts\nthat all of them get the same codec. It failed before the fix:\n*\"REFLECTION: every lookup should get the one cached codec\"*.\n\nThe core and Kotlin suites pass with `-Dmorphia.mapper=critter` and with\nreflection: 1483 + 5 tests, 0 failures.\n\n## Decisions to review\n1. **Recursive self-lookups now keep the inner codec.** If building a\ntype's codec recursively looks up that same type, the inner lookup\nstores its codec first, and the outer one is discarded. Before, the\nouter one overwrote it. Both are fully built codecs for the same model.",
          "timestamp": "2026-10-15T12:51:23Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/30554c3d4561e2db62ccbe1b45bd138b69a48bee"
        },
        "date": 1792078857992,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1565.7032959828123,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 4055.963590978036,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 12355.551148231783,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 7608.29424452816,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 1307.2210366770857,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 82029.9588570652,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 3460.3216334451768,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 3858.5359144235795,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1595.4719446932866,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 4106.673349806412,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 12845.275039209724,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 7758.970629413456,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 1322.702512036824,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 84020.02547621584,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 3485.3743042632796,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 3756.5882424900824,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 891.4416119298011,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2738.3170388265435,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 9799.168012314058,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 4827.62063150282,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 952.1211671826889,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 123191.76172778939,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 2642.3868230184803,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 1963.22622258415,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"reflection\"} )",
            "value": 19.180649983625493,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"reflection\"} )",
            "value": 31.342343174032287,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"reflection\"} )",
            "value": 1570.682628269638,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"reflection\"} )",
            "value": 1621.716577558642,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"reflection\"} )",
            "value": 5954.922758158006,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 3605.1786200630168,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 17143.101595178006,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 3022.0792286452083,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 13178.210610028682,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"reflection\"} )",
            "value": 81.84382262914205,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"reflection\"} )",
            "value": 39570.75348462158,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"reflection\"} )",
            "value": 27071.281469943893,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"reflection\"} )",
            "value": 3829.042235061627,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"reflection\"} )",
            "value": 1164.2983817228617,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"reflection\"} )",
            "value": 227.8386251,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"reflection\"} )",
            "value": 3293.2668385999996,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"reflection\"} )",
            "value": 422.7550486,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "23772b843402736f12140e1b77298b2978746561",
          "message": "Read and write primitive arrays without boxing (plan item 3.16) (#4422)\n\nPlan item **3.16** (review B7).\n\n## The problem\n`TypedArrayCodec` handles `double[]`, `int[]`, `long[]`, `float[]`,\n`short[]`, `boolean[]` and `char[]`, plus the matching wrapper arrays.\nIt treated every element as an object:\n- **encode:** `Array.get` (boxes), then the boxed component codec;\n- **decode:** the boxed component codec, `ArrayList.add`, then\n`Array.set` for every element of a new array.\n\nFor a `double[1536]` embedding, that is 1,536 boxed `Double`s each way,\nplus the list.\n\n## Change\n- **`TypedArrayCodec.of(datastore, type)`** returns a specialized codec\nfor each primitive component type. `MorphiaTypesCodecProvider` uses it.\nWrapper component types keep the general codec.\n- **Encode** writes each element directly: `writeDouble`, `writeInt32`,\n`writeInt64`, `writeBoolean`, and `writeString` for `char`. This is\nexactly what the driver's `DoubleCodec`/`IntegerCodec`/… write,\nincluding `float` → `writeDouble` and `short` → `writeInt32`.\n- **Decode** reads an element stored as the component's native BSON type\nstraight into a primitive buffer: `DOUBLE` for `double`/`float`, `INT32`\nfor `int`/`short`, `INT64` for `long`.\n- **Any other BSON type** goes through the driver's codec for that\nelement, so numeric conversions (e.g. an `INT32` in a `double[]`, or a\nwhole-number `DOUBLE` in an `int[]`) and their errors are unchanged.\nThis is the boxed fallback the plan asks for.\n- The `float` and `short` fast paths repeat the range checks\n`FloatCodec`/`ShortCodec` make, with the same exception and message.\n  - `boolean[]` reads with `readBoolean`, as `BooleanCodec` does.\n- `char[]` still goes through `CharacterCodec`, which keeps its\nstring-length checks. `Character.valueOf` caches the ASCII range, so\nthis rarely allocates.\n- **The decode buffer starts at the length of the last array that codec\ndecoded,** capped at 65,536 elements. Arrays of one property tend to\nshare a length, such as an embedding's dimension, so the usual case\nallocates the result once, with no growth and no final copy.\n- **The fast paths apply only while the component codec is the driver's\nown class.** If an application registers its own codec for `double` (or\n`Double`) through `MorphiaConfig.codecProvider`, the array codec uses\nthe original boxed path through that codec.\n- **The general (wrapper) path** loops over `Object[]` instead of\ncalling `Array.get`, and builds the typed array with `list.toArray(T[])`\ninstead of `Array.set`.\n\n## Tests\n- **New `TestPrimitiveArrayCodecs`** compares the new codecs against a\ncopy of the old algorithm, for all 14 component types, primitive and\nwrapper:\n- **decode, element by element,** across 41 BSON values:\n`INT32`/`INT64`/`DOUBLE`/`DECIMAL128` at and past every type's range\nlimits, `-0.0`, NaN, ±∞, `Float.MAX_VALUE` × 2, fractional values,\n`null`, one- and two-character and empty strings, and booleans. Each has\nto give the same array, or **fail with the same exception class and\nmessage**.\n- **decode, whole arrays,** of sizes 0, 1, 15, 16, 17, 32 and 40+, using\nevery element that decodes on its own, mixed together. The sizes run in\nsequence on one codec, so the length hint grows and shrinks.\n- **encode** of sample arrays, including the extremes, NaN, `-0.0`,\n`char` `\\u0000`/`ï¿¿`, empty and 40-element arrays, and wrapper arrays\nwith `null`s. Each one is read back and compared with what the old\nalgorithm gives.\n- **a replaced component codec:** a `double` codec that stores tenths as\n`INT64` still sees every element, both ways.\n- It **passes on `master` too**, so it pins existing behavior. Dropping\nthe `short` range check makes it fail.\n- The harness's `HarnessArrays` fixtures and the existing `*MappingTest`\nprimitive tests are unchanged and pass.\n- The core and Kotlin suites pass with `-Dmorphia.mapper=critter` and\nwith reflection: 1532 + 5 tests, 0 failures.\n\n## JMH: `CodecBenchmark` ARRAYS, `master` (`26b10d99ec6`) vs this branch\n`ArraysEntity` holds a `double[1536]` and a `String[20]`, and only the\n`double[]` uses this codec. Same machine, `-prof gc`, master and this\nbranch alternating, two rounds each.\n| variant | benchmark | master ns/op (2 runs) | this branch ns/op (2\nruns) | master B/op | this branch B/op |\n|---|---|---|---|---:|---:|\n| reflection | encode | 97,456 ± 949, 99,591 ± 2,850 | 31,332 ± 350,\n31,637 ± 452 | 135,587 | 98,721 |\n| critter | encode | 97,780 ± 949, 97,990 ± 1,983 | 32,225 ± 503, 31,722\n± 498 | 139,875 | 98,721 |\n| reflection | decode | 68,485 ± 569, 68,547 ± 1,020 | 26,585 ± 321,\n26,434 ± 238 | 74,858 | 15,569 |\n| critter | decode | 68,580 ± 806, 67,753 ± 648 | 26,426 ± 268, 28,224 ±\n2,747 | 74,849–74,858 | 15,569 |\n\n- **Encode is 3.1× faster**, matching the review's 3.0×.\n- **Decode is 2.6× faster and allocates 79% less.** The review measured\n2.4×.\n- Without the length hint, decode was 2.25× faster at 48,337 B/op.\nNearly all of that was the buffer doubling from 16 to 2,048.\n- With the hint, what's left is essentially the `double[1536]` itself\n(12.3 KB) and the strings.\n- **`MixedCodecBenchmark`,** which has no primitive arrays, is unchanged\n(within noise) and serves as a control.\n\n## Decisions to review\n1. **The length hint.** Each primitive codec remembers the length of the\nlast array it decoded and starts its next buffer at that size, capped at\n65,536 elements. The field is a plain `int`: a value raced in from\nanother thread only costs a resize. The cap bounds the up-front\nover-allocation after a single huge array to 512 KB, for a `double[]`.\nIt is per codec, which means per datastore and component type, not per\nproperty, so two `double[]` properties of different lengths will share\nit and still grow. It's a one-line change to drop if you'd rather keep\nit simpler.\n2. **The fast path is skipped when the component codec isn't the\ndriver's.** It checks the exact class (`DoubleCodec.class`, …), so a\nsubclass also gets the boxed path. I'd rather give a custom codec the\nslow path than bypass it.\n3. **I left `ArrayCodec` alone.** It handles arrays of other types, e.g.\nthe benchmark's `String[]`, and the review suggested building its typed\narray directly. 3.5 already cached its component codec, and the plan\nitem scopes this to `TypedArrayCodec`.\n4. **The wrapper decode path uses `list.toArray(T[])`.** If a custom\ncomponent codec returned the wrong element type, that now throws\n`ArrayStoreException` where `Array.set` threw\n`IllegalArgumentException`. Driver codecs can't do this.\n5. **Two quirks that already exist** turned up while writing the\nround-trip test. They're unchanged, and the test pins them:\n- a wrapper array with `null` elements (e.g. `Integer[] {1, null}`)\nencodes the nulls, but can't be read back: the component codecs reject\n`NULL`;\n- a `float[]` holding ±∞ encodes, but `FloatCodec` rejects ±∞ on decode,\nwhile NaN passes.\n\n   Both would be separate issues if you want them fixed.",
          "timestamp": "2026-10-30T15:48:43Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/23772b843402736f12140e1b77298b2978746561"
        },
        "date": 1793381393940,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1214.5012440956139,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 3333.7482791425814,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 10752.065482094558,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 5931.965765878899,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 1220.8084036319667,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 34547.08802373746,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 3101.2257853967735,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 2976.2226300829875,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1230.0280497374733,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 3372.674480964981,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 11139.010540811012,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 6205.94511214324,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 1188.3605600860799,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 35553.195554383594,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 3186.490611588463,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 2979.187425722423,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 924.916109534012,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2679.5852526188205,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 9577.148009419376,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 4724.263756912348,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 896.5514642186278,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 39559.45085212781,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 2537.108184858321,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 1929.5124906043793,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"reflection\"} )",
            "value": 19.161770622290458,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"reflection\"} )",
            "value": 29.44467956635837,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"reflection\"} )",
            "value": 1200.3603976939435,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"reflection\"} )",
            "value": 1354.9526876708119,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"reflection\"} )",
            "value": 5627.637084733125,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 2210.1321194161383,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 10176.805616876305,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1801.4718319275664,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 7426.408667932995,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"reflection\"} )",
            "value": 64.40647482309004,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"reflection\"} )",
            "value": 33043.940722437066,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"reflection\"} )",
            "value": 26549.472003896437,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"reflection\"} )",
            "value": 1682.9504940664053,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"reflection\"} )",
            "value": 375.84763018544913,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"reflection\"} )",
            "value": 244.05000179999996,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"reflection\"} )",
            "value": 1254.4757802,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"reflection\"} )",
            "value": 408.54437219999994,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "f283c310889dc25853d428fc60170f92e2c5043f",
          "message": "Recognize discriminators under an entity's own key in untyped values (#4470)\n\nCloses #4463. Follow-up to D12 (#4454).\n\n## Problem\n\nAn entity can store its discriminator under its own key with\n`@Entity(discriminatorKey = \"...\")`. `ObjectCodec` decides whether a\nstored document is an entity when nothing else says what type it is. It\nlooked only for the configured key (`MorphiaConfig.discriminatorKey()`,\n`_t` by default).\n\nThe issue asked for a test of each path first, to pin down where users\nsee this:\n- **Untyped values: broken.** An entity using its own key, held by a\nfield of type `Object`, a `List<Object>` or a `Map<String, Object>`,\nloaded back as a `Document`. The same entity under the configured key\nloaded as itself. The new `untypedValuesLoadAsTheirEntity` fails on\nmaster with `expected: <OwnKeyKind(a)> but was:\n<Document{{_kind=…OwnKeyKind, name=a}}>`.\n- **Ids stored as documents: worked.** That covers references (eager,\nlazy, id-only, collections), typed id fields, and reference cycles\nthrough such an id. Both the decode session's keys and the references'\nids left them as `Document`s, so the two sides matched.\n\n## Changes\n\n- **`DiscriminatorLookup`** records, as each model registers, the key it\nstores its discriminator under (for models that use one).\n- `isKey(configuredKey, name)`: whether a string under this field name\nmay be a discriminator.\n  - `lookup(configuredKey, name, value)`:\n- Under the configured key, it resolves exactly as before, including the\n`Class.forName` fallback and its error.\n- Under an entity's own key, it resolves only if the value is exactly\nthat entity's discriminator. Anything else returns `null`, so the\ndocument stays a document.\n- **`ObjectCodec`** uses it for untyped values, so an entity under its\nown key loads as itself.\n- **Ids stored as documents are deliberately unchanged.**\n`ObjectCodec.forIds(registry)` returns an id-mode codec that honors only\nthe configured key, which is the old behavior. Every place that reads an\nid stored as a document uses it:\n- the decode session's keys (`EntityDecoder`'s\n`readKey`/`scanForId`/`peekId`);\n  - `ReferenceCodec` for a single reference's id;\n  - `ReferenceCodec` for a `DBRef`'s id;\n- an id property typed `Object` (`@Id Object id`), whose codec in the\nplan is the id-mode one. Otherwise it would decode an own-key id as an\nentity while references look it up as a `Document` (found in Copilot's\nsecond round).\n\n`ReferenceCodec.processId` and `EntityDecoder.decodesAsEntity` are as on\nmaster.\n\n### Why ids are left alone (Copilot review, round 1)\n\nThe first version of this PR applied own keys to ids too, changing\n`ObjectCodec`, `processId` and `decodesAsEntity` together, as the issue\nproposed. Copilot found three ways that broke own-key ids that worked on\nmaster, and new tests confirm each:\n- **A lifecycle owner** whose id has its own key isn't registered in the\nsession before its properties decode. `decodesAsEntity` says not to read\nan entity id ahead, so an eager self-reference overflows the stack.\n- **An `idOnly` `Map` of references** looks up its values as raw\n`Document`s while the session now holds id instances, so the cycle never\ncloses and the stack overflows.\n- **An owner without callbacks** reads its own-key id as an entity once\nfor the session key and again for the property, so the id's `@PostLoad`\nruns twice.\n\nUsers don't hit any of these with own-key ids, because those ids stay\n`Document`s on both sides. Converting them to entities brings in\nproblems that ids under the **configured** key already have on master. A\nscratch run of the same three scenarios with `_t`-keyed compound ids\ngives two stack overflows and a doubled `@PostLoad`. Making ids entities\nsafely needs a design for session keys: probably a canonical raw form on\nboth sides, plus callback-safe key reads. That's a separate change,\ntracked in #4471, #4472 and #4473 for the configured-key cases. So this\nPR fixes the user-visible bug, untyped values, and keeps every id path\nexactly as it was.\n\n## ⚠️ Behavior change\n\n**An untyped document can now load as an entity where it used to stay a\n`Document`.** A stored document in an untyped place (`Object`,\n`List<Object>`, `Map<String, Object>`) loads as an entity when two\nthings hold:\n- it has a string field named like an entity's own discriminator key;\n- the field holds **exactly that entity's discriminator**.\n\nFor example, with `@Entity(discriminatorKey = \"type\", discriminator =\n\"PNG\") class Png` (`dev.morphia.test.models.Png` is one), `{type: \"PNG\",\n…}` in a `Map<String, Object>` now loads as a `Png`. That's the fix for\nentities stored there. But plain user data that happens to match would\nalso convert.\n\nThe match is deliberately narrow:\n- the field must be a key some mapped entity declares;\n- the value must be a string;\n- it must be that same entity's discriminator.\n\nA `type: \"something else\"`, a `type: 5`, or another entity's\ndiscriminator under that key leaves the document alone.\n`documentsWithAFieldOfThatNameStayDocuments` covers those three. This is\nthe rule that has always applied to the configured `_t` key, extended to\nthe keys entities choose themselves.\n\nMigration note: MorphiaOrg/morphia-docs#26 adds \"Untyped values and\nper-entity discriminator keys\" to the 3.0 migration page. It describes\nthis change and what to check. #4459 (the 2.5 → 3.0 audit) should pick\nit up too.\n\n## Decisions to review\n\n- **Untyped values only, not ids.** See above.\n- **Exact match on the entity's own key**, not any registered\ndiscriminator under any declared key. That keeps plain documents from\nconverting as far as possible.\n- **The configured key is unchanged,** in untyped values and in ids.\n- **Field order decides between two discriminators.** An untyped\ndocument holding discriminators under both the configured key and an\nentity's own key resolves by whichever comes first, as `ObjectCodec`\nalready stopped at the first match. Morphia never writes both for one\nentity. Since ids only honor the configured key, this can't make the\nsession and references disagree. Scanning every field to prefer one key\nwasn't worth the cost for documents Morphia doesn't produce.\n- **`ObjectCodec.forIds` only swaps in the id mode for Morphia's own\n`ObjectCodec`** (exact class). A subclass a provider supplies keeps\nreading ids its own way (found in Copilot's round 3; covered by\n`aCustomObjectCodecStillReadsIds`).\n- **Known limitation, not addressed:** an id typed as a `Map` whose\nvalues include an entity with its own key. The id property decodes such\na nested value as the entity, while the session key keeps it a\n`Document`. Copilot's third-round summary hinted at this without an\ninline comment, and I haven't reproduced it. It falls under the\nsession-key design in #4471–#4473.\n- **Only mapped entities are recognized.** A key is known once its\nentity is mapped, which with the default package scan happens at\nstartup.\n\n## Testing\n\nNew `TestOwnDiscriminatorKey`, run against a server:\n- `untypedValuesLoadAsTheirEntity`: an `Object` field, a `List<Object>`\nand a `Map<String, Object>` holding an entity with its own key, plus a\ncontrol under the configured key. **Fails on master.**\n- `documentsWithAFieldOfThatNameStayDocuments`: plain documents whose\n`_kind` field holds a non-discriminator string, a number, or another\nentity's discriminator all stay `Document`s.\n- Guards that own-key ids behave as on master. Each passes on master,\nand the last three **failed on this PR's first version**:\n- `referencesByAnIdWithItsOwnKey`: eager, lazy, id-only and list\nreferences;\n  - `anIdWithItsOwnKeyLoadsOnce`: a lifecycle owner;\n- `aLifecycleOwnerClosesACycleThroughAnIdWithItsOwnKey`: the review's\nfirst case;\n  - `aMapOfReferencesClosesACycleThroughAnIdWithItsOwnKey`: its second;\n  - `anIdWithItsOwnKeyLoadsOnceUnderAnOwnerWithoutCallbacks`: its third.\n- `aListOfReferencesFindsTargetsWithAnUntypedIdWithItsOwnKey`: an `@Id\nObject id` holding an own-key id, referenced only from a list. It came\nback empty on the second version (Copilot's round 2) and is found now.\n\nAlso:\n- `TestDiscriminatorResolution.discriminatorsResolveUnderTheirOwnKey`\nunit-tests `isKey`/`lookup` on both mappers.\n- `TestReferences.compoundIdsWithTheirOwnDiscriminatorKeyCloseCycles`\n(from D12) still passes.\n- Core suite passes with `critter` and with `reflection` (2,535 tests,\n16 skipped).\n- Local Copilot CLI review, one round, on the first version. It found a\nmisplaced javadoc and a configured-key inconsistency in\n`decodesAsEntity`. Both are moot now, since that code is back to master.\n\n## Benchmarks\n\nNone run. The changed code only runs for untyped values, meaning\n`ObjectCodec` on a document, and none of the benchmark models has one.\nIn the scan, the added cost per field is one `getCurrentBsonType()`,\nplus a set lookup for string fields. Id reads take the same path as\nbefore.",
          "timestamp": "2026-11-11T06:35:39Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/f283c310889dc25853d428fc60170f92e2c5043f"
        },
        "date": 1794380526918,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 695.7319101749093,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 1961.526821131218,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 6638.513301812782,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 3326.98413800708,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 418.6220294025042,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 25610.31370538387,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 2049.8080339233693,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 2094.812291667045,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 758.0982139238306,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2089.8555320159066,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 6935.921902622781,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 3478.410129401552,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 437.8454077297476,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 25488.27893533125,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 2149.2867059395503,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 2267.16375820507,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 808.0168485421168,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2341.945376178505,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 8292.800284204235,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 4435.451246310031,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 381.77851822684556,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 33403.704069139174,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 2108.5999889557393,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 1854.0878791458556,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"reflection\"} )",
            "value": 3.412681794097062,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"reflection\"} )",
            "value": 7.230427706161699,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"reflection\"} )",
            "value": 690.0683161215607,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"reflection\"} )",
            "value": 1051.3692227277581,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"reflection\"} )",
            "value": 3748.3992920810983,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1833.6046134839503,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 8363.994801473913,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 738.2378533556198,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2042.1546448874465,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1557.3194875088623,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 6041.863487032327,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 855.5604041553585,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2548.0547323114915,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"reflection\"} )",
            "value": 46.00097120774038,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"reflection\"} )",
            "value": 20050.04847774419,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"reflection\"} )",
            "value": 23195.983394955816,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"reflection\"} )",
            "value": 1340.5125546416834,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"reflection\"} )",
            "value": 316.6450363420048,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"reflection\"} )",
            "value": 180.54534199999998,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"reflection\"} )",
            "value": 942.587047,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"reflection\"} )",
            "value": 312.451037,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "1ff98e9eebbf9bf81680d3cd4c85f33167ba20c6",
          "message": "Fix stack overflow for recursive embedded types (#4510)\n\nPreserve the supplied codec registry during property resolution to retain cycle detection for recursive entity types. Add persistence regressions for recursive fields, containers, refresh, and custom codecs.",
          "timestamp": "2026-11-16T02:59:33Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/1ff98e9eebbf9bf81680d3cd4c85f33167ba20c6"
        },
        "date": 1794845356408,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 945.62024179776,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2803.8757283552645,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 9539.546908972123,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 4806.0716367880905,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 563.0479352177289,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 34598.00763340986,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 2794.863765481908,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 2632.117076508539,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"reflection\"} )",
            "value": 27387.628184303157,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"reflection\"} )",
            "value": 25636.872195160766,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"reflection\"} )",
            "value": 1557.3908871433218,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"reflection\"} )",
            "value": 343.7008996194873,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"reflection\"} )",
            "value": 237.43061749999998,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"reflection\"} )",
            "value": 1211.9758934000001,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"reflection\"} )",
            "value": 402.75235340000006,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 967.6984661021113,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2809.9498138543295,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 10020.837595813535,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 4796.898930696867,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 580.8837075708351,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 35349.62942203092,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 2846.8755829999814,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 2645.6942078854045,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 911.2583309420503,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2682.765696127235,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 9464.857758379729,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 4688.095538945245,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 438.6822424664998,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 38795.42613254175,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 2565.5270848897567,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 1981.5998701546794,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"reflection\"} )",
            "value": 4.335496543813765,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"reflection\"} )",
            "value": 9.049915877596744,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"reflection\"} )",
            "value": 922.5703336700516,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"reflection\"} )",
            "value": 1388.1020095804465,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"reflection\"} )",
            "value": 5231.297759248413,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 2258.987294240436,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 10276.068772693576,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 989.4092047383452,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2979.428166037493,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1840.1233800894022,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 7457.3091529414405,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 953.1875060664571,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2854.408727923034,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"reflection\"} )",
            "value": 58.56567108719146,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "05759847a73c50e1444ddf35163c18b7bea69bcf",
          "message": "Bump the maven-dependencies group with 4 updates (#4526)\n\nBumps the maven-dependencies group with 4 updates:\n[ch.qos.logback:logback-classic](https://github.com/qos-ch/logback),\n[com.google.devtools.ksp:symbol-processing-api](https://github.com/google/ksp),\n[com.google.devtools.ksp:symbol-processing-common-deps](https://github.com/google/ksp)\nand\n[com.google.devtools.ksp:symbol-processing-aa-embeddable](https://github.com/google/ksp).\n\nUpdates `ch.qos.logback:logback-classic` from 1.6.6 to 1.6.7\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/qos-ch/logback/releases\">ch.qos.logback:logback-classic's\nreleases</a>.</em></p>\n<blockquote>\n<h2>Logback 1.6.7</h2>\n<h3>2026-11-14 Release of logback version 1.6.7</h3>\n<p>• The <code>scan</code> attribute of the\n<code>&lt;propertiesConfigurator&gt;</code> element, when\nset, now takes precedence over the <code>scan</code> attribute of the\ntop-level\n<code>&lt;configuration&gt;</code> element, in both directions. With\n<code>scan=&quot;true&quot;</code>\nand scanning disabled or unset at the top level, the properties file\nis now actually monitored for changes. Previously, the file was\nadded to the watch list but no task ever polled it. The scan period\nis taken from the <code>scanPeriod</code> attribute of the top-level\n<code>&lt;configuration&gt;</code> element. Conversely, with\n<code>scan=&quot;false&quot;</code> and\n<code>scan=&quot;true&quot;</code> at the top level, the properties file\nis no longer\nmonitored. Moreover, when several <code>&lt;propertiesConfigurator\nscan=&quot;true&quot;&gt;</code>\nelements are present without top-level scanning, all\nthe referenced files are now monitored. Previously, only the last\none was.</p>\n<p>• Two new modules, <em>logback-core-kryo</em> and\n<em>logback-classic-kryo</em>,\nencode and decode logging events with the <a\nhref=\"https://github.com/EsotericSoftware/kryo\">Kryo 5</a> serialization\nlibrary. <a\nhref=\"https://logback.qos.ch/manual/encoders.html#KryoEncoder\"><code>KryoEncoder</code></a>\nwrites <code>ILoggingEvent</code> instances into a\n<strong>compact binary stream</strong> and <code>KryoDecoder</code>\nreads them back. The\nstream is framed so that the decoder can recover from torn or truncated\nwrites, for example after a crash of the application. Both modules\nrequire\nJava 11 and ship with JPMS module descriptors, namely\n<code>ch.qos.logback.core.kryo</code>\nand <code>ch.qos.logback.classic.kryo</code>. See also the relevant\ndocumentation\nat <a\nhref=\"https://logback.qos.ch/manual/encoders.html#KryoEncoder\">KryoEncoder</a>.</p>\n<p>• Added the <code>toBooleanObject(String, Boolean)</code> and\n<code>toLopsidedBooleanObject</code> methods in\n<code>OptionHelper</code>.</p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/8b97c2fe6dd0cfa298ed3c3dd4f500b942edc63a\"><code>8b97c2f</code></a>\nprepare release 1.6.7</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/764b5c4613edb5c0920edfe7ff2404e778a0f0f1\"><code>764b5c4</code></a>\nkryo: use stream version 0 when KryoEncoder is set to Vanilla</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/b29f465c3a3556a5432f768d37f0b3e127ebc076\"><code>b29f465</code></a>\nkryo: remove V3 and V4 stream encodings, keep Vanilla and V5</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/d42c0129146630e7d769e9e8c2261bb68a3c04e5\"><code>d42c012</code></a>\nadd logback-core-kryo and logback-classic-kryo modules</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/069d51abcd1549d67f872fc4a315d585150f8087\"><code>069d51a</code></a>\nremove dead code</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/271d89c82cad3b7071478610e2eea2013210bb6f\"><code>271d89c</code></a>\nhonor scan attribute of &lt;propertiesConfigurator&gt; in both\ndirections</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/b01611fddfddd58ff03be4c7cbb37b6a4e7161ca\"><code>b01611f</code></a>\nadded a OptionHelper#toLopsidedBooleanObject</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/b1267ca05580340c42aca3985394de6e9093e501\"><code>b1267ca</code></a>\nstart work on 1.6.7-SNAPSHOT</li>\n<li>See full diff in <a\nhref=\"https://github.com/qos-ch/logback/compare/v_1.6.6...v_1.6.7\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\nUpdates `com.google.devtools.ksp:symbol-processing-api` from 2.3.13 to\n2.3.14\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/google/ksp/releases\">com.google.devtools.ksp:symbol-processing-api's\nreleases</a>.</em></p>\n<blockquote>\n<h2>2.3.14</h2>\n<h2>What's Changed</h2>\n<ul>\n<li>Fix regression where library symbols disappear during concurrent KSP\nexecutions by only clearing <code>CoreJarFileSystem</code> handlers when\nno executions are active (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3286\">#3286</a>)</li>\n<li>Implement support for reading Kotlin context parameters\n(<code>KSContextParameter</code>) on functions and properties (<a\nhref=\"https://redirect.github.com/google/ksp/issues/2472\">#2472</a>)</li>\n<li>Skip Analysis API session initialization and processor execution\nduring incremental builds when no files are dirty (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3256\">#3256</a>)</li>\n<li>Fix <code>KSAnnotation.arguments</code> omitting <code>vararg</code>\nparameters when annotations are applied with no arguments (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3271\">#3271</a>)</li>\n<li>Align <code>KSAnnotation.defaultArguments</code> behavior across\nplatforms by not falling back to an empty array on JVM for array-typed\nparameters (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3233\">#3233</a>)</li>\n<li>Fix <code>KSTypeParameter.bounds</code> returning expanded type\narguments when the bound is a type alias (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3242\">#3242</a>)</li>\n</ul>\n<h2>Contributors</h2>\n<ul>\n<li>Thanks to <a\nhref=\"https://github.com/bcorso\"><code>@​bcorso</code></a>, and everyone\nwho reported bugs and participated in discussions!</li>\n</ul>\n<p>Full Changelog: <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">https://github.com/google/ksp/compare/2.3.13...2.3.14</a></p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a3aaaabae570bae3ab4b84e72385483a14ab4498\"><code>a3aaaab</code></a>\nSkip impossible URL lookups</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/ff066aca3f800b8c61cf4d1c20e7518035247e84\"><code>ff066ac</code></a>\nFix potential memory leak in jarFileSystem cleanup</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/e2c1e7696a8f5380cb5515b4d6f2caa9774e269c\"><code>e2c1e76</code></a>\nRun LibraryRootsSearchScopeTest in isolation</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/5f7fa4b24ab2ebab15bff5da6a34539208108d50\"><code>5f7fa4b</code></a>\nKSP2: Skip the Analysis API session and processors when an incremental\nrun ha...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/3ed0f292b993796fa2df68d4ca203257e73dc015\"><code>3ed0f29</code></a>\nRead KSBackingFieldJavaImpl.modifiers from the underlying Java field\nPSI</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a2133e0e320a6bc14fac30010dfdfb5e2d342b12\"><code>a2133e0</code></a>\nRemove wrong leading '/' in test path</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/9dd67d2487a51853d68850ee2d21f17b6158ab9e\"><code>9dd67d2</code></a>\nFix KSBackingField case in <code>mapToJvmSignatureInternal</code></li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/cae8dc66f772085e12901220b17375c631ab7388\"><code>cae8dc6</code></a>\nAdd helper functions in KSPUnitTestSuite</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/96ba20a67aaf8fe7c48ceab49c2fb620c59aee71\"><code>96ba20a</code></a>\nOnly clear CoreJarFileSystem handlers when no concurrent KSP executions\nare a...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/1b0eb9a1696a03ffa53429078af9700b483dad47\"><code>1b0eb9a</code></a>\nFallback to root URLs in LibraryRootsSearchScope when JAR handlers are\ncleared</li>\n<li>Additional commits viewable in <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\nUpdates `com.google.devtools.ksp:symbol-processing-common-deps` from\n2.3.13 to 2.3.14\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/google/ksp/releases\">com.google.devtools.ksp:symbol-processing-common-deps's\nreleases</a>.</em></p>\n<blockquote>\n<h2>2.3.14</h2>\n<h2>What's Changed</h2>\n<ul>\n<li>Fix regression where library symbols disappear during concurrent KSP\nexecutions by only clearing <code>CoreJarFileSystem</code> handlers when\nno executions are active (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3286\">#3286</a>)</li>\n<li>Implement support for reading Kotlin context parameters\n(<code>KSContextParameter</code>) on functions and properties (<a\nhref=\"https://redirect.github.com/google/ksp/issues/2472\">#2472</a>)</li>\n<li>Skip Analysis API session initialization and processor execution\nduring incremental builds when no files are dirty (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3256\">#3256</a>)</li>\n<li>Fix <code>KSAnnotation.arguments</code> omitting <code>vararg</code>\nparameters when annotations are applied with no arguments (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3271\">#3271</a>)</li>\n<li>Align <code>KSAnnotation.defaultArguments</code> behavior across\nplatforms by not falling back to an empty array on JVM for array-typed\nparameters (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3233\">#3233</a>)</li>\n<li>Fix <code>KSTypeParameter.bounds</code> returning expanded type\narguments when the bound is a type alias (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3242\">#3242</a>)</li>\n</ul>\n<h2>Contributors</h2>\n<ul>\n<li>Thanks to <a\nhref=\"https://github.com/bcorso\"><code>@​bcorso</code></a>, and everyone\nwho reported bugs and participated in discussions!</li>\n</ul>\n<p>Full Changelog: <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">https://github.com/google/ksp/compare/2.3.13...2.3.14</a></p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a3aaaabae570bae3ab4b84e72385483a14ab4498\"><code>a3aaaab</code></a>\nSkip impossible URL lookups</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/ff066aca3f800b8c61cf4d1c20e7518035247e84\"><code>ff066ac</code></a>\nFix potential memory leak in jarFileSystem cleanup</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/e2c1e7696a8f5380cb5515b4d6f2caa9774e269c\"><code>e2c1e76</code></a>\nRun LibraryRootsSearchScopeTest in isolation</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/5f7fa4b24ab2ebab15bff5da6a34539208108d50\"><code>5f7fa4b</code></a>\nKSP2: Skip the Analysis API session and processors when an incremental\nrun ha...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/3ed0f292b993796fa2df68d4ca203257e73dc015\"><code>3ed0f29</code></a>\nRead KSBackingFieldJavaImpl.modifiers from the underlying Java field\nPSI</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a2133e0e320a6bc14fac30010dfdfb5e2d342b12\"><code>a2133e0</code></a>\nRemove wrong leading '/' in test path</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/9dd67d2487a51853d68850ee2d21f17b6158ab9e\"><code>9dd67d2</code></a>\nFix KSBackingField case in <code>mapToJvmSignatureInternal</code></li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/cae8dc66f772085e12901220b17375c631ab7388\"><code>cae8dc6</code></a>\nAdd helper functions in KSPUnitTestSuite</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/96ba20a67aaf8fe7c48ceab49c2fb620c59aee71\"><code>96ba20a</code></a>\nOnly clear CoreJarFileSystem handlers when no concurrent KSP executions\nare a...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/1b0eb9a1696a03ffa53429078af9700b483dad47\"><code>1b0eb9a</code></a>\nFallback to root URLs in LibraryRootsSearchScope when JAR handlers are\ncleared</li>\n<li>Additional commits viewable in <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\nUpdates `com.google.devtools.ksp:symbol-processing-aa-embeddable` from\n2.3.13 to 2.3.14\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/google/ksp/releases\">com.google.devtools.ksp:symbol-processing-aa-embeddable's\nreleases</a>.</em></p>\n<blockquote>\n<h2>2.3.14</h2>\n<h2>What's Changed</h2>\n<ul>\n<li>Fix regression where library symbols disappear during concurrent KSP\nexecutions by only clearing <code>CoreJarFileSystem</code> handlers when\nno executions are active (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3286\">#3286</a>)</li>\n<li>Implement support for reading Kotlin context parameters\n(<code>KSContextParameter</code>) on functions and properties (<a\nhref=\"https://redirect.github.com/google/ksp/issues/2472\">#2472</a>)</li>\n<li>Skip Analysis API session initialization and processor execution\nduring incremental builds when no files are dirty (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3256\">#3256</a>)</li>\n<li>Fix <code>KSAnnotation.arguments</code> omitting <code>vararg</code>\nparameters when annotations are applied with no arguments (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3271\">#3271</a>)</li>\n<li>Align <code>KSAnnotation.defaultArguments</code> behavior across\nplatforms by not falling back to an empty array on JVM for array-typed\nparameters (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3233\">#3233</a>)</li>\n<li>Fix <code>KSTypeParameter.bounds</code> returning expanded type\narguments when the bound is a type alias (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3242\">#3242</a>)</li>\n</ul>\n<h2>Contributors</h2>\n<ul>\n<li>Thanks to <a\nhref=\"https://github.com/bcorso\"><code>@​bcorso</code></a>, and everyone\nwho reported bugs and participated in discussions!</li>\n</ul>\n<p>Full Changelog: <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">https://github.com/google/ksp/compare/2.3.13...2.3.14</a></p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a3aaaabae570bae3ab4b84e72385483a14ab4498\"><code>a3aaaab</code></a>\nSkip impossible URL lookups</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/ff066aca3f800b8c61cf4d1c20e7518035247e84\"><code>ff066ac</code></a>\nFix potential memory leak in jarFileSystem cleanup</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/e2c1e7696a8f5380cb5515b4d6f2caa9774e269c\"><code>e2c1e76</code></a>\nRun LibraryRootsSearchScopeTest in isolation</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/5f7fa4b24ab2ebab15bff5da6a34539208108d50\"><code>5f7fa4b</code></a>\nKSP2: Skip the Analysis API session and processors when an incremental\nrun ha...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/3ed0f292b993796fa2df68d4ca203257e73dc015\"><code>3ed0f29</code></a>\nRead KSBackingFieldJavaImpl.modifiers from the underlying Java field\nPSI</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a2133e0e320a6bc14fac30010dfdfb5e2d342b12\"><code>a2133e0</code></a>\nRemove wrong leading '/' in test path</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/9dd67d2487a51853d68850ee2d21f17b6158ab9e\"><code>9dd67d2</code></a>\nFix KSBackingField case in <code>mapToJvmSignatureInternal</code></li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/cae8dc66f772085e12901220b17375c631ab7388\"><code>cae8dc6</code></a>\nAdd helper functions in KSPUnitTestSuite</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/96ba20a67aaf8fe7c48ceab49c2fb620c59aee71\"><code>96ba20a</code></a>\nOnly clear CoreJarFileSystem handlers when no concurrent KSP executions\nare a...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/1b0eb9a1696a03ffa53429078af9700b483dad47\"><code>1b0eb9a</code></a>\nFallback to root URLs in LibraryRootsSearchScope when JAR handlers are\ncleared</li>\n<li>Additional commits viewable in <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\nUpdates `com.google.devtools.ksp:symbol-processing-common-deps` from\n2.3.13 to 2.3.14\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/google/ksp/releases\">com.google.devtools.ksp:symbol-processing-common-deps's\nreleases</a>.</em></p>\n<blockquote>\n<h2>2.3.14</h2>\n<h2>What's Changed</h2>\n<ul>\n<li>Fix regression where library symbols disappear during concurrent KSP\nexecutions by only clearing <code>CoreJarFileSystem</code> handlers when\nno executions are active (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3286\">#3286</a>)</li>\n<li>Implement support for reading Kotlin context parameters\n(<code>KSContextParameter</code>) on functions and properties (<a\nhref=\"https://redirect.github.com/google/ksp/issues/2472\">#2472</a>)</li>\n<li>Skip Analysis API session initialization and processor execution\nduring incremental builds when no files are dirty (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3256\">#3256</a>)</li>\n<li>Fix <code>KSAnnotation.arguments</code> omitting <code>vararg</code>\nparameters when annotations are applied with no arguments (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3271\">#3271</a>)</li>\n<li>Align <code>KSAnnotation.defaultArguments</code> behavior across\nplatforms by not falling back to an empty array on JVM for array-typed\nparameters (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3233\">#3233</a>)</li>\n<li>Fix <code>KSTypeParameter.bounds</code> returning expanded type\narguments when the bound is a type alias (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3242\">#3242</a>)</li>\n</ul>\n<h2>Contributors</h2>\n<ul>\n<li>Thanks to <a\nhref=\"https://github.com/bcorso\"><code>@​bcorso</code></a>, and everyone\nwho reported bugs and participated in discussions!</li>\n</ul>\n<p>Full Changelog: <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">https://github.com/google/ksp/compare/2.3.13...2.3.14</a></p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a3aaaabae570bae3ab4b84e72385483a14ab4498\"><code>a3aaaab</code></a>\nSkip impossible URL lookups</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/ff066aca3f800b8c61cf4d1c20e7518035247e84\"><code>ff066ac</code></a>\nFix potential memory leak in jarFileSystem cleanup</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/e2c1e7696a8f5380cb5515b4d6f2caa9774e269c\"><code>e2c1e76</code></a>\nRun LibraryRootsSearchScopeTest in isolation</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/5f7fa4b24ab2ebab15bff5da6a34539208108d50\"><code>5f7fa4b</code></a>\nKSP2: Skip the Analysis API session and processors when an incremental\nrun ha...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/3ed0f292b993796fa2df68d4ca203257e73dc015\"><code>3ed0f29</code></a>\nRead KSBackingFieldJavaImpl.modifiers from the underlying Java field\nPSI</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a2133e0e320a6bc14fac30010dfdfb5e2d342b12\"><code>a2133e0</code></a>\nRemove wrong leading '/' in test path</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/9dd67d2487a51853d68850ee2d21f17b6158ab9e\"><code>9dd67d2</code></a>\nFix KSBackingField case in <code>mapToJvmSignatureInternal</code></li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/cae8dc66f772085e12901220b17375c631ab7388\"><code>cae8dc6</code></a>\nAdd helper functions in KSPUnitTestSuite</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/96ba20a67aaf8fe7c48ceab49c2fb620c59aee71\"><code>96ba20a</code></a>\nOnly clear CoreJarFileSystem handlers when no concurrent KSP executions\nare a...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/1b0eb9a1696a03ffa53429078af9700b483dad47\"><code>1b0eb9a</code></a>\nFallback to root URLs in LibraryRootsSearchScope when JAR handlers are\ncleared</li>\n<li>Additional commits viewable in <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\nUpdates `com.google.devtools.ksp:symbol-processing-aa-embeddable` from\n2.3.13 to 2.3.14\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/google/ksp/releases\">com.google.devtools.ksp:symbol-processing-aa-embeddable's\nreleases</a>.</em></p>\n<blockquote>\n<h2>2.3.14</h2>\n<h2>What's Changed</h2>\n<ul>\n<li>Fix regression where library symbols disappear during concurrent KSP\nexecutions by only clearing <code>CoreJarFileSystem</code> handlers when\nno executions are active (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3286\">#3286</a>)</li>\n<li>Implement support for reading Kotlin context parameters\n(<code>KSContextParameter</code>) on functions and properties (<a\nhref=\"https://redirect.github.com/google/ksp/issues/2472\">#2472</a>)</li>\n<li>Skip Analysis API session initialization and processor execution\nduring incremental builds when no files are dirty (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3256\">#3256</a>)</li>\n<li>Fix <code>KSAnnotation.arguments</code> omitting <code>vararg</code>\nparameters when annotations are applied with no arguments (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3271\">#3271</a>)</li>\n<li>Align <code>KSAnnotation.defaultArguments</code> behavior across\nplatforms by not falling back to an empty array on JVM for array-typed\nparameters (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3233\">#3233</a>)</li>\n<li>Fix <code>KSTypeParameter.bounds</code> returning expanded type\narguments when the bound is a type alias (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3242\">#3242</a>)</li>\n</ul>\n<h2>Contributors</h2>\n<ul>\n<li>Thanks to <a\nhref=\"https://github.com/bcorso\"><code>@​bcorso</code></a>, and everyone\nwho reported bugs and participated in discussions!</li>\n</ul>\n<p>Full Changelog: <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">https://github.com/google/ksp/compare/2.3.13...2.3.14</a></p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a3aaaabae570bae3ab4b84e72385483a14ab4498\"><code>a3aaaab</code></a>\nSkip impossible URL lookups</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/ff066aca3f800b8c61cf4d1c20e7518035247e84\"><code>ff066ac</code></a>\nFix potential memory leak in jarFileSystem cleanup</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/e2c1e7696a8f5380cb5515b4d6f2caa9774e269c\"><code>e2c1e76</code></a>\nRun LibraryRootsSearchScopeTest in isolation</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/5f7fa4b24ab2ebab15bff5da6a34539208108d50\"><code>5f7fa4b</code></a>\nKSP2: Skip the Analysis API session and processors when an incremental\nrun ha...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/3ed0f292b993796fa2df68d4ca203257e73dc015\"><code>3ed0f29</code></a>\nRead KSBackingFieldJavaImpl.modifiers from the underlying Java field\nPSI</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a2133e0e320a6bc14fac30010dfdfb5e2d342b12\"><code>a2133e0</code></a>\nRemove wrong leading '/' in test path</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/9dd67d2487a51853d68850ee2d21f17b6158ab9e\"><code>9dd67d2</code></a>\nFix KSBackingField case in <code>mapToJvmSignatureInternal</code></li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/cae8dc66f772085e12901220b17375c631ab7388\"><code>cae8dc6</code></a>\nAdd helper functions in KSPUnitTestSuite</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/96ba20a67aaf8fe7c48ceab49c2fb620c59aee71\"><code>96ba20a</code></a>\nOnly clear CoreJarFileSystem handlers when no concurrent KSP executions\nare a...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/1b0eb9a1696a03ffa53429078af9700b483dad47\"><code>1b0eb9a</code></a>\nFallback to root URLs in LibraryRootsSearchScope when JAR handlers are\ncleared</li>\n<li>Additional commits viewable in <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\n\nDependabot will resolve any conflicts with this PR as long as you don't\nalter it yourself. You can also trigger a rebase manually by commenting\n`@dependabot rebase`.\n\n[//]: # (dependabot-automerge-start)\n[//]: # (dependabot-automerge-end)\n\n---\n\n<details>\n<summary>Dependabot commands and options</summary>\n<br />\n\nYou can trigger Dependabot actions by commenting on this PR:\n- `@dependabot rebase` will rebase this PR\n- `@dependabot recreate` will recreate this PR, overwriting any edits\nthat have been made to it\n- `@dependabot show <dependency name> ignore conditions` will show all\nof the ignore conditions of the specified dependency\n- `@dependabot ignore <dependency name> major version` will close this\ngroup update PR and stop Dependabot creating any more for the specific\ndependency's major version (unless you unignore this specific\ndependency's major version or upgrade to it yourself)\n- `@dependabot ignore <dependency name> minor version` will close this\ngroup update PR and stop Dependabot creating any more for the specific\ndependency's minor version (unless you unignore this specific\ndependency's minor version or upgrade to it yourself)\n- `@dependabot ignore <dependency name>` will close this group update PR\nand stop Dependabot creating any more for the specific dependency\n(unless you unignore this specific dependency or upgrade to it yourself)\n- `@dependabot unignore <dependency name>` will remove all of the ignore\nconditions of the specified dependency\n- `@dependabot unignore <dependency name> <ignore condition>` will\nremove the ignore condition of the specified dependency and ignore\nconditions\n\n\n</details>\n\nSigned-off-by: dependabot[bot] <support@github.com>\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2026-11-20T10:00:13Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/05759847a73c50e1444ddf35163c18b7bea69bcf"
        },
        "date": 1795459356661,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 936.9954855175971,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2721.8565266328214,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 9600.773394142798,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 4664.305278285562,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 588.2754002690302,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 34786.60133737804,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 2775.472479695997,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 2567.684937970138,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"reflection\"} )",
            "value": 27227.40546818489,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"reflection\"} )",
            "value": 25540.507998527566,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"reflection\"} )",
            "value": 1554.0688448909789,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"reflection\"} )",
            "value": 347.072979060448,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"reflection\"} )",
            "value": 240.55772019999995,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"reflection\"} )",
            "value": 1201.8622884000001,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"reflection\"} )",
            "value": 420.588527,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 959.175242490065,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2767.871954022093,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 10078.837091774574,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 4759.1531368750175,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 570.4496094360986,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 35238.877803387535,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 2842.734568377883,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 2684.130997468001,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 911.6690725152336,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2685.531803699223,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 9549.071577261975,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"reflection\"} )",
            "value": 4675.386404423633,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"reflection\"} )",
            "value": 439.3018365331038,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"reflection\"} )",
            "value": 40514.306346695026,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"reflection\"} )",
            "value": 2572.0920120184737,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"reflection\"} )",
            "value": 1970.6903611046966,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"reflection\"} )",
            "value": 4.284675425835612,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"reflection\"} )",
            "value": 8.847768337624824,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"reflection\"} )",
            "value": 917.752458563428,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"reflection\"} )",
            "value": 1376.381751033348,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"reflection\"} )",
            "value": 5152.485769842611,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 2216.431630505902,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 10238.391806898602,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 982.5147977294014,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2943.423637964981,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 1841.5194707120334,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 7502.3915281465925,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"reflection\"} )",
            "value": 947.5491509792695,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"reflection\"} )",
            "value": 2820.8577375156146,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"reflection\"} )",
            "value": 58.09972846872423,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          }
        ]
      }
    ],
    "Mapper: critter": [
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "cc32cd505db2989982b1bfe1004f58698c4fa5fd",
          "message": "Fix critter decode building a new Conversions per property (#4338)\n\n## Problem\nCritter-mapped entities decoded about 10× slower than reflective ones\nand allocated about 15× more per decode. The new mapper benchmarks\n(#4336) surfaced this.\n\nJFR showed almost all the time and allocation inside\n`Conversions.<init>`, called from `PropertyModel.setValue`:\n\n```java\nConversions c = conversions != null ? conversions\n        : new Conversions(Thread.currentThread().getContextClassLoader());\n```\n\nReflective property models get `.conversions(mapper.getConversions())`\nfrom `FieldDiscovery` / `MethodDiscovery`. Critter's generated property\nmodels never had it set. Every property set during decode therefore\nrebuilt the whole conversions registry: a few dozen map entries and\nlambdas.\n\n## Fix\n`CritterEntityModel.configureProperties(mapper)` now also sets\n`property.conversions(mapper.getConversions())`. That method already\nruns at the end of every generated entity-model constructor, AOT and\nruntime alike. Existing AOT output picks up the fix without\nregeneration.\n\n## Results\nIn-memory JMH benchmarks from #4336, JDK 21, short warmed runs:\n\n| decode | reflection | critter before | critter after |\n|---|---:|---:|---:|\n| SIMPLE | 2.6 µs, 3,080 B/op | 25 µs, 46,089 B/op | 2.4 µs, 3,144 B/op\n|\n| NESTED | 12.1 µs, 28,944 B/op | 62 µs, 136,562 B/op | 12.0 µs, 28,864\nB/op |\n\nCOLLECTIONS, POLYMORPHIC and LIFECYCLE also now match reflection for\nboth critter AOT and critter runtime. Encode was never affected.\n\n## Testing\n- Full `morphia-core` suite (Testcontainers MongoDB 8.0.0):\n- `-Dmorphia.mapper=critter`: 1,274 tests, 0 failures, 0 errors, 17\nskipped.\n- `-Dmorphia.mapper=reflection`: 1,274 tests, 0 failures, 0 errors, 17\nskipped.\n- `spotless:apply` makes no changes.\n\n🤖 Generated with [Claude Code](https://claude.com/claude-code)\n\nhttps://claude.ai/code/session_01MExceZHKD4HFFCDa6tXn8g\n\nCo-authored-by: Claude <noreply@anthropic.com>",
          "timestamp": "2026-10-04T01:02:07Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/cc32cd505db2989982b1bfe1004f58698c4fa5fd"
        },
        "date": 1791082003404,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 920.8526157450307,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 4670.580064221319,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 13082.725350475675,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 10128.72730217114,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 673.0829763956377,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 754.9703919329434,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2469.8264202817572,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 7097.051404640314,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4805.459637108781,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 777.1477699703448,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter\"} )",
            "value": 2029.5478437392146,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter\"} )",
            "value": 243.2483004,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "6853f62f6b5e4968779a8476a3736cecb71402d1",
          "message": "Create the critter class loader lazily and drop its ByteBuddy base class (#4344)\n\n## Problem\n\nAfter #4342 and #4343, every benchmark model loads from AOT, but\ncritter's `coldStart` was still about 12% slower than reflection. The\ncause was `CritterMapper`'s constructor, which always creates a\n`CritterClassLoader`, even when every model is pre-generated and the\nloader is never used.\n\n`CritterClassLoader` extended ByteBuddy's\n`ByteArrayClassLoader.ChildFirst`, so creating one initialized a large\npart of ByteBuddy on every cold start. Diffing the classes loaded by a\ncold `mappedMapper` call against reflection showed 79 ByteBuddy classes,\nplus JDK dynamic proxies and classes like `Process`, `Console` and\n`ResourceBundle` that nothing else needs. Constructing a\n`CritterClassLoader` alone cost about 43–60 ms in a fresh JVM.\n\n## Fix\n\n- **Lazy loader:** `CritterMapper` creates its `CritterClassLoader` the\nfirst time a model has to be generated at runtime. Copies of a mapper\nshare one holder, so they still share one loader.\n- **No ByteBuddy base class:** `CritterClassLoader` is now a plain\n`ClassLoader`. It keeps the behavior that generated code relies on:\n- registered classes and `dev.morphia.critter.*` classes load\nchild-first; everything else goes to the parent\n- a class's bytes are released once it is defined, as with ByteBuddy's\ndefault `LATENT` persistence\n- `getResource`/`getResourceAsStream` return `null` for the `.class`\nfile of a registered or child-defined class; `getResources` still lists\nthe parent's copy, as before\n\nByteBuddy is still a dependency because `ReferenceCodec` uses it for\nlazy reference proxies.\n\n`CritterClassLoaderTest` pins this contract. I wrote it first and\nconfirmed it passes against the old ByteBuddy-based loader, then against\nthe new one.\n\nOne behavior change: the no-arg-loader constructor used to pass the\n`CritterClassLoader` to `AbstractMapper` as the mapper's class loader,\nwhich is used for `Conversions`, `DiscriminatorLookup` and package\nscanning. It now passes the context class loader directly. Most classes\nresolve the same way, because the old loader delegated them to the\nparent. `dev.morphia.critter.*` classes looked up by name are the\nexception: the old loader would have returned a second copy of those,\ndistinct from the mapped class.\n\n## Results\n\nJMH `MappingBenchmark.coldStart`, 20 forks:\n\n| | master | this PR |\n|---|---:|---:|\n| critter | 224.8 ± 8.1 ms | **197.5 ± 4.5 ms** |\n| critter-runtime | 241.4 ± 5.3 ms | 229.6 ± 19.1 ms |\n| reflection | 133.8 ± 4.1 ms | n/a |\n\nOn master alone most of the critter gap is the types that still fall\nback to runtime generation, which #4342 and #4343 fix. With all three\nchanges combined, critter's `coldStart` measured 125.4 ± 1.5 ms against\n128.6 ± 2.7 ms for reflection. A control run without the lazy loader\nmeasured 160.9 ± 16.8 ms. That combined measurement used the lazy loader\nwith the old ByteBuddy base class; once the loader is never created, the\nbase class makes no difference there.\n\n## Testing\n\n- New `CritterClassLoaderTest`: passes against both the old and new\nloader.\n- Full `morphia-core` suite with regenerated AOT test models and\n`-Dmorphia.mapper=critter`: 1279 run, 0 failures. The 33 AOT-skipped\ntest entities go through runtime generation, so this exercises the new\nloader.\n- Full `morphia-core` suite with `-Dmorphia.mapper=reflection`: 1279\nrun, 0 failures.\n- critter-maven unit tests (5) and invoker ITs (3) pass.",
          "timestamp": "2026-10-04T22:40:04Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/6853f62f6b5e4968779a8476a3736cecb71402d1"
        },
        "date": 1791154343812,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 1229.829400601801,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 6588.496939157238,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 17976.654965046644,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 13803.47911208432,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 844.251595615702,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 960.913828602408,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 3337.8902801313543,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9472.897559603065,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 6927.456396312493,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 982.2851686253099,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter\"} )",
            "value": 33.177077577108626,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter\"} )",
            "value": 173.68556569999998,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "0a5812f93e9bff88cdb6e7a51d3712fec43d349d",
          "message": "Test critter's pre-generated models in pull-request CI (#4370)\n\nAdds a **Critter (AOT)** job to the Pull Requests workflow, so pull\nrequests exercise critter's pre-generated models and not only runtime\ngeneration.\n\n### Why\n- **Today's PR job:** the shared workflow behind `Build` runs a plain\n`install -Dmorphia.mapper=critter`. Core's build doesn't run\ncritter-maven, so there are no pre-generated models and critter\ngenerates every model at runtime. That's how #4366's AOT record tests\nfailed in PR CI.\n- **Push builds:** branches pushed to this repo also run `build.yml`.\nThat workflow generates the models and tests against them, so same-repo\nPRs did get AOT coverage there.\n- **Forks:** pull requests from forks only run this workflow, so they\nnever tested the AOT path.\n\n### What the job does\nIt mirrors `build.yml`:\n1. `install -DskipTests`\n2. `generate-test-models` in `core`\n3. `surefire:test -Dmorphia.mapper=critter`, which doesn't recompile.\nRecompiling would replace the woven classes and leave stale models\n(`NoSuchMethodError: __readXxx`).\n\nThe existing `Build` job is unchanged. It keeps running critter with\nruntime generation for every model, which, together with `Coverage\nReport (critter)` in `build.yml`, is the only full-suite run of the\nruntime tier. The two jobs cover the two critter tiers.\n\n### Verified locally\n- The workflow parses.\n- I ran the job's three commands as written: `BUILD SUCCESS` across the\nreactor.\n- Core ran 1313 tests against the generated models (0 failures, 16\nskipped). The `Aot*` record fixtures got their models, and their\nAOT-tier checks passed.\n\nThis PR's own CI runs the new job, since `pull_request` uses the\nworkflow from the PR.\n\nThe new job isn't a required check. If it should block merges, add\n`Critter (AOT)` to master's required status checks.\n\nFixes #4369",
          "timestamp": "2026-10-06T04:17:28Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/0a5812f93e9bff88cdb6e7a51d3712fec43d349d"
        },
        "date": 1791260731893,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 955.1584054507649,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2930.062938758631,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9149.694673389207,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 5584.103156634913,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 821.3508778010213,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 689.763194805821,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2259.0833176543674,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 7492.747877047314,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4124.784895590767,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 755.6151809365316,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter\"} )",
            "value": 32.55021344077561,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter\"} )",
            "value": 180.0587418,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "30554c3d4561e2db62ccbe1b45bd138b69a48bee",
          "message": "Cache entity codecs in a ConcurrentHashMap (plan item 2.8) (#4397)\n\nPlan item **2.8** (review D8). This completes the Phase 2 fixes. 2.5\nships with 3.8, as the plan says.\n\n## The bug\n`MorphiaCodecProvider.codecs` was a plain `HashMap`, and `get` mutated\nit on every miss. With concurrent first use of an entity type, a\n`HashMap` can be corrupted. Even when it isn't, every thread that missed\nbuilt its own `MorphiaCodec` and stored it, overwriting the others, so\ncallers ended up holding different codec instances for the same type.\n\n## Change\n`codecs` is a `ConcurrentHashMap`. On a miss, `get` builds the codec and\nstores it with `putIfAbsent`. If another thread stored one first, it\nreturns that one, so every caller shares a single codec per type.\n\nIt doesn't use `computeIfAbsent`, as the plan says: building a codec\nlooks up other codecs, and those lookups can land back in this provider,\nwhich isn't allowed inside a `ConcurrentHashMap` compute.\n\n## Tests\n`CodecHarnessTest.concurrentCodecLookupsShareOneCodec` runs under both\nmappers. For 20 fresh providers, 8 threads released together call\n`provider.get(HarnessScalars.class, registry)`, and the test asserts\nthat all of them get the same codec. It failed before the fix:\n*\"REFLECTION: every lookup should get the one cached codec\"*.\n\nThe core and Kotlin suites pass with `-Dmorphia.mapper=critter` and with\nreflection: 1483 + 5 tests, 0 failures.\n\n## Decisions to review\n1. **Recursive self-lookups now keep the inner codec.** If building a\ntype's codec recursively looks up that same type, the inner lookup\nstores its codec first, and the outer one is discarded. Before, the\nouter one overwrote it. Both are fully built codecs for the same model.",
          "timestamp": "2026-10-15T12:51:23Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/30554c3d4561e2db62ccbe1b45bd138b69a48bee"
        },
        "date": 1792078860940,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 1009.0704824831987,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2556.2406910036725,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 7193.910437956433,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4309.50681667942,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 833.0614130206525,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 56324.05140180404,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2167.178029479366,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 2367.723807093472,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 1006.7813986988222,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2572.1514996759956,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 7611.372481302718,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4678.279567734736,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 843.6120628259562,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 56010.93637931683,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2214.140613373402,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 2406.094128953828,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 513.8333955706249,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 1664.4870099866835,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 5401.9401034473985,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 2974.5914642417983,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 559.5601627343802,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 63730.10897567194,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 1420.8888326559,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 1145.582559513155,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"critter\"} )",
            "value": 18.647274639968405,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"critter\"} )",
            "value": 23.429602998457923,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"critter\"} )",
            "value": 1018.6856281234083,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"critter\"} )",
            "value": 1039.3529166113444,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"critter\"} )",
            "value": 3524.0899645482614,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 2221.5441312097387,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 10519.455270463915,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 1843.6820238311648,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 7812.944788206371,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter\"} )",
            "value": 27.504422270555885,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"critter\"} )",
            "value": 23346.278324610037,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"critter\"} )",
            "value": 15587.988871208485,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"critter\"} )",
            "value": 2276.5211403142844,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"critter\"} )",
            "value": 630.6640039586567,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter\"} )",
            "value": 143.33285210000003,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"critter\"} )",
            "value": 1914.228388,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"critter\"} )",
            "value": 291.86428420000004,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "23772b843402736f12140e1b77298b2978746561",
          "message": "Read and write primitive arrays without boxing (plan item 3.16) (#4422)\n\nPlan item **3.16** (review B7).\n\n## The problem\n`TypedArrayCodec` handles `double[]`, `int[]`, `long[]`, `float[]`,\n`short[]`, `boolean[]` and `char[]`, plus the matching wrapper arrays.\nIt treated every element as an object:\n- **encode:** `Array.get` (boxes), then the boxed component codec;\n- **decode:** the boxed component codec, `ArrayList.add`, then\n`Array.set` for every element of a new array.\n\nFor a `double[1536]` embedding, that is 1,536 boxed `Double`s each way,\nplus the list.\n\n## Change\n- **`TypedArrayCodec.of(datastore, type)`** returns a specialized codec\nfor each primitive component type. `MorphiaTypesCodecProvider` uses it.\nWrapper component types keep the general codec.\n- **Encode** writes each element directly: `writeDouble`, `writeInt32`,\n`writeInt64`, `writeBoolean`, and `writeString` for `char`. This is\nexactly what the driver's `DoubleCodec`/`IntegerCodec`/… write,\nincluding `float` → `writeDouble` and `short` → `writeInt32`.\n- **Decode** reads an element stored as the component's native BSON type\nstraight into a primitive buffer: `DOUBLE` for `double`/`float`, `INT32`\nfor `int`/`short`, `INT64` for `long`.\n- **Any other BSON type** goes through the driver's codec for that\nelement, so numeric conversions (e.g. an `INT32` in a `double[]`, or a\nwhole-number `DOUBLE` in an `int[]`) and their errors are unchanged.\nThis is the boxed fallback the plan asks for.\n- The `float` and `short` fast paths repeat the range checks\n`FloatCodec`/`ShortCodec` make, with the same exception and message.\n  - `boolean[]` reads with `readBoolean`, as `BooleanCodec` does.\n- `char[]` still goes through `CharacterCodec`, which keeps its\nstring-length checks. `Character.valueOf` caches the ASCII range, so\nthis rarely allocates.\n- **The decode buffer starts at the length of the last array that codec\ndecoded,** capped at 65,536 elements. Arrays of one property tend to\nshare a length, such as an embedding's dimension, so the usual case\nallocates the result once, with no growth and no final copy.\n- **The fast paths apply only while the component codec is the driver's\nown class.** If an application registers its own codec for `double` (or\n`Double`) through `MorphiaConfig.codecProvider`, the array codec uses\nthe original boxed path through that codec.\n- **The general (wrapper) path** loops over `Object[]` instead of\ncalling `Array.get`, and builds the typed array with `list.toArray(T[])`\ninstead of `Array.set`.\n\n## Tests\n- **New `TestPrimitiveArrayCodecs`** compares the new codecs against a\ncopy of the old algorithm, for all 14 component types, primitive and\nwrapper:\n- **decode, element by element,** across 41 BSON values:\n`INT32`/`INT64`/`DOUBLE`/`DECIMAL128` at and past every type's range\nlimits, `-0.0`, NaN, ±∞, `Float.MAX_VALUE` × 2, fractional values,\n`null`, one- and two-character and empty strings, and booleans. Each has\nto give the same array, or **fail with the same exception class and\nmessage**.\n- **decode, whole arrays,** of sizes 0, 1, 15, 16, 17, 32 and 40+, using\nevery element that decodes on its own, mixed together. The sizes run in\nsequence on one codec, so the length hint grows and shrinks.\n- **encode** of sample arrays, including the extremes, NaN, `-0.0`,\n`char` `\\u0000`/`ï¿¿`, empty and 40-element arrays, and wrapper arrays\nwith `null`s. Each one is read back and compared with what the old\nalgorithm gives.\n- **a replaced component codec:** a `double` codec that stores tenths as\n`INT64` still sees every element, both ways.\n- It **passes on `master` too**, so it pins existing behavior. Dropping\nthe `short` range check makes it fail.\n- The harness's `HarnessArrays` fixtures and the existing `*MappingTest`\nprimitive tests are unchanged and pass.\n- The core and Kotlin suites pass with `-Dmorphia.mapper=critter` and\nwith reflection: 1532 + 5 tests, 0 failures.\n\n## JMH: `CodecBenchmark` ARRAYS, `master` (`26b10d99ec6`) vs this branch\n`ArraysEntity` holds a `double[1536]` and a `String[20]`, and only the\n`double[]` uses this codec. Same machine, `-prof gc`, master and this\nbranch alternating, two rounds each.\n| variant | benchmark | master ns/op (2 runs) | this branch ns/op (2\nruns) | master B/op | this branch B/op |\n|---|---|---|---|---:|---:|\n| reflection | encode | 97,456 ± 949, 99,591 ± 2,850 | 31,332 ± 350,\n31,637 ± 452 | 135,587 | 98,721 |\n| critter | encode | 97,780 ± 949, 97,990 ± 1,983 | 32,225 ± 503, 31,722\n± 498 | 139,875 | 98,721 |\n| reflection | decode | 68,485 ± 569, 68,547 ± 1,020 | 26,585 ± 321,\n26,434 ± 238 | 74,858 | 15,569 |\n| critter | decode | 68,580 ± 806, 67,753 ± 648 | 26,426 ± 268, 28,224 ±\n2,747 | 74,849–74,858 | 15,569 |\n\n- **Encode is 3.1× faster**, matching the review's 3.0×.\n- **Decode is 2.6× faster and allocates 79% less.** The review measured\n2.4×.\n- Without the length hint, decode was 2.25× faster at 48,337 B/op.\nNearly all of that was the buffer doubling from 16 to 2,048.\n- With the hint, what's left is essentially the `double[1536]` itself\n(12.3 KB) and the strings.\n- **`MixedCodecBenchmark`,** which has no primitive arrays, is unchanged\n(within noise) and serves as a control.\n\n## Decisions to review\n1. **The length hint.** Each primitive codec remembers the length of the\nlast array it decoded and starts its next buffer at that size, capped at\n65,536 elements. The field is a plain `int`: a value raced in from\nanother thread only costs a resize. The cap bounds the up-front\nover-allocation after a single huge array to 512 KB, for a `double[]`.\nIt is per codec, which means per datastore and component type, not per\nproperty, so two `double[]` properties of different lengths will share\nit and still grow. It's a one-line change to drop if you'd rather keep\nit simpler.\n2. **The fast path is skipped when the component codec isn't the\ndriver's.** It checks the exact class (`DoubleCodec.class`, …), so a\nsubclass also gets the boxed path. I'd rather give a custom codec the\nslow path than bypass it.\n3. **I left `ArrayCodec` alone.** It handles arrays of other types, e.g.\nthe benchmark's `String[]`, and the review suggested building its typed\narray directly. 3.5 already cached its component codec, and the plan\nitem scopes this to `TypedArrayCodec`.\n4. **The wrapper decode path uses `list.toArray(T[])`.** If a custom\ncomponent codec returned the wrong element type, that now throws\n`ArrayStoreException` where `Array.set` threw\n`IllegalArgumentException`. Driver codecs can't do this.\n5. **Two quirks that already exist** turned up while writing the\nround-trip test. They're unchanged, and the test pins them:\n- a wrapper array with `null` elements (e.g. `Integer[] {1, null}`)\nencodes the nulls, but can't be read back: the component codecs reject\n`NULL`;\n- a `float[]` holding ±∞ encodes, but `FloatCodec` rejects ±∞ on decode,\nwhile NaN passes.\n\n   Both would be separate issues if you want them fixed.",
          "timestamp": "2026-10-30T15:48:43Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/23772b843402736f12140e1b77298b2978746561"
        },
        "date": 1793381397508,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 947.078325791642,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2831.429800626561,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 8453.836664337685,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4863.532998410933,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 908.6874510163424,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 29895.95154199238,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2452.6172434488517,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 2237.371293829044,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 975.9632605176545,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2890.980145224005,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9003.242262383392,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 5270.732703623536,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 927.9967848382846,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 30523.102714931174,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2526.2860039563748,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 2282.6281356161335,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 685.1398770185904,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2364.403074596305,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 7531.2018165026,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 3842.4416141843167,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 644.006808749203,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 32691.701158906562,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 1906.3374189686608,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 1396.6841810877415,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"critter\"} )",
            "value": 22.414862875188028,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"critter\"} )",
            "value": 31.299266402765106,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"critter\"} )",
            "value": 934.5497529233792,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"critter\"} )",
            "value": 1085.0523087498775,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"critter\"} )",
            "value": 4261.413501615508,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 1731.8988791822399,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 8318.074837647864,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 1384.2166067746289,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 6524.692287780124,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter\"} )",
            "value": 31.742012455636097,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"critter\"} )",
            "value": 27389.292651619144,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"critter\"} )",
            "value": 22521.817525851722,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"critter\"} )",
            "value": 1264.2849616741732,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"critter\"} )",
            "value": 230.72034941432793,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter\"} )",
            "value": 184.024883,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"critter\"} )",
            "value": 1026.3587997999998,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"critter\"} )",
            "value": 374.573891,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "f283c310889dc25853d428fc60170f92e2c5043f",
          "message": "Recognize discriminators under an entity's own key in untyped values (#4470)\n\nCloses #4463. Follow-up to D12 (#4454).\n\n## Problem\n\nAn entity can store its discriminator under its own key with\n`@Entity(discriminatorKey = \"...\")`. `ObjectCodec` decides whether a\nstored document is an entity when nothing else says what type it is. It\nlooked only for the configured key (`MorphiaConfig.discriminatorKey()`,\n`_t` by default).\n\nThe issue asked for a test of each path first, to pin down where users\nsee this:\n- **Untyped values: broken.** An entity using its own key, held by a\nfield of type `Object`, a `List<Object>` or a `Map<String, Object>`,\nloaded back as a `Document`. The same entity under the configured key\nloaded as itself. The new `untypedValuesLoadAsTheirEntity` fails on\nmaster with `expected: <OwnKeyKind(a)> but was:\n<Document{{_kind=…OwnKeyKind, name=a}}>`.\n- **Ids stored as documents: worked.** That covers references (eager,\nlazy, id-only, collections), typed id fields, and reference cycles\nthrough such an id. Both the decode session's keys and the references'\nids left them as `Document`s, so the two sides matched.\n\n## Changes\n\n- **`DiscriminatorLookup`** records, as each model registers, the key it\nstores its discriminator under (for models that use one).\n- `isKey(configuredKey, name)`: whether a string under this field name\nmay be a discriminator.\n  - `lookup(configuredKey, name, value)`:\n- Under the configured key, it resolves exactly as before, including the\n`Class.forName` fallback and its error.\n- Under an entity's own key, it resolves only if the value is exactly\nthat entity's discriminator. Anything else returns `null`, so the\ndocument stays a document.\n- **`ObjectCodec`** uses it for untyped values, so an entity under its\nown key loads as itself.\n- **Ids stored as documents are deliberately unchanged.**\n`ObjectCodec.forIds(registry)` returns an id-mode codec that honors only\nthe configured key, which is the old behavior. Every place that reads an\nid stored as a document uses it:\n- the decode session's keys (`EntityDecoder`'s\n`readKey`/`scanForId`/`peekId`);\n  - `ReferenceCodec` for a single reference's id;\n  - `ReferenceCodec` for a `DBRef`'s id;\n- an id property typed `Object` (`@Id Object id`), whose codec in the\nplan is the id-mode one. Otherwise it would decode an own-key id as an\nentity while references look it up as a `Document` (found in Copilot's\nsecond round).\n\n`ReferenceCodec.processId` and `EntityDecoder.decodesAsEntity` are as on\nmaster.\n\n### Why ids are left alone (Copilot review, round 1)\n\nThe first version of this PR applied own keys to ids too, changing\n`ObjectCodec`, `processId` and `decodesAsEntity` together, as the issue\nproposed. Copilot found three ways that broke own-key ids that worked on\nmaster, and new tests confirm each:\n- **A lifecycle owner** whose id has its own key isn't registered in the\nsession before its properties decode. `decodesAsEntity` says not to read\nan entity id ahead, so an eager self-reference overflows the stack.\n- **An `idOnly` `Map` of references** looks up its values as raw\n`Document`s while the session now holds id instances, so the cycle never\ncloses and the stack overflows.\n- **An owner without callbacks** reads its own-key id as an entity once\nfor the session key and again for the property, so the id's `@PostLoad`\nruns twice.\n\nUsers don't hit any of these with own-key ids, because those ids stay\n`Document`s on both sides. Converting them to entities brings in\nproblems that ids under the **configured** key already have on master. A\nscratch run of the same three scenarios with `_t`-keyed compound ids\ngives two stack overflows and a doubled `@PostLoad`. Making ids entities\nsafely needs a design for session keys: probably a canonical raw form on\nboth sides, plus callback-safe key reads. That's a separate change,\ntracked in #4471, #4472 and #4473 for the configured-key cases. So this\nPR fixes the user-visible bug, untyped values, and keeps every id path\nexactly as it was.\n\n## ⚠️ Behavior change\n\n**An untyped document can now load as an entity where it used to stay a\n`Document`.** A stored document in an untyped place (`Object`,\n`List<Object>`, `Map<String, Object>`) loads as an entity when two\nthings hold:\n- it has a string field named like an entity's own discriminator key;\n- the field holds **exactly that entity's discriminator**.\n\nFor example, with `@Entity(discriminatorKey = \"type\", discriminator =\n\"PNG\") class Png` (`dev.morphia.test.models.Png` is one), `{type: \"PNG\",\n…}` in a `Map<String, Object>` now loads as a `Png`. That's the fix for\nentities stored there. But plain user data that happens to match would\nalso convert.\n\nThe match is deliberately narrow:\n- the field must be a key some mapped entity declares;\n- the value must be a string;\n- it must be that same entity's discriminator.\n\nA `type: \"something else\"`, a `type: 5`, or another entity's\ndiscriminator under that key leaves the document alone.\n`documentsWithAFieldOfThatNameStayDocuments` covers those three. This is\nthe rule that has always applied to the configured `_t` key, extended to\nthe keys entities choose themselves.\n\nMigration note: MorphiaOrg/morphia-docs#26 adds \"Untyped values and\nper-entity discriminator keys\" to the 3.0 migration page. It describes\nthis change and what to check. #4459 (the 2.5 → 3.0 audit) should pick\nit up too.\n\n## Decisions to review\n\n- **Untyped values only, not ids.** See above.\n- **Exact match on the entity's own key**, not any registered\ndiscriminator under any declared key. That keeps plain documents from\nconverting as far as possible.\n- **The configured key is unchanged,** in untyped values and in ids.\n- **Field order decides between two discriminators.** An untyped\ndocument holding discriminators under both the configured key and an\nentity's own key resolves by whichever comes first, as `ObjectCodec`\nalready stopped at the first match. Morphia never writes both for one\nentity. Since ids only honor the configured key, this can't make the\nsession and references disagree. Scanning every field to prefer one key\nwasn't worth the cost for documents Morphia doesn't produce.\n- **`ObjectCodec.forIds` only swaps in the id mode for Morphia's own\n`ObjectCodec`** (exact class). A subclass a provider supplies keeps\nreading ids its own way (found in Copilot's round 3; covered by\n`aCustomObjectCodecStillReadsIds`).\n- **Known limitation, not addressed:** an id typed as a `Map` whose\nvalues include an entity with its own key. The id property decodes such\na nested value as the entity, while the session key keeps it a\n`Document`. Copilot's third-round summary hinted at this without an\ninline comment, and I haven't reproduced it. It falls under the\nsession-key design in #4471–#4473.\n- **Only mapped entities are recognized.** A key is known once its\nentity is mapped, which with the default package scan happens at\nstartup.\n\n## Testing\n\nNew `TestOwnDiscriminatorKey`, run against a server:\n- `untypedValuesLoadAsTheirEntity`: an `Object` field, a `List<Object>`\nand a `Map<String, Object>` holding an entity with its own key, plus a\ncontrol under the configured key. **Fails on master.**\n- `documentsWithAFieldOfThatNameStayDocuments`: plain documents whose\n`_kind` field holds a non-discriminator string, a number, or another\nentity's discriminator all stay `Document`s.\n- Guards that own-key ids behave as on master. Each passes on master,\nand the last three **failed on this PR's first version**:\n- `referencesByAnIdWithItsOwnKey`: eager, lazy, id-only and list\nreferences;\n  - `anIdWithItsOwnKeyLoadsOnce`: a lifecycle owner;\n- `aLifecycleOwnerClosesACycleThroughAnIdWithItsOwnKey`: the review's\nfirst case;\n  - `aMapOfReferencesClosesACycleThroughAnIdWithItsOwnKey`: its second;\n  - `anIdWithItsOwnKeyLoadsOnceUnderAnOwnerWithoutCallbacks`: its third.\n- `aListOfReferencesFindsTargetsWithAnUntypedIdWithItsOwnKey`: an `@Id\nObject id` holding an own-key id, referenced only from a list. It came\nback empty on the second version (Copilot's round 2) and is found now.\n\nAlso:\n- `TestDiscriminatorResolution.discriminatorsResolveUnderTheirOwnKey`\nunit-tests `isKey`/`lookup` on both mappers.\n- `TestReferences.compoundIdsWithTheirOwnDiscriminatorKeyCloseCycles`\n(from D12) still passes.\n- Core suite passes with `critter` and with `reflection` (2,535 tests,\n16 skipped).\n- Local Copilot CLI review, one round, on the first version. It found a\nmisplaced javadoc and a configured-key inconsistency in\n`decodesAsEntity`. Both are moot now, since that code is back to master.\n\n## Benchmarks\n\nNone run. The changed code only runs for untyped values, meaning\n`ObjectCodec` on a document, and none of the benchmark models has one.\nIn the scan, the added cost per field is one `getCurrentBsonType()`,\nplus a set lookup for string fields. Id reads take the same path as\nbefore.",
          "timestamp": "2026-11-11T06:35:39Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/f283c310889dc25853d428fc60170f92e2c5043f"
        },
        "date": 1794380529350,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 890.7429855952448,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2837.4273388482475,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9592.786800246628,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4649.632536016718,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 557.3440016103054,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 34899.481492559506,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2829.165873948858,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 2573.1484829621063,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 899.0844472142963,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2756.485864235166,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9746.511504946238,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4625.043897642229,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 572.4866394181032,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 35180.62721229023,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2905.0930690930554,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 2628.0574180300437,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 836.2849608530848,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2581.8965641780205,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9069.02609606779,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4642.95799378135,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 409.6224079425679,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 40451.58419020888,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2488.5883796939775,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 1857.674848523374,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"critter\"} )",
            "value": 4.3532665998736,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"critter\"} )",
            "value": 8.785050567211037,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"critter\"} )",
            "value": 871.9193386263496,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"critter\"} )",
            "value": 1327.668140169852,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"critter\"} )",
            "value": 5388.279946748999,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 2223.3917545638847,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 10184.681114503179,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 932.2289658236126,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2922.132317943676,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 1778.1987950041669,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 7581.1119469101395,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 885.2395017317228,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2744.793477230795,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter\"} )",
            "value": 20.805329402444816,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"critter\"} )",
            "value": 26965.14634719324,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"critter\"} )",
            "value": 25563.5200669159,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"critter\"} )",
            "value": 1550.1088261559048,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"critter\"} )",
            "value": 351.07896007026443,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter\"} )",
            "value": 231.5385824,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"critter\"} )",
            "value": 1198.4044594000002,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"critter\"} )",
            "value": 395.6936668,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "1ff98e9eebbf9bf81680d3cd4c85f33167ba20c6",
          "message": "Fix stack overflow for recursive embedded types (#4510)\n\nPreserve the supplied codec registry during property resolution to retain cycle detection for recursive entity types. Add persistence regressions for recursive fields, containers, refresh, and custom codecs.",
          "timestamp": "2026-11-16T02:59:33Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/1ff98e9eebbf9bf81680d3cd4c85f33167ba20c6"
        },
        "date": 1794845360527,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 892.5259863821474,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2824.8610714483375,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9526.552547369914,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4616.548650042978,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 561.6270309113263,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 34707.46486966691,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2833.1078761498457,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 2524.8272141096295,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"critter\"} )",
            "value": 27016.93346963831,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"critter\"} )",
            "value": 25563.582360693545,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"critter\"} )",
            "value": 1578.3832688192585,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"critter\"} )",
            "value": 353.82585738283086,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter\"} )",
            "value": 238.19722190000002,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"critter\"} )",
            "value": 1210.559607,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"critter\"} )",
            "value": 407.2315574,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 890.4019570878778,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2960.652711584784,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9785.348079098756,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4816.3404868610505,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 597.6096681152461,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 35144.6413523515,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2884.0791173905136,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 2620.584569692121,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 843.952091534446,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2599.925598352471,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9116.898485359881,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4701.136727047581,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 413.9052302749335,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 39891.58040953832,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2528.9500014004675,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 1833.5107263958323,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"critter\"} )",
            "value": 4.352365732017927,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"critter\"} )",
            "value": 8.843568021856312,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"critter\"} )",
            "value": 882.413492960433,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"critter\"} )",
            "value": 1357.700138165457,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"critter\"} )",
            "value": 5167.741370862802,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 2179.1822640133473,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 10052.155099587702,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 961.6013828297056,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2952.444079556802,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 1760.7768376367062,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 7301.692022138945,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 888.625389052761,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2795.3332481745415,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter\"} )",
            "value": 23.139145862564977,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "05759847a73c50e1444ddf35163c18b7bea69bcf",
          "message": "Bump the maven-dependencies group with 4 updates (#4526)\n\nBumps the maven-dependencies group with 4 updates:\n[ch.qos.logback:logback-classic](https://github.com/qos-ch/logback),\n[com.google.devtools.ksp:symbol-processing-api](https://github.com/google/ksp),\n[com.google.devtools.ksp:symbol-processing-common-deps](https://github.com/google/ksp)\nand\n[com.google.devtools.ksp:symbol-processing-aa-embeddable](https://github.com/google/ksp).\n\nUpdates `ch.qos.logback:logback-classic` from 1.6.6 to 1.6.7\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/qos-ch/logback/releases\">ch.qos.logback:logback-classic's\nreleases</a>.</em></p>\n<blockquote>\n<h2>Logback 1.6.7</h2>\n<h3>2026-11-14 Release of logback version 1.6.7</h3>\n<p>• The <code>scan</code> attribute of the\n<code>&lt;propertiesConfigurator&gt;</code> element, when\nset, now takes precedence over the <code>scan</code> attribute of the\ntop-level\n<code>&lt;configuration&gt;</code> element, in both directions. With\n<code>scan=&quot;true&quot;</code>\nand scanning disabled or unset at the top level, the properties file\nis now actually monitored for changes. Previously, the file was\nadded to the watch list but no task ever polled it. The scan period\nis taken from the <code>scanPeriod</code> attribute of the top-level\n<code>&lt;configuration&gt;</code> element. Conversely, with\n<code>scan=&quot;false&quot;</code> and\n<code>scan=&quot;true&quot;</code> at the top level, the properties file\nis no longer\nmonitored. Moreover, when several <code>&lt;propertiesConfigurator\nscan=&quot;true&quot;&gt;</code>\nelements are present without top-level scanning, all\nthe referenced files are now monitored. Previously, only the last\none was.</p>\n<p>• Two new modules, <em>logback-core-kryo</em> and\n<em>logback-classic-kryo</em>,\nencode and decode logging events with the <a\nhref=\"https://github.com/EsotericSoftware/kryo\">Kryo 5</a> serialization\nlibrary. <a\nhref=\"https://logback.qos.ch/manual/encoders.html#KryoEncoder\"><code>KryoEncoder</code></a>\nwrites <code>ILoggingEvent</code> instances into a\n<strong>compact binary stream</strong> and <code>KryoDecoder</code>\nreads them back. The\nstream is framed so that the decoder can recover from torn or truncated\nwrites, for example after a crash of the application. Both modules\nrequire\nJava 11 and ship with JPMS module descriptors, namely\n<code>ch.qos.logback.core.kryo</code>\nand <code>ch.qos.logback.classic.kryo</code>. See also the relevant\ndocumentation\nat <a\nhref=\"https://logback.qos.ch/manual/encoders.html#KryoEncoder\">KryoEncoder</a>.</p>\n<p>• Added the <code>toBooleanObject(String, Boolean)</code> and\n<code>toLopsidedBooleanObject</code> methods in\n<code>OptionHelper</code>.</p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/8b97c2fe6dd0cfa298ed3c3dd4f500b942edc63a\"><code>8b97c2f</code></a>\nprepare release 1.6.7</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/764b5c4613edb5c0920edfe7ff2404e778a0f0f1\"><code>764b5c4</code></a>\nkryo: use stream version 0 when KryoEncoder is set to Vanilla</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/b29f465c3a3556a5432f768d37f0b3e127ebc076\"><code>b29f465</code></a>\nkryo: remove V3 and V4 stream encodings, keep Vanilla and V5</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/d42c0129146630e7d769e9e8c2261bb68a3c04e5\"><code>d42c012</code></a>\nadd logback-core-kryo and logback-classic-kryo modules</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/069d51abcd1549d67f872fc4a315d585150f8087\"><code>069d51a</code></a>\nremove dead code</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/271d89c82cad3b7071478610e2eea2013210bb6f\"><code>271d89c</code></a>\nhonor scan attribute of &lt;propertiesConfigurator&gt; in both\ndirections</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/b01611fddfddd58ff03be4c7cbb37b6a4e7161ca\"><code>b01611f</code></a>\nadded a OptionHelper#toLopsidedBooleanObject</li>\n<li><a\nhref=\"https://github.com/qos-ch/logback/commit/b1267ca05580340c42aca3985394de6e9093e501\"><code>b1267ca</code></a>\nstart work on 1.6.7-SNAPSHOT</li>\n<li>See full diff in <a\nhref=\"https://github.com/qos-ch/logback/compare/v_1.6.6...v_1.6.7\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\nUpdates `com.google.devtools.ksp:symbol-processing-api` from 2.3.13 to\n2.3.14\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/google/ksp/releases\">com.google.devtools.ksp:symbol-processing-api's\nreleases</a>.</em></p>\n<blockquote>\n<h2>2.3.14</h2>\n<h2>What's Changed</h2>\n<ul>\n<li>Fix regression where library symbols disappear during concurrent KSP\nexecutions by only clearing <code>CoreJarFileSystem</code> handlers when\nno executions are active (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3286\">#3286</a>)</li>\n<li>Implement support for reading Kotlin context parameters\n(<code>KSContextParameter</code>) on functions and properties (<a\nhref=\"https://redirect.github.com/google/ksp/issues/2472\">#2472</a>)</li>\n<li>Skip Analysis API session initialization and processor execution\nduring incremental builds when no files are dirty (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3256\">#3256</a>)</li>\n<li>Fix <code>KSAnnotation.arguments</code> omitting <code>vararg</code>\nparameters when annotations are applied with no arguments (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3271\">#3271</a>)</li>\n<li>Align <code>KSAnnotation.defaultArguments</code> behavior across\nplatforms by not falling back to an empty array on JVM for array-typed\nparameters (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3233\">#3233</a>)</li>\n<li>Fix <code>KSTypeParameter.bounds</code> returning expanded type\narguments when the bound is a type alias (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3242\">#3242</a>)</li>\n</ul>\n<h2>Contributors</h2>\n<ul>\n<li>Thanks to <a\nhref=\"https://github.com/bcorso\"><code>@​bcorso</code></a>, and everyone\nwho reported bugs and participated in discussions!</li>\n</ul>\n<p>Full Changelog: <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">https://github.com/google/ksp/compare/2.3.13...2.3.14</a></p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a3aaaabae570bae3ab4b84e72385483a14ab4498\"><code>a3aaaab</code></a>\nSkip impossible URL lookups</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/ff066aca3f800b8c61cf4d1c20e7518035247e84\"><code>ff066ac</code></a>\nFix potential memory leak in jarFileSystem cleanup</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/e2c1e7696a8f5380cb5515b4d6f2caa9774e269c\"><code>e2c1e76</code></a>\nRun LibraryRootsSearchScopeTest in isolation</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/5f7fa4b24ab2ebab15bff5da6a34539208108d50\"><code>5f7fa4b</code></a>\nKSP2: Skip the Analysis API session and processors when an incremental\nrun ha...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/3ed0f292b993796fa2df68d4ca203257e73dc015\"><code>3ed0f29</code></a>\nRead KSBackingFieldJavaImpl.modifiers from the underlying Java field\nPSI</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a2133e0e320a6bc14fac30010dfdfb5e2d342b12\"><code>a2133e0</code></a>\nRemove wrong leading '/' in test path</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/9dd67d2487a51853d68850ee2d21f17b6158ab9e\"><code>9dd67d2</code></a>\nFix KSBackingField case in <code>mapToJvmSignatureInternal</code></li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/cae8dc66f772085e12901220b17375c631ab7388\"><code>cae8dc6</code></a>\nAdd helper functions in KSPUnitTestSuite</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/96ba20a67aaf8fe7c48ceab49c2fb620c59aee71\"><code>96ba20a</code></a>\nOnly clear CoreJarFileSystem handlers when no concurrent KSP executions\nare a...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/1b0eb9a1696a03ffa53429078af9700b483dad47\"><code>1b0eb9a</code></a>\nFallback to root URLs in LibraryRootsSearchScope when JAR handlers are\ncleared</li>\n<li>Additional commits viewable in <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\nUpdates `com.google.devtools.ksp:symbol-processing-common-deps` from\n2.3.13 to 2.3.14\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/google/ksp/releases\">com.google.devtools.ksp:symbol-processing-common-deps's\nreleases</a>.</em></p>\n<blockquote>\n<h2>2.3.14</h2>\n<h2>What's Changed</h2>\n<ul>\n<li>Fix regression where library symbols disappear during concurrent KSP\nexecutions by only clearing <code>CoreJarFileSystem</code> handlers when\nno executions are active (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3286\">#3286</a>)</li>\n<li>Implement support for reading Kotlin context parameters\n(<code>KSContextParameter</code>) on functions and properties (<a\nhref=\"https://redirect.github.com/google/ksp/issues/2472\">#2472</a>)</li>\n<li>Skip Analysis API session initialization and processor execution\nduring incremental builds when no files are dirty (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3256\">#3256</a>)</li>\n<li>Fix <code>KSAnnotation.arguments</code> omitting <code>vararg</code>\nparameters when annotations are applied with no arguments (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3271\">#3271</a>)</li>\n<li>Align <code>KSAnnotation.defaultArguments</code> behavior across\nplatforms by not falling back to an empty array on JVM for array-typed\nparameters (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3233\">#3233</a>)</li>\n<li>Fix <code>KSTypeParameter.bounds</code> returning expanded type\narguments when the bound is a type alias (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3242\">#3242</a>)</li>\n</ul>\n<h2>Contributors</h2>\n<ul>\n<li>Thanks to <a\nhref=\"https://github.com/bcorso\"><code>@​bcorso</code></a>, and everyone\nwho reported bugs and participated in discussions!</li>\n</ul>\n<p>Full Changelog: <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">https://github.com/google/ksp/compare/2.3.13...2.3.14</a></p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a3aaaabae570bae3ab4b84e72385483a14ab4498\"><code>a3aaaab</code></a>\nSkip impossible URL lookups</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/ff066aca3f800b8c61cf4d1c20e7518035247e84\"><code>ff066ac</code></a>\nFix potential memory leak in jarFileSystem cleanup</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/e2c1e7696a8f5380cb5515b4d6f2caa9774e269c\"><code>e2c1e76</code></a>\nRun LibraryRootsSearchScopeTest in isolation</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/5f7fa4b24ab2ebab15bff5da6a34539208108d50\"><code>5f7fa4b</code></a>\nKSP2: Skip the Analysis API session and processors when an incremental\nrun ha...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/3ed0f292b993796fa2df68d4ca203257e73dc015\"><code>3ed0f29</code></a>\nRead KSBackingFieldJavaImpl.modifiers from the underlying Java field\nPSI</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a2133e0e320a6bc14fac30010dfdfb5e2d342b12\"><code>a2133e0</code></a>\nRemove wrong leading '/' in test path</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/9dd67d2487a51853d68850ee2d21f17b6158ab9e\"><code>9dd67d2</code></a>\nFix KSBackingField case in <code>mapToJvmSignatureInternal</code></li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/cae8dc66f772085e12901220b17375c631ab7388\"><code>cae8dc6</code></a>\nAdd helper functions in KSPUnitTestSuite</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/96ba20a67aaf8fe7c48ceab49c2fb620c59aee71\"><code>96ba20a</code></a>\nOnly clear CoreJarFileSystem handlers when no concurrent KSP executions\nare a...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/1b0eb9a1696a03ffa53429078af9700b483dad47\"><code>1b0eb9a</code></a>\nFallback to root URLs in LibraryRootsSearchScope when JAR handlers are\ncleared</li>\n<li>Additional commits viewable in <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\nUpdates `com.google.devtools.ksp:symbol-processing-aa-embeddable` from\n2.3.13 to 2.3.14\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/google/ksp/releases\">com.google.devtools.ksp:symbol-processing-aa-embeddable's\nreleases</a>.</em></p>\n<blockquote>\n<h2>2.3.14</h2>\n<h2>What's Changed</h2>\n<ul>\n<li>Fix regression where library symbols disappear during concurrent KSP\nexecutions by only clearing <code>CoreJarFileSystem</code> handlers when\nno executions are active (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3286\">#3286</a>)</li>\n<li>Implement support for reading Kotlin context parameters\n(<code>KSContextParameter</code>) on functions and properties (<a\nhref=\"https://redirect.github.com/google/ksp/issues/2472\">#2472</a>)</li>\n<li>Skip Analysis API session initialization and processor execution\nduring incremental builds when no files are dirty (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3256\">#3256</a>)</li>\n<li>Fix <code>KSAnnotation.arguments</code> omitting <code>vararg</code>\nparameters when annotations are applied with no arguments (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3271\">#3271</a>)</li>\n<li>Align <code>KSAnnotation.defaultArguments</code> behavior across\nplatforms by not falling back to an empty array on JVM for array-typed\nparameters (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3233\">#3233</a>)</li>\n<li>Fix <code>KSTypeParameter.bounds</code> returning expanded type\narguments when the bound is a type alias (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3242\">#3242</a>)</li>\n</ul>\n<h2>Contributors</h2>\n<ul>\n<li>Thanks to <a\nhref=\"https://github.com/bcorso\"><code>@​bcorso</code></a>, and everyone\nwho reported bugs and participated in discussions!</li>\n</ul>\n<p>Full Changelog: <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">https://github.com/google/ksp/compare/2.3.13...2.3.14</a></p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a3aaaabae570bae3ab4b84e72385483a14ab4498\"><code>a3aaaab</code></a>\nSkip impossible URL lookups</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/ff066aca3f800b8c61cf4d1c20e7518035247e84\"><code>ff066ac</code></a>\nFix potential memory leak in jarFileSystem cleanup</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/e2c1e7696a8f5380cb5515b4d6f2caa9774e269c\"><code>e2c1e76</code></a>\nRun LibraryRootsSearchScopeTest in isolation</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/5f7fa4b24ab2ebab15bff5da6a34539208108d50\"><code>5f7fa4b</code></a>\nKSP2: Skip the Analysis API session and processors when an incremental\nrun ha...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/3ed0f292b993796fa2df68d4ca203257e73dc015\"><code>3ed0f29</code></a>\nRead KSBackingFieldJavaImpl.modifiers from the underlying Java field\nPSI</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a2133e0e320a6bc14fac30010dfdfb5e2d342b12\"><code>a2133e0</code></a>\nRemove wrong leading '/' in test path</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/9dd67d2487a51853d68850ee2d21f17b6158ab9e\"><code>9dd67d2</code></a>\nFix KSBackingField case in <code>mapToJvmSignatureInternal</code></li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/cae8dc66f772085e12901220b17375c631ab7388\"><code>cae8dc6</code></a>\nAdd helper functions in KSPUnitTestSuite</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/96ba20a67aaf8fe7c48ceab49c2fb620c59aee71\"><code>96ba20a</code></a>\nOnly clear CoreJarFileSystem handlers when no concurrent KSP executions\nare a...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/1b0eb9a1696a03ffa53429078af9700b483dad47\"><code>1b0eb9a</code></a>\nFallback to root URLs in LibraryRootsSearchScope when JAR handlers are\ncleared</li>\n<li>Additional commits viewable in <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\nUpdates `com.google.devtools.ksp:symbol-processing-common-deps` from\n2.3.13 to 2.3.14\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/google/ksp/releases\">com.google.devtools.ksp:symbol-processing-common-deps's\nreleases</a>.</em></p>\n<blockquote>\n<h2>2.3.14</h2>\n<h2>What's Changed</h2>\n<ul>\n<li>Fix regression where library symbols disappear during concurrent KSP\nexecutions by only clearing <code>CoreJarFileSystem</code> handlers when\nno executions are active (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3286\">#3286</a>)</li>\n<li>Implement support for reading Kotlin context parameters\n(<code>KSContextParameter</code>) on functions and properties (<a\nhref=\"https://redirect.github.com/google/ksp/issues/2472\">#2472</a>)</li>\n<li>Skip Analysis API session initialization and processor execution\nduring incremental builds when no files are dirty (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3256\">#3256</a>)</li>\n<li>Fix <code>KSAnnotation.arguments</code> omitting <code>vararg</code>\nparameters when annotations are applied with no arguments (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3271\">#3271</a>)</li>\n<li>Align <code>KSAnnotation.defaultArguments</code> behavior across\nplatforms by not falling back to an empty array on JVM for array-typed\nparameters (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3233\">#3233</a>)</li>\n<li>Fix <code>KSTypeParameter.bounds</code> returning expanded type\narguments when the bound is a type alias (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3242\">#3242</a>)</li>\n</ul>\n<h2>Contributors</h2>\n<ul>\n<li>Thanks to <a\nhref=\"https://github.com/bcorso\"><code>@​bcorso</code></a>, and everyone\nwho reported bugs and participated in discussions!</li>\n</ul>\n<p>Full Changelog: <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">https://github.com/google/ksp/compare/2.3.13...2.3.14</a></p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a3aaaabae570bae3ab4b84e72385483a14ab4498\"><code>a3aaaab</code></a>\nSkip impossible URL lookups</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/ff066aca3f800b8c61cf4d1c20e7518035247e84\"><code>ff066ac</code></a>\nFix potential memory leak in jarFileSystem cleanup</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/e2c1e7696a8f5380cb5515b4d6f2caa9774e269c\"><code>e2c1e76</code></a>\nRun LibraryRootsSearchScopeTest in isolation</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/5f7fa4b24ab2ebab15bff5da6a34539208108d50\"><code>5f7fa4b</code></a>\nKSP2: Skip the Analysis API session and processors when an incremental\nrun ha...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/3ed0f292b993796fa2df68d4ca203257e73dc015\"><code>3ed0f29</code></a>\nRead KSBackingFieldJavaImpl.modifiers from the underlying Java field\nPSI</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a2133e0e320a6bc14fac30010dfdfb5e2d342b12\"><code>a2133e0</code></a>\nRemove wrong leading '/' in test path</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/9dd67d2487a51853d68850ee2d21f17b6158ab9e\"><code>9dd67d2</code></a>\nFix KSBackingField case in <code>mapToJvmSignatureInternal</code></li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/cae8dc66f772085e12901220b17375c631ab7388\"><code>cae8dc6</code></a>\nAdd helper functions in KSPUnitTestSuite</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/96ba20a67aaf8fe7c48ceab49c2fb620c59aee71\"><code>96ba20a</code></a>\nOnly clear CoreJarFileSystem handlers when no concurrent KSP executions\nare a...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/1b0eb9a1696a03ffa53429078af9700b483dad47\"><code>1b0eb9a</code></a>\nFallback to root URLs in LibraryRootsSearchScope when JAR handlers are\ncleared</li>\n<li>Additional commits viewable in <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\nUpdates `com.google.devtools.ksp:symbol-processing-aa-embeddable` from\n2.3.13 to 2.3.14\n<details>\n<summary>Release notes</summary>\n<p><em>Sourced from <a\nhref=\"https://github.com/google/ksp/releases\">com.google.devtools.ksp:symbol-processing-aa-embeddable's\nreleases</a>.</em></p>\n<blockquote>\n<h2>2.3.14</h2>\n<h2>What's Changed</h2>\n<ul>\n<li>Fix regression where library symbols disappear during concurrent KSP\nexecutions by only clearing <code>CoreJarFileSystem</code> handlers when\nno executions are active (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3286\">#3286</a>)</li>\n<li>Implement support for reading Kotlin context parameters\n(<code>KSContextParameter</code>) on functions and properties (<a\nhref=\"https://redirect.github.com/google/ksp/issues/2472\">#2472</a>)</li>\n<li>Skip Analysis API session initialization and processor execution\nduring incremental builds when no files are dirty (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3256\">#3256</a>)</li>\n<li>Fix <code>KSAnnotation.arguments</code> omitting <code>vararg</code>\nparameters when annotations are applied with no arguments (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3271\">#3271</a>)</li>\n<li>Align <code>KSAnnotation.defaultArguments</code> behavior across\nplatforms by not falling back to an empty array on JVM for array-typed\nparameters (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3233\">#3233</a>)</li>\n<li>Fix <code>KSTypeParameter.bounds</code> returning expanded type\narguments when the bound is a type alias (<a\nhref=\"https://redirect.github.com/google/ksp/issues/3242\">#3242</a>)</li>\n</ul>\n<h2>Contributors</h2>\n<ul>\n<li>Thanks to <a\nhref=\"https://github.com/bcorso\"><code>@​bcorso</code></a>, and everyone\nwho reported bugs and participated in discussions!</li>\n</ul>\n<p>Full Changelog: <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">https://github.com/google/ksp/compare/2.3.13...2.3.14</a></p>\n</blockquote>\n</details>\n<details>\n<summary>Commits</summary>\n<ul>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a3aaaabae570bae3ab4b84e72385483a14ab4498\"><code>a3aaaab</code></a>\nSkip impossible URL lookups</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/ff066aca3f800b8c61cf4d1c20e7518035247e84\"><code>ff066ac</code></a>\nFix potential memory leak in jarFileSystem cleanup</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/e2c1e7696a8f5380cb5515b4d6f2caa9774e269c\"><code>e2c1e76</code></a>\nRun LibraryRootsSearchScopeTest in isolation</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/5f7fa4b24ab2ebab15bff5da6a34539208108d50\"><code>5f7fa4b</code></a>\nKSP2: Skip the Analysis API session and processors when an incremental\nrun ha...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/3ed0f292b993796fa2df68d4ca203257e73dc015\"><code>3ed0f29</code></a>\nRead KSBackingFieldJavaImpl.modifiers from the underlying Java field\nPSI</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/a2133e0e320a6bc14fac30010dfdfb5e2d342b12\"><code>a2133e0</code></a>\nRemove wrong leading '/' in test path</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/9dd67d2487a51853d68850ee2d21f17b6158ab9e\"><code>9dd67d2</code></a>\nFix KSBackingField case in <code>mapToJvmSignatureInternal</code></li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/cae8dc66f772085e12901220b17375c631ab7388\"><code>cae8dc6</code></a>\nAdd helper functions in KSPUnitTestSuite</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/96ba20a67aaf8fe7c48ceab49c2fb620c59aee71\"><code>96ba20a</code></a>\nOnly clear CoreJarFileSystem handlers when no concurrent KSP executions\nare a...</li>\n<li><a\nhref=\"https://github.com/google/ksp/commit/1b0eb9a1696a03ffa53429078af9700b483dad47\"><code>1b0eb9a</code></a>\nFallback to root URLs in LibraryRootsSearchScope when JAR handlers are\ncleared</li>\n<li>Additional commits viewable in <a\nhref=\"https://github.com/google/ksp/compare/2.3.13...2.3.14\">compare\nview</a></li>\n</ul>\n</details>\n<br />\n\n\nDependabot will resolve any conflicts with this PR as long as you don't\nalter it yourself. You can also trigger a rebase manually by commenting\n`@dependabot rebase`.\n\n[//]: # (dependabot-automerge-start)\n[//]: # (dependabot-automerge-end)\n\n---\n\n<details>\n<summary>Dependabot commands and options</summary>\n<br />\n\nYou can trigger Dependabot actions by commenting on this PR:\n- `@dependabot rebase` will rebase this PR\n- `@dependabot recreate` will recreate this PR, overwriting any edits\nthat have been made to it\n- `@dependabot show <dependency name> ignore conditions` will show all\nof the ignore conditions of the specified dependency\n- `@dependabot ignore <dependency name> major version` will close this\ngroup update PR and stop Dependabot creating any more for the specific\ndependency's major version (unless you unignore this specific\ndependency's major version or upgrade to it yourself)\n- `@dependabot ignore <dependency name> minor version` will close this\ngroup update PR and stop Dependabot creating any more for the specific\ndependency's minor version (unless you unignore this specific\ndependency's minor version or upgrade to it yourself)\n- `@dependabot ignore <dependency name>` will close this group update PR\nand stop Dependabot creating any more for the specific dependency\n(unless you unignore this specific dependency or upgrade to it yourself)\n- `@dependabot unignore <dependency name>` will remove all of the ignore\nconditions of the specified dependency\n- `@dependabot unignore <dependency name> <ignore condition>` will\nremove the ignore condition of the specified dependency and ignore\nconditions\n\n\n</details>\n\nSigned-off-by: dependabot[bot] <support@github.com>\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2026-11-20T10:00:13Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/05759847a73c50e1444ddf35163c18b7bea69bcf"
        },
        "date": 1795459359853,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 880.1314440064665,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2801.029505769473,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9588.500454088198,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4590.159214731915,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 560.2931410862434,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 34846.43565182713,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2807.0582099725993,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 2543.8519089411598,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"critter\"} )",
            "value": 26979.883425647873,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"critter\"} )",
            "value": 25457.496378316086,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"critter\"} )",
            "value": 1557.227127751463,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"critter\"} )",
            "value": 353.220350390278,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter\"} )",
            "value": 241.4475668,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"critter\"} )",
            "value": 1182.1643542,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"critter\"} )",
            "value": 411.440834,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 902.3383851192319,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2834.795518717614,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9869.750314164026,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4699.430750056646,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 570.9690716797171,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 35225.36929330698,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2887.319597550787,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 2596.1392559412106,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 852.7891453701072,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2613.8522138508533,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 9253.889226596217,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter\"} )",
            "value": 4712.183728046959,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter\"} )",
            "value": 412.23736338071456,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"critter\"} )",
            "value": 39646.73916327786,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter\"} )",
            "value": 2504.5261847495613,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter\"} )",
            "value": 1852.965739438982,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"critter\"} )",
            "value": 4.378959306821434,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"critter\"} )",
            "value": 8.746041868768446,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"critter\"} )",
            "value": 888.7609449338593,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"critter\"} )",
            "value": 1326.237176517361,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"critter\"} )",
            "value": 5085.8473280767,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 2146.488404846892,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 10128.06869756325,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 962.6558063148143,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2928.5409525317796,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 1751.8762020643355,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 7315.393016536812,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"critter\"} )",
            "value": 889.1029465408411,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"critter\"} )",
            "value": 2770.97380480126,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter\"} )",
            "value": 22.813319930186914,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          }
        ]
      }
    ],
    "Mapper: critter-runtime": [
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "cc32cd505db2989982b1bfe1004f58698c4fa5fd",
          "message": "Fix critter decode building a new Conversions per property (#4338)\n\n## Problem\nCritter-mapped entities decoded about 10× slower than reflective ones\nand allocated about 15× more per decode. The new mapper benchmarks\n(#4336) surfaced this.\n\nJFR showed almost all the time and allocation inside\n`Conversions.<init>`, called from `PropertyModel.setValue`:\n\n```java\nConversions c = conversions != null ? conversions\n        : new Conversions(Thread.currentThread().getContextClassLoader());\n```\n\nReflective property models get `.conversions(mapper.getConversions())`\nfrom `FieldDiscovery` / `MethodDiscovery`. Critter's generated property\nmodels never had it set. Every property set during decode therefore\nrebuilt the whole conversions registry: a few dozen map entries and\nlambdas.\n\n## Fix\n`CritterEntityModel.configureProperties(mapper)` now also sets\n`property.conversions(mapper.getConversions())`. That method already\nruns at the end of every generated entity-model constructor, AOT and\nruntime alike. Existing AOT output picks up the fix without\nregeneration.\n\n## Results\nIn-memory JMH benchmarks from #4336, JDK 21, short warmed runs:\n\n| decode | reflection | critter before | critter after |\n|---|---:|---:|---:|\n| SIMPLE | 2.6 µs, 3,080 B/op | 25 µs, 46,089 B/op | 2.4 µs, 3,144 B/op\n|\n| NESTED | 12.1 µs, 28,944 B/op | 62 µs, 136,562 B/op | 12.0 µs, 28,864\nB/op |\n\nCOLLECTIONS, POLYMORPHIC and LIFECYCLE also now match reflection for\nboth critter AOT and critter runtime. Encode was never affected.\n\n## Testing\n- Full `morphia-core` suite (Testcontainers MongoDB 8.0.0):\n- `-Dmorphia.mapper=critter`: 1,274 tests, 0 failures, 0 errors, 17\nskipped.\n- `-Dmorphia.mapper=reflection`: 1,274 tests, 0 failures, 0 errors, 17\nskipped.\n- `spotless:apply` makes no changes.\n\n🤖 Generated with [Claude Code](https://claude.com/claude-code)\n\nhttps://claude.ai/code/session_01MExceZHKD4HFFCDa6tXn8g\n\nCo-authored-by: Claude <noreply@anthropic.com>",
          "timestamp": "2026-10-04T01:02:07Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/cc32cd505db2989982b1bfe1004f58698c4fa5fd"
        },
        "date": 1791082006160,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1245.2818646332578,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 6472.332507584977,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 17403.702092192365,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 13558.710451084326,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 866.6011288172426,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1104.7001470862845,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 3653.6658509806407,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 10342.193538222624,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 7579.294062522237,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1041.6356439462925,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter-runtime\"} )",
            "value": 7083.738606944719,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter-runtime\"} )",
            "value": 358.8783896,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "6853f62f6b5e4968779a8476a3736cecb71402d1",
          "message": "Create the critter class loader lazily and drop its ByteBuddy base class (#4344)\n\n## Problem\n\nAfter #4342 and #4343, every benchmark model loads from AOT, but\ncritter's `coldStart` was still about 12% slower than reflection. The\ncause was `CritterMapper`'s constructor, which always creates a\n`CritterClassLoader`, even when every model is pre-generated and the\nloader is never used.\n\n`CritterClassLoader` extended ByteBuddy's\n`ByteArrayClassLoader.ChildFirst`, so creating one initialized a large\npart of ByteBuddy on every cold start. Diffing the classes loaded by a\ncold `mappedMapper` call against reflection showed 79 ByteBuddy classes,\nplus JDK dynamic proxies and classes like `Process`, `Console` and\n`ResourceBundle` that nothing else needs. Constructing a\n`CritterClassLoader` alone cost about 43–60 ms in a fresh JVM.\n\n## Fix\n\n- **Lazy loader:** `CritterMapper` creates its `CritterClassLoader` the\nfirst time a model has to be generated at runtime. Copies of a mapper\nshare one holder, so they still share one loader.\n- **No ByteBuddy base class:** `CritterClassLoader` is now a plain\n`ClassLoader`. It keeps the behavior that generated code relies on:\n- registered classes and `dev.morphia.critter.*` classes load\nchild-first; everything else goes to the parent\n- a class's bytes are released once it is defined, as with ByteBuddy's\ndefault `LATENT` persistence\n- `getResource`/`getResourceAsStream` return `null` for the `.class`\nfile of a registered or child-defined class; `getResources` still lists\nthe parent's copy, as before\n\nByteBuddy is still a dependency because `ReferenceCodec` uses it for\nlazy reference proxies.\n\n`CritterClassLoaderTest` pins this contract. I wrote it first and\nconfirmed it passes against the old ByteBuddy-based loader, then against\nthe new one.\n\nOne behavior change: the no-arg-loader constructor used to pass the\n`CritterClassLoader` to `AbstractMapper` as the mapper's class loader,\nwhich is used for `Conversions`, `DiscriminatorLookup` and package\nscanning. It now passes the context class loader directly. Most classes\nresolve the same way, because the old loader delegated them to the\nparent. `dev.morphia.critter.*` classes looked up by name are the\nexception: the old loader would have returned a second copy of those,\ndistinct from the mapped class.\n\n## Results\n\nJMH `MappingBenchmark.coldStart`, 20 forks:\n\n| | master | this PR |\n|---|---:|---:|\n| critter | 224.8 ± 8.1 ms | **197.5 ± 4.5 ms** |\n| critter-runtime | 241.4 ± 5.3 ms | 229.6 ± 19.1 ms |\n| reflection | 133.8 ± 4.1 ms | n/a |\n\nOn master alone most of the critter gap is the types that still fall\nback to runtime generation, which #4342 and #4343 fix. With all three\nchanges combined, critter's `coldStart` measured 125.4 ± 1.5 ms against\n128.6 ± 2.7 ms for reflection. A control run without the lazy loader\nmeasured 160.9 ± 16.8 ms. That combined measurement used the lazy loader\nwith the old ByteBuddy base class; once the loader is never created, the\nbase class makes no difference there.\n\n## Testing\n\n- New `CritterClassLoaderTest`: passes against both the old and new\nloader.\n- Full `morphia-core` suite with regenerated AOT test models and\n`-Dmorphia.mapper=critter`: 1279 run, 0 failures. The 33 AOT-skipped\ntest entities go through runtime generation, so this exercises the new\nloader.\n- Full `morphia-core` suite with `-Dmorphia.mapper=reflection`: 1279\nrun, 0 failures.\n- critter-maven unit tests (5) and invoker ITs (3) pass.",
          "timestamp": "2026-10-04T22:40:04Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/6853f62f6b5e4968779a8476a3736cecb71402d1"
        },
        "date": 1791154346729,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1567.962037265558,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 8400.736806377225,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 23460.01807724649,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 17803.493606502056,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1127.848002716454,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1461.2407636551025,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 4820.078869889587,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 13425.47704019034,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 9568.615921956314,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1377.8867109747514,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter-runtime\"} )",
            "value": 9701.788385792488,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter-runtime\"} )",
            "value": 386.6828208,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "0a5812f93e9bff88cdb6e7a51d3712fec43d349d",
          "message": "Test critter's pre-generated models in pull-request CI (#4370)\n\nAdds a **Critter (AOT)** job to the Pull Requests workflow, so pull\nrequests exercise critter's pre-generated models and not only runtime\ngeneration.\n\n### Why\n- **Today's PR job:** the shared workflow behind `Build` runs a plain\n`install -Dmorphia.mapper=critter`. Core's build doesn't run\ncritter-maven, so there are no pre-generated models and critter\ngenerates every model at runtime. That's how #4366's AOT record tests\nfailed in PR CI.\n- **Push builds:** branches pushed to this repo also run `build.yml`.\nThat workflow generates the models and tests against them, so same-repo\nPRs did get AOT coverage there.\n- **Forks:** pull requests from forks only run this workflow, so they\nnever tested the AOT path.\n\n### What the job does\nIt mirrors `build.yml`:\n1. `install -DskipTests`\n2. `generate-test-models` in `core`\n3. `surefire:test -Dmorphia.mapper=critter`, which doesn't recompile.\nRecompiling would replace the woven classes and leave stale models\n(`NoSuchMethodError: __readXxx`).\n\nThe existing `Build` job is unchanged. It keeps running critter with\nruntime generation for every model, which, together with `Coverage\nReport (critter)` in `build.yml`, is the only full-suite run of the\nruntime tier. The two jobs cover the two critter tiers.\n\n### Verified locally\n- The workflow parses.\n- I ran the job's three commands as written: `BUILD SUCCESS` across the\nreactor.\n- Core ran 1313 tests against the generated models (0 failures, 16\nskipped). The `Aot*` record fixtures got their models, and their\nAOT-tier checks passed.\n\nThis PR's own CI runs the new job, since `pull_request` uses the\nworkflow from the PR.\n\nThe new job isn't a required check. If it should block merges, add\n`Critter (AOT)` to master's required status checks.\n\nFixes #4369",
          "timestamp": "2026-10-06T04:17:28Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/0a5812f93e9bff88cdb6e7a51d3712fec43d349d"
        },
        "date": 1791260734379,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 954.2226837998511,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2950.533610558183,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 9377.329274477046,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 5561.694384658154,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 833.3074444294255,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 692.8405682185492,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2214.47252243984,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 7470.6925852208715,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 4170.98948312613,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 742.65455088121,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter-runtime\"} )",
            "value": 78.62200435176638,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter-runtime\"} )",
            "value": 300.22875509999994,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "30554c3d4561e2db62ccbe1b45bd138b69a48bee",
          "message": "Cache entity codecs in a ConcurrentHashMap (plan item 2.8) (#4397)\n\nPlan item **2.8** (review D8). This completes the Phase 2 fixes. 2.5\nships with 3.8, as the plan says.\n\n## The bug\n`MorphiaCodecProvider.codecs` was a plain `HashMap`, and `get` mutated\nit on every miss. With concurrent first use of an entity type, a\n`HashMap` can be corrupted. Even when it isn't, every thread that missed\nbuilt its own `MorphiaCodec` and stored it, overwriting the others, so\ncallers ended up holding different codec instances for the same type.\n\n## Change\n`codecs` is a `ConcurrentHashMap`. On a miss, `get` builds the codec and\nstores it with `putIfAbsent`. If another thread stored one first, it\nreturns that one, so every caller shares a single codec per type.\n\nIt doesn't use `computeIfAbsent`, as the plan says: building a codec\nlooks up other codecs, and those lookups can land back in this provider,\nwhich isn't allowed inside a `ConcurrentHashMap` compute.\n\n## Tests\n`CodecHarnessTest.concurrentCodecLookupsShareOneCodec` runs under both\nmappers. For 20 fresh providers, 8 threads released together call\n`provider.get(HarnessScalars.class, registry)`, and the test asserts\nthat all of them get the same codec. It failed before the fix:\n*\"REFLECTION: every lookup should get the one cached codec\"*.\n\nThe core and Kotlin suites pass with `-Dmorphia.mapper=critter` and with\nreflection: 1483 + 5 tests, 0 failures.\n\n## Decisions to review\n1. **Recursive self-lookups now keep the inner codec.** If building a\ntype's codec recursively looks up that same type, the inner lookup\nstores its codec first, and the outer one is discarded. Before, the\nouter one overwrote it. Both are fully built codecs for the same model.",
          "timestamp": "2026-10-15T12:51:23Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/30554c3d4561e2db62ccbe1b45bd138b69a48bee"
        },
        "date": 1792078863625,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1837.2378983231154,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 4569.443355604357,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 12799.786244548108,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 8428.817366835669,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1545.648118007789,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 90737.54398127156,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 3813.8755352595867,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 4212.9161553069125,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1919.9496518426263,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 4724.754210281652,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 13420.800669397713,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 8892.034725914988,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1647.696308049313,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 91819.28914249348,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 4199.315730993203,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 4539.062830359281,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1016.4134905660233,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 3137.4721771073146,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 10448.498737980779,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 6038.473380854714,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1108.5785774666651,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 122310.4836081893,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 2800.277093768608,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 2274.3373014376048,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"critter-runtime\"} )",
            "value": 29.5080158271066,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"critter-runtime\"} )",
            "value": 39.50121471248686,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"critter-runtime\"} )",
            "value": 1752.105263023243,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"critter-runtime\"} )",
            "value": 1756.2416349931893,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"critter-runtime\"} )",
            "value": 5626.095155375931,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 4227.477631513981,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 19210.111973315536,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 3443.8508072343075,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 14717.395654842814,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter-runtime\"} )",
            "value": 109.82730153342516,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"critter-runtime\"} )",
            "value": 41033.560966878315,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"critter-runtime\"} )",
            "value": 30661.28349449789,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"critter-runtime\"} )",
            "value": 5537.5176329639535,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"critter-runtime\"} )",
            "value": 1698.1424753382573,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter-runtime\"} )",
            "value": 402.01232820000007,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"critter-runtime\"} )",
            "value": 3458.4995254,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"critter-runtime\"} )",
            "value": 613.3430730000001,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "23772b843402736f12140e1b77298b2978746561",
          "message": "Read and write primitive arrays without boxing (plan item 3.16) (#4422)\n\nPlan item **3.16** (review B7).\n\n## The problem\n`TypedArrayCodec` handles `double[]`, `int[]`, `long[]`, `float[]`,\n`short[]`, `boolean[]` and `char[]`, plus the matching wrapper arrays.\nIt treated every element as an object:\n- **encode:** `Array.get` (boxes), then the boxed component codec;\n- **decode:** the boxed component codec, `ArrayList.add`, then\n`Array.set` for every element of a new array.\n\nFor a `double[1536]` embedding, that is 1,536 boxed `Double`s each way,\nplus the list.\n\n## Change\n- **`TypedArrayCodec.of(datastore, type)`** returns a specialized codec\nfor each primitive component type. `MorphiaTypesCodecProvider` uses it.\nWrapper component types keep the general codec.\n- **Encode** writes each element directly: `writeDouble`, `writeInt32`,\n`writeInt64`, `writeBoolean`, and `writeString` for `char`. This is\nexactly what the driver's `DoubleCodec`/`IntegerCodec`/… write,\nincluding `float` → `writeDouble` and `short` → `writeInt32`.\n- **Decode** reads an element stored as the component's native BSON type\nstraight into a primitive buffer: `DOUBLE` for `double`/`float`, `INT32`\nfor `int`/`short`, `INT64` for `long`.\n- **Any other BSON type** goes through the driver's codec for that\nelement, so numeric conversions (e.g. an `INT32` in a `double[]`, or a\nwhole-number `DOUBLE` in an `int[]`) and their errors are unchanged.\nThis is the boxed fallback the plan asks for.\n- The `float` and `short` fast paths repeat the range checks\n`FloatCodec`/`ShortCodec` make, with the same exception and message.\n  - `boolean[]` reads with `readBoolean`, as `BooleanCodec` does.\n- `char[]` still goes through `CharacterCodec`, which keeps its\nstring-length checks. `Character.valueOf` caches the ASCII range, so\nthis rarely allocates.\n- **The decode buffer starts at the length of the last array that codec\ndecoded,** capped at 65,536 elements. Arrays of one property tend to\nshare a length, such as an embedding's dimension, so the usual case\nallocates the result once, with no growth and no final copy.\n- **The fast paths apply only while the component codec is the driver's\nown class.** If an application registers its own codec for `double` (or\n`Double`) through `MorphiaConfig.codecProvider`, the array codec uses\nthe original boxed path through that codec.\n- **The general (wrapper) path** loops over `Object[]` instead of\ncalling `Array.get`, and builds the typed array with `list.toArray(T[])`\ninstead of `Array.set`.\n\n## Tests\n- **New `TestPrimitiveArrayCodecs`** compares the new codecs against a\ncopy of the old algorithm, for all 14 component types, primitive and\nwrapper:\n- **decode, element by element,** across 41 BSON values:\n`INT32`/`INT64`/`DOUBLE`/`DECIMAL128` at and past every type's range\nlimits, `-0.0`, NaN, ±∞, `Float.MAX_VALUE` × 2, fractional values,\n`null`, one- and two-character and empty strings, and booleans. Each has\nto give the same array, or **fail with the same exception class and\nmessage**.\n- **decode, whole arrays,** of sizes 0, 1, 15, 16, 17, 32 and 40+, using\nevery element that decodes on its own, mixed together. The sizes run in\nsequence on one codec, so the length hint grows and shrinks.\n- **encode** of sample arrays, including the extremes, NaN, `-0.0`,\n`char` `\\u0000`/`ï¿¿`, empty and 40-element arrays, and wrapper arrays\nwith `null`s. Each one is read back and compared with what the old\nalgorithm gives.\n- **a replaced component codec:** a `double` codec that stores tenths as\n`INT64` still sees every element, both ways.\n- It **passes on `master` too**, so it pins existing behavior. Dropping\nthe `short` range check makes it fail.\n- The harness's `HarnessArrays` fixtures and the existing `*MappingTest`\nprimitive tests are unchanged and pass.\n- The core and Kotlin suites pass with `-Dmorphia.mapper=critter` and\nwith reflection: 1532 + 5 tests, 0 failures.\n\n## JMH: `CodecBenchmark` ARRAYS, `master` (`26b10d99ec6`) vs this branch\n`ArraysEntity` holds a `double[1536]` and a `String[20]`, and only the\n`double[]` uses this codec. Same machine, `-prof gc`, master and this\nbranch alternating, two rounds each.\n| variant | benchmark | master ns/op (2 runs) | this branch ns/op (2\nruns) | master B/op | this branch B/op |\n|---|---|---|---|---:|---:|\n| reflection | encode | 97,456 ± 949, 99,591 ± 2,850 | 31,332 ± 350,\n31,637 ± 452 | 135,587 | 98,721 |\n| critter | encode | 97,780 ± 949, 97,990 ± 1,983 | 32,225 ± 503, 31,722\n± 498 | 139,875 | 98,721 |\n| reflection | decode | 68,485 ± 569, 68,547 ± 1,020 | 26,585 ± 321,\n26,434 ± 238 | 74,858 | 15,569 |\n| critter | decode | 68,580 ± 806, 67,753 ± 648 | 26,426 ± 268, 28,224 ±\n2,747 | 74,849–74,858 | 15,569 |\n\n- **Encode is 3.1× faster**, matching the review's 3.0×.\n- **Decode is 2.6× faster and allocates 79% less.** The review measured\n2.4×.\n- Without the length hint, decode was 2.25× faster at 48,337 B/op.\nNearly all of that was the buffer doubling from 16 to 2,048.\n- With the hint, what's left is essentially the `double[1536]` itself\n(12.3 KB) and the strings.\n- **`MixedCodecBenchmark`,** which has no primitive arrays, is unchanged\n(within noise) and serves as a control.\n\n## Decisions to review\n1. **The length hint.** Each primitive codec remembers the length of the\nlast array it decoded and starts its next buffer at that size, capped at\n65,536 elements. The field is a plain `int`: a value raced in from\nanother thread only costs a resize. The cap bounds the up-front\nover-allocation after a single huge array to 512 KB, for a `double[]`.\nIt is per codec, which means per datastore and component type, not per\nproperty, so two `double[]` properties of different lengths will share\nit and still grow. It's a one-line change to drop if you'd rather keep\nit simpler.\n2. **The fast path is skipped when the component codec isn't the\ndriver's.** It checks the exact class (`DoubleCodec.class`, …), so a\nsubclass also gets the boxed path. I'd rather give a custom codec the\nslow path than bypass it.\n3. **I left `ArrayCodec` alone.** It handles arrays of other types, e.g.\nthe benchmark's `String[]`, and the review suggested building its typed\narray directly. 3.5 already cached its component codec, and the plan\nitem scopes this to `TypedArrayCodec`.\n4. **The wrapper decode path uses `list.toArray(T[])`.** If a custom\ncomponent codec returned the wrong element type, that now throws\n`ArrayStoreException` where `Array.set` threw\n`IllegalArgumentException`. Driver codecs can't do this.\n5. **Two quirks that already exist** turned up while writing the\nround-trip test. They're unchanged, and the test pins them:\n- a wrapper array with `null` elements (e.g. `Integer[] {1, null}`)\nencodes the nulls, but can't be read back: the component codecs reject\n`NULL`;\n- a `float[]` holding ±∞ encodes, but `FloatCodec` rejects ±∞ on decode,\nwhile NaN passes.\n\n   Both would be separate issues if you want them fixed.",
          "timestamp": "2026-10-30T15:48:43Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/23772b843402736f12140e1b77298b2978746561"
        },
        "date": 1793381400137,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1225.0792038052743,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 3704.5485011511846,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 11022.692656607118,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 6554.2327669268625,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1242.1096225012031,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 35971.43759292569,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 3210.267747414305,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 2932.9733107227994,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1250.8664910438051,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 3831.5741970835584,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 11567.508765165674,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 6773.894841294694,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1264.4757103051447,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 36877.07392317376,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 3348.711533686707,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 3031.23973770282,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 887.6767613534914,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2754.0601664759242,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 9317.229245130882,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 4812.49431256078,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 901.2685540445685,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 39713.41699347918,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 2539.0269693538403,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 1870.6275581327582,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"critter-runtime\"} )",
            "value": 30.640146548409724,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"critter-runtime\"} )",
            "value": 46.329400091107985,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"critter-runtime\"} )",
            "value": 1220.005632849364,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"critter-runtime\"} )",
            "value": 1374.0989240095973,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"critter-runtime\"} )",
            "value": 5586.342798165436,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 2193.661968846179,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 10826.02887897678,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1774.254563600247,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 7721.562200262799,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter-runtime\"} )",
            "value": 124.00678162411307,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"critter-runtime\"} )",
            "value": 35939.04783608165,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"critter-runtime\"} )",
            "value": 27109.276689765313,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"critter-runtime\"} )",
            "value": 1614.8972564933072,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"critter-runtime\"} )",
            "value": 295.8643126406687,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter-runtime\"} )",
            "value": 421.6293242,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"critter-runtime\"} )",
            "value": 1469.4868836,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"critter-runtime\"} )",
            "value": 635.9830942,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "f283c310889dc25853d428fc60170f92e2c5043f",
          "message": "Recognize discriminators under an entity's own key in untyped values (#4470)\n\nCloses #4463. Follow-up to D12 (#4454).\n\n## Problem\n\nAn entity can store its discriminator under its own key with\n`@Entity(discriminatorKey = \"...\")`. `ObjectCodec` decides whether a\nstored document is an entity when nothing else says what type it is. It\nlooked only for the configured key (`MorphiaConfig.discriminatorKey()`,\n`_t` by default).\n\nThe issue asked for a test of each path first, to pin down where users\nsee this:\n- **Untyped values: broken.** An entity using its own key, held by a\nfield of type `Object`, a `List<Object>` or a `Map<String, Object>`,\nloaded back as a `Document`. The same entity under the configured key\nloaded as itself. The new `untypedValuesLoadAsTheirEntity` fails on\nmaster with `expected: <OwnKeyKind(a)> but was:\n<Document{{_kind=…OwnKeyKind, name=a}}>`.\n- **Ids stored as documents: worked.** That covers references (eager,\nlazy, id-only, collections), typed id fields, and reference cycles\nthrough such an id. Both the decode session's keys and the references'\nids left them as `Document`s, so the two sides matched.\n\n## Changes\n\n- **`DiscriminatorLookup`** records, as each model registers, the key it\nstores its discriminator under (for models that use one).\n- `isKey(configuredKey, name)`: whether a string under this field name\nmay be a discriminator.\n  - `lookup(configuredKey, name, value)`:\n- Under the configured key, it resolves exactly as before, including the\n`Class.forName` fallback and its error.\n- Under an entity's own key, it resolves only if the value is exactly\nthat entity's discriminator. Anything else returns `null`, so the\ndocument stays a document.\n- **`ObjectCodec`** uses it for untyped values, so an entity under its\nown key loads as itself.\n- **Ids stored as documents are deliberately unchanged.**\n`ObjectCodec.forIds(registry)` returns an id-mode codec that honors only\nthe configured key, which is the old behavior. Every place that reads an\nid stored as a document uses it:\n- the decode session's keys (`EntityDecoder`'s\n`readKey`/`scanForId`/`peekId`);\n  - `ReferenceCodec` for a single reference's id;\n  - `ReferenceCodec` for a `DBRef`'s id;\n- an id property typed `Object` (`@Id Object id`), whose codec in the\nplan is the id-mode one. Otherwise it would decode an own-key id as an\nentity while references look it up as a `Document` (found in Copilot's\nsecond round).\n\n`ReferenceCodec.processId` and `EntityDecoder.decodesAsEntity` are as on\nmaster.\n\n### Why ids are left alone (Copilot review, round 1)\n\nThe first version of this PR applied own keys to ids too, changing\n`ObjectCodec`, `processId` and `decodesAsEntity` together, as the issue\nproposed. Copilot found three ways that broke own-key ids that worked on\nmaster, and new tests confirm each:\n- **A lifecycle owner** whose id has its own key isn't registered in the\nsession before its properties decode. `decodesAsEntity` says not to read\nan entity id ahead, so an eager self-reference overflows the stack.\n- **An `idOnly` `Map` of references** looks up its values as raw\n`Document`s while the session now holds id instances, so the cycle never\ncloses and the stack overflows.\n- **An owner without callbacks** reads its own-key id as an entity once\nfor the session key and again for the property, so the id's `@PostLoad`\nruns twice.\n\nUsers don't hit any of these with own-key ids, because those ids stay\n`Document`s on both sides. Converting them to entities brings in\nproblems that ids under the **configured** key already have on master. A\nscratch run of the same three scenarios with `_t`-keyed compound ids\ngives two stack overflows and a doubled `@PostLoad`. Making ids entities\nsafely needs a design for session keys: probably a canonical raw form on\nboth sides, plus callback-safe key reads. That's a separate change,\ntracked in #4471, #4472 and #4473 for the configured-key cases. So this\nPR fixes the user-visible bug, untyped values, and keeps every id path\nexactly as it was.\n\n## ⚠️ Behavior change\n\n**An untyped document can now load as an entity where it used to stay a\n`Document`.** A stored document in an untyped place (`Object`,\n`List<Object>`, `Map<String, Object>`) loads as an entity when two\nthings hold:\n- it has a string field named like an entity's own discriminator key;\n- the field holds **exactly that entity's discriminator**.\n\nFor example, with `@Entity(discriminatorKey = \"type\", discriminator =\n\"PNG\") class Png` (`dev.morphia.test.models.Png` is one), `{type: \"PNG\",\n…}` in a `Map<String, Object>` now loads as a `Png`. That's the fix for\nentities stored there. But plain user data that happens to match would\nalso convert.\n\nThe match is deliberately narrow:\n- the field must be a key some mapped entity declares;\n- the value must be a string;\n- it must be that same entity's discriminator.\n\nA `type: \"something else\"`, a `type: 5`, or another entity's\ndiscriminator under that key leaves the document alone.\n`documentsWithAFieldOfThatNameStayDocuments` covers those three. This is\nthe rule that has always applied to the configured `_t` key, extended to\nthe keys entities choose themselves.\n\nMigration note: MorphiaOrg/morphia-docs#26 adds \"Untyped values and\nper-entity discriminator keys\" to the 3.0 migration page. It describes\nthis change and what to check. #4459 (the 2.5 → 3.0 audit) should pick\nit up too.\n\n## Decisions to review\n\n- **Untyped values only, not ids.** See above.\n- **Exact match on the entity's own key**, not any registered\ndiscriminator under any declared key. That keeps plain documents from\nconverting as far as possible.\n- **The configured key is unchanged,** in untyped values and in ids.\n- **Field order decides between two discriminators.** An untyped\ndocument holding discriminators under both the configured key and an\nentity's own key resolves by whichever comes first, as `ObjectCodec`\nalready stopped at the first match. Morphia never writes both for one\nentity. Since ids only honor the configured key, this can't make the\nsession and references disagree. Scanning every field to prefer one key\nwasn't worth the cost for documents Morphia doesn't produce.\n- **`ObjectCodec.forIds` only swaps in the id mode for Morphia's own\n`ObjectCodec`** (exact class). A subclass a provider supplies keeps\nreading ids its own way (found in Copilot's round 3; covered by\n`aCustomObjectCodecStillReadsIds`).\n- **Known limitation, not addressed:** an id typed as a `Map` whose\nvalues include an entity with its own key. The id property decodes such\na nested value as the entity, while the session key keeps it a\n`Document`. Copilot's third-round summary hinted at this without an\ninline comment, and I haven't reproduced it. It falls under the\nsession-key design in #4471–#4473.\n- **Only mapped entities are recognized.** A key is known once its\nentity is mapped, which with the default package scan happens at\nstartup.\n\n## Testing\n\nNew `TestOwnDiscriminatorKey`, run against a server:\n- `untypedValuesLoadAsTheirEntity`: an `Object` field, a `List<Object>`\nand a `Map<String, Object>` holding an entity with its own key, plus a\ncontrol under the configured key. **Fails on master.**\n- `documentsWithAFieldOfThatNameStayDocuments`: plain documents whose\n`_kind` field holds a non-discriminator string, a number, or another\nentity's discriminator all stay `Document`s.\n- Guards that own-key ids behave as on master. Each passes on master,\nand the last three **failed on this PR's first version**:\n- `referencesByAnIdWithItsOwnKey`: eager, lazy, id-only and list\nreferences;\n  - `anIdWithItsOwnKeyLoadsOnce`: a lifecycle owner;\n- `aLifecycleOwnerClosesACycleThroughAnIdWithItsOwnKey`: the review's\nfirst case;\n  - `aMapOfReferencesClosesACycleThroughAnIdWithItsOwnKey`: its second;\n  - `anIdWithItsOwnKeyLoadsOnceUnderAnOwnerWithoutCallbacks`: its third.\n- `aListOfReferencesFindsTargetsWithAnUntypedIdWithItsOwnKey`: an `@Id\nObject id` holding an own-key id, referenced only from a list. It came\nback empty on the second version (Copilot's round 2) and is found now.\n\nAlso:\n- `TestDiscriminatorResolution.discriminatorsResolveUnderTheirOwnKey`\nunit-tests `isKey`/`lookup` on both mappers.\n- `TestReferences.compoundIdsWithTheirOwnDiscriminatorKeyCloseCycles`\n(from D12) still passes.\n- Core suite passes with `critter` and with `reflection` (2,535 tests,\n16 skipped).\n- Local Copilot CLI review, one round, on the first version. It found a\nmisplaced javadoc and a configured-key inconsistency in\n`decodesAsEntity`. Both are moot now, since that code is back to master.\n\n## Benchmarks\n\nNone run. The changed code only runs for untyped values, meaning\n`ObjectCodec` on a document, and none of the benchmark models has one.\nIn the scan, the added cost per field is one `getCurrentBsonType()`,\nplus a set lookup for string fields. Id reads take the same path as\nbefore.",
          "timestamp": "2026-11-11T06:35:39Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/f283c310889dc25853d428fc60170f92e2c5043f"
        },
        "date": 1794380531693,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 889.68367059666,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2753.3280655723047,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 9379.673468149542,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 4801.347611763704,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 564.1730969428005,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 35757.540491239204,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 2871.4451382963516,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 2574.912362048078,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 912.8936023864833,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2797.579155603314,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 9694.550437449698,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 4719.962143500463,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 573.9650019949331,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 35372.75086658867,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 2880.9860524156325,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 2648.1671001641052,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 853.9989446461966,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2599.9080691366157,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 9188.412918400778,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 4778.329060226185,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 409.6171020739084,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 39159.39539752535,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 2531.2872520474793,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 1867.0867143546243,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 3\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"critter-runtime\"} )",
            "value": 4.451148725412292,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"critter-runtime\"} )",
            "value": 9.25052968876648,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"critter-runtime\"} )",
            "value": 888.5672935823548,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"critter-runtime\"} )",
            "value": 1332.0871457799428,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"critter-runtime\"} )",
            "value": 5123.147804179708,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 2187.8713253149795,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 10511.823357184394,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 965.2913448340607,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2953.8924114735933,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1771.255434396783,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 7412.142774558315,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 878.1504464940678,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2738.598209377041,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter-runtime\"} )",
            "value": 22.850375831754473,
            "unit": "us/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"critter-runtime\"} )",
            "value": 27389.19252104619,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"critter-runtime\"} )",
            "value": 25270.967730602628,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"critter-runtime\"} )",
            "value": 1605.9763666761773,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"critter-runtime\"} )",
            "value": 352.1414716028829,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter-runtime\"} )",
            "value": 392.4473878,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"critter-runtime\"} )",
            "value": 1443.741403,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"critter-runtime\"} )",
            "value": 589.894557,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Justin Lee",
            "username": "evanchooly",
            "email": "evanchooly@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "1ff98e9eebbf9bf81680d3cd4c85f33167ba20c6",
          "message": "Fix stack overflow for recursive embedded types (#4510)\n\nPreserve the supplied codec registry during property resolution to retain cycle detection for recursive entity types. Add persistence regressions for recursive fields, containers, refresh, and custom codecs.",
          "timestamp": "2026-11-16T02:59:33Z",
          "url": "https://github.com/MorphiaOrg/morphia/commit/1ff98e9eebbf9bf81680d3cd4c85f33167ba20c6"
        },
        "date": 1794845363719,
        "tool": "jmh",
        "benches": [
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 909.5532064706916,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2831.0012517992072,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 9484.868690611718,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 4645.906213342215,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 561.8752621285405,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 34609.82652544345,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 2863.2259763672164,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 2585.465162661881,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.decode ( {\"variant\":\"critter-runtime\"} )",
            "value": 27476.277253469925,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MixedCodecBenchmark.encode ( {\"variant\":\"critter-runtime\"} )",
            "value": 25327.856483324093,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.queryDocument ( {\"variant\":\"critter-runtime\"} )",
            "value": 1572.978307631446,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.QueryBenchmark.translatedPath ( {\"variant\":\"critter-runtime\"} )",
            "value": 349.94147617040204,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.coldStart ( {\"variant\":\"critter-runtime\"} )",
            "value": 407.5532876,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 10\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanEverything ( {\"variant\":\"critter-runtime\"} )",
            "value": 1370.8078456,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.scanModelsPackage ( {\"variant\":\"critter-runtime\"} )",
            "value": 597.0626296,
            "unit": "ms/op",
            "extra": "iterations: 1\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 908.5491440589534,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2851.9749490616578,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 9829.364705179682,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 4731.410785001389,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 585.2665146097002,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 35086.55566960098,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 2937.5722108326977,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.decodeInCursor ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 2623.2290326475318,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 842.9238734494143,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2595.845352002846,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 9199.702749097754,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"POLYMORPHIC\",\"variant\":\"critter-runtime\"} )",
            "value": 4830.829395073359,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LIFECYCLE\",\"variant\":\"critter-runtime\"} )",
            "value": 408.88891210541755,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"ARRAYS\",\"variant\":\"critter-runtime\"} )",
            "value": 39228.12753128487,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"CONCRETE_COLLECTIONS\",\"variant\":\"critter-runtime\"} )",
            "value": 2487.8146052564844,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.CodecBenchmark.encode ( {\"model\":\"LAZY_REFERENCE\",\"variant\":\"critter-runtime\"} )",
            "value": 1847.4981151490304,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copy ( {\"variant\":\"critter-runtime\"} )",
            "value": 4.370437381009315,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DatastoreBenchmark.copyAndDecode ( {\"variant\":\"critter-runtime\"} )",
            "value": 9.042203873182576,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"AS_WRITTEN\",\"variant\":\"critter-runtime\"} )",
            "value": 885.7984189060063,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"NO_DISCRIMINATOR\",\"variant\":\"critter-runtime\"} )",
            "value": 1332.0906773680363,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.DocumentShapesBenchmark.decode ( {\"shape\":\"SCHEMA_DRIFT\",\"variant\":\"critter-runtime\"} )",
            "value": 5162.98065095551,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 2203.1601479543906,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 10502.991894600518,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 973.7474768587155,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.decode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2976.1129900593373,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 1774.6028106408069,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"DOCUMENT\",\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 7487.098973610314,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"SIMPLE\",\"variant\":\"critter-runtime\"} )",
            "value": 881.7473842694538,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.ListenerBenchmark.encode ( {\"listener\":\"ENTITY\",\"model\":\"NESTED\",\"variant\":\"critter-runtime\"} )",
            "value": 2734.0137279787114,
            "unit": "ns/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          },
          {
            "name": "dev.morphia.benchmarks.MappingBenchmark.warmMapping ( {\"variant\":\"critter-runtime\"} )",
            "value": 30.776622906874408,
            "unit": "us/op",
            "extra": "iterations: 3\nforks: 5\nthreads: 1"
          }
        ]
      }
    ]
  }
}