window.BENCHMARK_DATA = {
  "lastUpdate": 1791082003875,
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
      }
    ]
  }
}