## Mapper profiling results

Lower is better for every benchmark here. Ratios compare each variant against `reflection`: below 1.00× means faster. The legs ran on different runners, so read small differences with care.

| Benchmark | Params | reflection | critter | critter-runtime | critter / reflection | critter-runtime / reflection |
|---|---|---:|---:|---:|---:|---:|
| `CodecBenchmark.decode` | ARRAYS | 71,513 ± 1,194 ns/op | 72,481 ± 887 ns/op | 68,073 ± 618 ns/op | 1.01× | 0.95× |
| `CodecBenchmark.decode` | COLLECTIONS | 10,269 ± 101 ns/op | 10,303 ± 218 ns/op | 10,013 ± 107 ns/op | 1.00× | 0.98× |
| `CodecBenchmark.decode` | CONCRETE_COLLECTIONS | 3,072 ± 130 ns/op | 3,223 ± 54.38 ns/op | 2,961 ± 61.34 ns/op | 1.05× | 0.96× |
| `CodecBenchmark.decode` | LAZY_REFERENCE | 3,418 ± 118 ns/op | 3,380 ± 95.83 ns/op | 3,150 ± 29.83 ns/op | 0.99× | 0.92× 🟢 |
| `CodecBenchmark.decode` | LIFECYCLE | 1,235 ± 21.17 ns/op | 1,295 ± 35.61 ns/op | 1,235 ± 32.69 ns/op | 1.05× | 1.00× |
| `CodecBenchmark.decode` | NESTED | 3,482 ± 86.82 ns/op | 3,721 ± 77.95 ns/op | 3,590 ± 48.56 ns/op | 1.07× 🔴 | 1.03× |
| `CodecBenchmark.decode` | POLYMORPHIC | 6,507 ± 95.02 ns/op | 6,890 ± 294 ns/op | 6,429 ± 62.15 ns/op | 1.06× 🔴 | 0.99× |
| `CodecBenchmark.decode` | SIMPLE | 1,351 ± 19.89 ns/op | 1,354 ± 9.01 ns/op | 1,436 ± 25.05 ns/op | 1.00× | 1.06× 🔴 |
| `CodecBenchmark.decodeInCursor` | ARRAYS | 69,341 ± 722 ns/op | 74,205 ± 1,063 ns/op | 67,705 ± 462 ns/op | 1.07× 🔴 | 0.98× |
| `CodecBenchmark.decodeInCursor` | COLLECTIONS | 9,935 ± 211 ns/op | 10,669 ± 111 ns/op | 10,012 ± 162 ns/op | 1.07× 🔴 | 1.01× |
| `CodecBenchmark.decodeInCursor` | CONCRETE_COLLECTIONS | 3,167 ± 151 ns/op | 3,373 ± 146 ns/op | 3,064 ± 38.17 ns/op | 1.06× 🔴 | 0.97× |
| `CodecBenchmark.decodeInCursor` | LAZY_REFERENCE | 3,351 ± 132 ns/op | 3,492 ± 162 ns/op | 3,205 ± 141 ns/op | 1.04× | 0.96× |
| `CodecBenchmark.decodeInCursor` | LIFECYCLE | 1,208 ± 22.59 ns/op | 1,285 ± 25.95 ns/op | 1,266 ± 26.55 ns/op | 1.06× 🔴 | 1.05× |
| `CodecBenchmark.decodeInCursor` | NESTED | 3,365 ± 38.01 ns/op | 3,786 ± 115 ns/op | 3,489 ± 87.67 ns/op | 1.13× 🔴 | 1.04× |
| `CodecBenchmark.decodeInCursor` | POLYMORPHIC | 6,458 ± 253 ns/op | 6,943 ± 78.24 ns/op | 6,367 ± 86.43 ns/op | 1.08× 🔴 | 0.99× |
| `CodecBenchmark.decodeInCursor` | SIMPLE | 1,464 ± 140 ns/op | 1,459 ± 57.92 ns/op | 1,441 ± 16.58 ns/op | 1.00× | 0.98× |
| `CodecBenchmark.encode` | ARRAYS | 99,318 ± 1,769 ns/op | 107,751 ± 3,718 ns/op | 93,843 ± 841 ns/op | 1.08× 🔴 | 0.94× 🟢 |
| `CodecBenchmark.encode` | COLLECTIONS | 7,948 ± 106 ns/op | 8,352 ± 159 ns/op | 7,920 ± 59.36 ns/op | 1.05× 🔴 | 1.00× |
| `CodecBenchmark.encode` | CONCRETE_COLLECTIONS | 2,115 ± 46.30 ns/op | 2,288 ± 77.45 ns/op | 2,028 ± 15.83 ns/op | 1.08× 🔴 | 0.96× |
| `CodecBenchmark.encode` | LAZY_REFERENCE | 1,958 ± 79.80 ns/op | 1,911 ± 68.63 ns/op | 1,722 ± 12.87 ns/op | 0.98× | 0.88× 🟢 |
| `CodecBenchmark.encode` | LIFECYCLE | 818 ± 10.17 ns/op | 885 ± 20.34 ns/op | 806 ± 4.72 ns/op | 1.08× 🔴 | 0.98× |
| `CodecBenchmark.encode` | NESTED | 2,351 ± 19.63 ns/op | 2,526 ± 30.57 ns/op | 2,392 ± 8.31 ns/op | 1.07× 🔴 | 1.02× |
| `CodecBenchmark.encode` | POLYMORPHIC | 4,345 ± 54.96 ns/op | 4,786 ± 62.05 ns/op | 4,488 ± 42.56 ns/op | 1.10× 🔴 | 1.03× |
| `CodecBenchmark.encode` | SIMPLE | 806 ± 11.23 ns/op | 860 ± 18.97 ns/op | 776 ± 2.88 ns/op | 1.07× 🔴 | 0.96× |
| `DatastoreBenchmark.copy` |  | 19.39 ± 0.28 us/op | 28.75 ± 0.84 us/op | 21.80 ± 0.19 us/op | 1.48× 🔴 | 1.12× 🔴 |
| `DatastoreBenchmark.copyAndDecode` |  | 26.87 ± 0.29 us/op | 37.77 ± 0.99 us/op | 29.77 ± 0.16 us/op | 1.41× 🔴 | 1.11× 🔴 |
| `DocumentShapesBenchmark.decode` | AS_WRITTEN | 1,315 ± 18.70 ns/op | 1,491 ± 17.92 ns/op | 1,353 ± 9.87 ns/op | 1.13× 🔴 | 1.03× |
| `DocumentShapesBenchmark.decode` | NO_DISCRIMINATOR | 1,465 ± 83.63 ns/op | 1,591 ± 77.20 ns/op | 1,367 ± 14.13 ns/op | 1.09× 🔴 | 0.93× 🟢 |
| `DocumentShapesBenchmark.decode` | SCHEMA_DRIFT | 4,521 ± 44.46 ns/op | 4,887 ± 111 ns/op | 4,411 ± 30.75 ns/op | 1.08× 🔴 | 0.98× |
| `ListenerBenchmark.decode` | NESTED | 14,583 ± 319 ns/op | 16,115 ± 499 ns/op | 14,610 ± 178 ns/op | 1.11× 🔴 | 1.00× |
| `ListenerBenchmark.decode` | SIMPLE | 3,196 ± 84.97 ns/op | 3,368 ± 68.70 ns/op | 3,216 ± 18.08 ns/op | 1.05× 🔴 | 1.01× |
| `ListenerBenchmark.encode` | NESTED | 11,724 ± 258 ns/op | 12,068 ± 181 ns/op | 11,484 ± 121 ns/op | 1.03× | 0.98× |
| `ListenerBenchmark.encode` | SIMPLE | 2,742 ± 119 ns/op | 2,799 ± 248 ns/op | 2,610 ± 36.10 ns/op | 1.02× | 0.95× |
| `MappingBenchmark.coldStart` |  | 125 ± 2.10 ms/op | 127 ± 4.37 ms/op | 210 ± 6.21 ms/op | 1.02× | 1.68× 🔴 |
| `MappingBenchmark.scanEverything` |  | 1,968 ± 213 ms/op | 1,979 ± 147 ms/op | 2,033 ± 122 ms/op | 1.01× | 1.03× |
| `MappingBenchmark.scanModelsPackage` |  | 222 ± 9.57 ms/op | 262 ± 7.63 ms/op | 322 ± 18.92 ms/op | 1.18× 🔴 | 1.45× 🔴 |
| `MappingBenchmark.warmMapping` |  | 63.92 ± 1.26 us/op | 37.07 ± 0.59 us/op | 85.18 ± 3.44 us/op | 0.58× 🟢 | 1.33× 🔴 |
| `MixedCodecBenchmark.decode` |  | 30,789 ± 262 ns/op | 33,539 ± 377 ns/op | 32,673 ± 432 ns/op | 1.09× 🔴 | 1.06× 🔴 |
| `MixedCodecBenchmark.encode` |  | 22,561 ± 343 ns/op | 22,991 ± 290 ns/op | 23,612 ± 374 ns/op | 1.02× | 1.05× |
| `QueryBenchmark.queryDocument` |  | 3,409 ± 27.00 ns/op | 3,282 ± 30.30 ns/op | 4,159 ± 84.05 ns/op | 0.96× | 1.22× 🔴 |
| `QueryBenchmark.translatedPath` |  | 1,063 ± 22.85 ns/op | 957 ± 7.48 ns/op | 1,171 ± 7.34 ns/op | 0.90× 🟢 | 1.10× 🔴 |

### Allocation per operation (`gc.alloc.rate.norm`)

| Benchmark | Params | reflection | critter | critter-runtime | critter / reflection | critter-runtime / reflection |
|---|---|---:|---:|---:|---:|---:|
| `CodecBenchmark.decode` | ARRAYS | 76,930 B/op | 76,930 B/op | 76,930 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decode` | COLLECTIONS | 22,088 B/op | 22,096 B/op | 22,120 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decode` | CONCRETE_COLLECTIONS | 6,640 B/op | 6,640 B/op | 6,640 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decode` | LAZY_REFERENCE | 8,269 B/op | 8,240 B/op | 8,245 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decode` | LIFECYCLE | 3,160 B/op | 3,117 B/op | 3,117 B/op | 0.99× | 0.99× |
| `CodecBenchmark.decode` | NESTED | 8,008 B/op | 7,992 B/op | 7,992 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decode` | POLYMORPHIC | 14,424 B/op | 14,432 B/op | 14,424 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decode` | SIMPLE | 3,160 B/op | 3,168 B/op | 3,168 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decodeInCursor` | ARRAYS | 76,594 B/op | 76,594 B/op | 76,594 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decodeInCursor` | COLLECTIONS | 21,752 B/op | 21,744 B/op | 21,760 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decodeInCursor` | CONCRETE_COLLECTIONS | 6,304 B/op | 6,304 B/op | 6,304 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decodeInCursor` | LAZY_REFERENCE | 7,925 B/op | 7,883 B/op | 7,939 B/op | 0.99× | 1.00× |
| `CodecBenchmark.decodeInCursor` | LIFECYCLE | 2,803 B/op | 2,800 B/op | 2,803 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decodeInCursor` | NESTED | 7,672 B/op | 7,672 B/op | 7,627 B/op | 1.00× | 0.99× |
| `CodecBenchmark.decodeInCursor` | POLYMORPHIC | 14,064 B/op | 14,064 B/op | 14,064 B/op | 1.00× | 1.00× |
| `CodecBenchmark.decodeInCursor` | SIMPLE | 2,832 B/op | 2,832 B/op | 2,832 B/op | 1.00× | 1.00× |
| `CodecBenchmark.encode` | ARRAYS | 137,220 B/op | 145,796 B/op | 137,219 B/op | 1.06× 🔴 | 1.00× |
| `CodecBenchmark.encode` | COLLECTIONS | 7,688 B/op | 7,688 B/op | 7,742 B/op | 1.00× | 1.01× |
| `CodecBenchmark.encode` | CONCRETE_COLLECTIONS | 3,157 B/op | 3,136 B/op | 3,136 B/op | 0.99× | 0.99× |
| `CodecBenchmark.encode` | LAZY_REFERENCE | 4,104 B/op | 4,104 B/op | 4,104 B/op | 1.00× | 1.00× |
| `CodecBenchmark.encode` | LIFECYCLE | 3,075 B/op | 3,096 B/op | 3,032 B/op | 1.01× | 0.99× |
| `CodecBenchmark.encode` | NESTED | 2,016 B/op | 2,016 B/op | 2,016 B/op | 1.00× | 1.00× |
| `CodecBenchmark.encode` | POLYMORPHIC | 5,960 B/op | 5,960 B/op | 5,960 B/op | 1.00× | 1.00× |
| `CodecBenchmark.encode` | SIMPLE | 1,248 B/op | 1,248 B/op | 1,248 B/op | 1.00× | 1.00× |
| `DatastoreBenchmark.copy` |  | 65,169 B/op | 85,565 B/op | 74,093 B/op | 1.31× 🔴 | 1.14× 🔴 |
| `DatastoreBenchmark.copyAndDecode` |  | 81,913 B/op | 98,985 B/op | 90,045 B/op | 1.21× 🔴 | 1.10× 🔴 |
| `DocumentShapesBenchmark.decode` | AS_WRITTEN | 3,168 B/op | 3,168 B/op | 3,168 B/op | 1.00× | 1.00× |
| `DocumentShapesBenchmark.decode` | NO_DISCRIMINATOR | 3,216 B/op | 3,216 B/op | 3,216 B/op | 1.00× | 1.00× |
| `DocumentShapesBenchmark.decode` | SCHEMA_DRIFT | 5,628 B/op | 5,848 B/op | 5,847 B/op | 1.04× | 1.04× |
| `ListenerBenchmark.decode` | NESTED | 42,160 B/op | 42,320 B/op | 42,492 B/op | 1.00× | 1.01× |
| `ListenerBenchmark.decode` | SIMPLE | 8,704 B/op | 8,688 B/op | 8,720 B/op | 1.00× | 1.00× |
| `ListenerBenchmark.encode` | NESTED | 38,736 B/op | 38,656 B/op | 38,736 B/op | 1.00× | 1.00× |
| `ListenerBenchmark.encode` | SIMPLE | 8,224 B/op | 8,216 B/op | 8,224 B/op | 1.00× | 1.00× |
| `MappingBenchmark.coldStart` |  | 11,720,204 B/op | 12,092,909 B/op | 23,429,605 B/op | 1.03× | 2.00× 🔴 |
| `MappingBenchmark.scanEverything` |  | 762,624,899 B/op | 769,891,954 B/op | 771,088,326 B/op | 1.01× | 1.01× |
| `MappingBenchmark.scanModelsPackage` |  | 27,248,614 B/op | 33,344,109 B/op | 40,908,162 B/op | 1.22× 🔴 | 1.50× 🔴 |
| `MappingBenchmark.warmMapping` |  | 170,040 B/op | 91,600 B/op | 104,296 B/op | 0.54× 🟢 | 0.61× 🟢 |
| `MixedCodecBenchmark.decode` |  | 67,305 B/op | 67,329 B/op | 67,437 B/op | 1.00× | 1.00× |
| `MixedCodecBenchmark.encode` |  | 27,777 B/op | 27,777 B/op | 27,777 B/op | 1.00× | 1.00× |
| `QueryBenchmark.queryDocument` |  | 14,752 B/op | 15,068 B/op | 15,568 B/op | 1.02× | 1.06× 🔴 |
| `QueryBenchmark.translatedPath` |  | 5,248 B/op | 5,244 B/op | 5,440 B/op | 1.00× | 1.04× |
