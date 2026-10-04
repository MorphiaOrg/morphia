window.BENCHMARK_DATA = {
  "lastUpdate": 1791154347499,
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
      }
    ]
  }
}