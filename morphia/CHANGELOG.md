
## Version 1.5.0 (Apr 14, 2019)

### Notes

### Downloads
Binaries can be found on maven central.

### Docs
Full documentation and javadoc can be found at https://github.com/MorphiaOrg/morphia and https://morphia.dev/1.5/javadoc/.

### 81 Issues Resolved
#### ![](https://placehold.it/15/dde580/000000?text=+) TASK
* [Issue #1324](https://github.com/MorphiaOrg/morphia/issues/1324): Rename packages

#### ![](https://placehold.it/15/fef2c0/000000?text=+) AGGREGATION
* [Issue #1006](https://github.com/MorphiaOrg/morphia/issues/1006): add support for $sample

#### ![](https://placehold.it/15/fc2929/000000?text=+) BUG
* [Issue #336](https://github.com/MorphiaOrg/morphia/issues/336): DBRefs break when a custom collection name is used to save referenced entities
* [Issue #1027](https://github.com/MorphiaOrg/morphia/issues/1027): equalIgnoreCase fails with input including "... [a-z]"
* [Issue #1104](https://github.com/MorphiaOrg/morphia/issues/1104): NPE in GeoIntersectsQueriesWithPointTest
* [Issue #1114](https://github.com/MorphiaOrg/morphia/issues/1114): Update query incrementing BigDecimal - missing codec for BigDecimal
* [PR #1130](https://github.com/MorphiaOrg/morphia/pull/1130): bugfix: the resolve function would be called multiple times
* [Issue #1145](https://github.com/MorphiaOrg/morphia/issues/1145): Map with a collection value breaks mapper
* [Issue #1151](https://github.com/MorphiaOrg/morphia/issues/1151): ensureIndex broken in 1.3.2
* [Issue #1154](https://github.com/MorphiaOrg/morphia/issues/1154): multi is not consistently set in DatastoreImpl update methods
* [Issue #1175](https://github.com/MorphiaOrg/morphia/issues/1175): AdvancedDatastore.ensureIndexes doesn't handle fields subtypes properly with version 1.3.2
* [Issue #1197](https://github.com/MorphiaOrg/morphia/issues/1197): doesn't auto create index for entities that's mapped in the subpackage
* [PR #1211](https://github.com/MorphiaOrg/morphia/pull/1211): Fix for ensureIndexes() logic
* [Issue #1217](https://github.com/MorphiaOrg/morphia/issues/1217): Lazy loading in interfaces
* [Issue #1255](https://github.com/MorphiaOrg/morphia/issues/1255): Excessive NullPointerException in Mapper.getId()
* [Issue #1278](https://github.com/MorphiaOrg/morphia/issues/1278): Morphia's Criteriacontainer overwrites previously set "$or" statement
* [Issue #1290](https://github.com/MorphiaOrg/morphia/issues/1290): Indexes: correctly translate partial filter expressions with automatic embedded entity indexes
* [Issue #1291](https://github.com/MorphiaOrg/morphia/issues/1291): field of type Set is returing incorrect type (easy fix)
* [Issue #1312](https://github.com/MorphiaOrg/morphia/issues/1312): EnsureCaps on DatastoreImpl only works for collections that do not exist
* [PR #1328](https://github.com/MorphiaOrg/morphia/pull/1328): fix getByKeys()
* [Issue #1330](https://github.com/MorphiaOrg/morphia/issues/1330): MorphiaReferences don't work with interfaces
* [Issue #1331](https://github.com/MorphiaOrg/morphia/issues/1331): Deprecate alternate collection saves
* [Issue #1336](https://github.com/MorphiaOrg/morphia/issues/1336): Using @Indexed with no other attribute than options=@IndexOptions() breaks

#### ![](https://placehold.it/15/fbca04/000000?text=+) DOCS
* [Issue #1261](https://github.com/MorphiaOrg/morphia/issues/1261): Validation will not be created
* [Issue #1265](https://github.com/MorphiaOrg/morphia/issues/1265): Update all documentation urls
* [Issue #1286](https://github.com/MorphiaOrg/morphia/issues/1286): Create new examples module
* [Issue #1303](https://github.com/MorphiaOrg/morphia/issues/1303): Add @inline taglet
* [Issue #1304](https://github.com/MorphiaOrg/morphia/issues/1304): Create a roadmap
* [Issue #1307](https://github.com/MorphiaOrg/morphia/issues/1307): Delete package-info.java files
* [Issue #1311](https://github.com/MorphiaOrg/morphia/issues/1311): Document new reference wrappers
* [Issue #1325](https://github.com/MorphiaOrg/morphia/issues/1325):  Update documentation with the new package
* [Issue #1331](https://github.com/MorphiaOrg/morphia/issues/1331): Deprecate alternate collection saves

#### ![](https://placehold.it/15/84b6eb/000000?text=+) ENHANCEMENT
* [Issue #617](https://github.com/MorphiaOrg/morphia/issues/617): No usable constructor for java.util.Currency
* [Issue #948](https://github.com/MorphiaOrg/morphia/issues/948): Introduce reference wrapper type 
* [Issue #977](https://github.com/MorphiaOrg/morphia/issues/977): add minDistance option to $near geospatial operator
* [Issue #1006](https://github.com/MorphiaOrg/morphia/issues/1006): add support for $sample
* [Issue #1077](https://github.com/MorphiaOrg/morphia/issues/1077): Update QueryValidator to use PathTarget
* [PR #1081](https://github.com/MorphiaOrg/morphia/pull/1081): implemented $sample operation
* [Issue #1099](https://github.com/MorphiaOrg/morphia/issues/1099): Deprecate `updateFirst` methods and `update*` methods that don't take `UpdateOptions`
* [Issue #1117](https://github.com/MorphiaOrg/morphia/issues/1117): Introduce `Query<?> Datastore.find()`
* [Issue #1119](https://github.com/MorphiaOrg/morphia/issues/1119): Deprecate AdvancedDatastore#find(String, Class, String, V, int, int)
* [Issue #1120](https://github.com/MorphiaOrg/morphia/issues/1120): Deprecate org.mongodb.morphia.query.Query#order(java.lang.String)
* [Issue #1123](https://github.com/MorphiaOrg/morphia/issues/1123): UpdateOpsImpl does not respect fieldName annotations for maps
* [Issue #1125](https://github.com/MorphiaOrg/morphia/issues/1125): Deprecate Datastore.getDB()
* [Issue #1133](https://github.com/MorphiaOrg/morphia/issues/1133): Move constants off Mapper
* [Issue #1134](https://github.com/MorphiaOrg/morphia/issues/1134): Introduce builder for MapperOptions
* [Issue #1136](https://github.com/MorphiaOrg/morphia/issues/1136): Deprecate Mapper, MappedField, and MappedClass references
* [Issue #1142](https://github.com/MorphiaOrg/morphia/issues/1142): Deprecate uses of MorphiaIterator and MorphiaKeyIterator
* [PR #1150](https://github.com/MorphiaOrg/morphia/pull/1150): Projection expressions with single arguments e.g. {"$size": "$source"}
* [PR #1152](https://github.com/MorphiaOrg/morphia/pull/1152): JAVA-2453 Optimistically lock manually ID'd Versioned Entities
* [Issue #1174](https://github.com/MorphiaOrg/morphia/issues/1174): Add option in @Entity or @Embedded to not automatically create indexes for Embedded Classes
* [PR #1176](https://github.com/MorphiaOrg/morphia/pull/1176): #617 add java.util.Currency converter
* [Issue #1202](https://github.com/MorphiaOrg/morphia/issues/1202): UpdateOpImpl addToSet : can you add Iterable as an input along with List and Object 
* [PR #1203](https://github.com/MorphiaOrg/morphia/pull/1203): Fixed #1202
* [Issue #1218](https://github.com/MorphiaOrg/morphia/issues/1218): Java 8 LocalDate/Time conversion
* [PR #1225](https://github.com/MorphiaOrg/morphia/pull/1225): build clean ups
* [PR #1237](https://github.com/MorphiaOrg/morphia/pull/1237): add field name to an exception
* [PR #1245](https://github.com/MorphiaOrg/morphia/pull/1245):  Add support for UnwindOptions in AggregationPipeline
* [Issue #1255](https://github.com/MorphiaOrg/morphia/issues/1255): Excessive NullPointerException in Mapper.getId()
* [Issue #1259](https://github.com/MorphiaOrg/morphia/issues/1259): Items deprecated in 3.6
* [Issue #1270](https://github.com/MorphiaOrg/morphia/issues/1270): Deprecate methods on QueryResults.  add new ones
* [Issue #1271](https://github.com/MorphiaOrg/morphia/issues/1271): deprecrate IndexBuilder#dropDups()
* [Issue #1273](https://github.com/MorphiaOrg/morphia/issues/1273): Deprecate MapReduce
* [Issue #1276](https://github.com/MorphiaOrg/morphia/issues/1276): remove no-proxy-deps-tests module
* [Issue #1280](https://github.com/MorphiaOrg/morphia/issues/1280): Order of LinkedHashMap is lost during update operation
* [Issue #1285](https://github.com/MorphiaOrg/morphia/issues/1285): Deprecate Datastore.getCollection()
* [Issue #1286](https://github.com/MorphiaOrg/morphia/issues/1286): Create new examples module
* [Issue #1303](https://github.com/MorphiaOrg/morphia/issues/1303): Add @inline taglet
* [Issue #1307](https://github.com/MorphiaOrg/morphia/issues/1307): Delete package-info.java files
* [Issue #1308](https://github.com/MorphiaOrg/morphia/issues/1308): Deprecate native logging
* [Issue #1313](https://github.com/MorphiaOrg/morphia/issues/1313): Deprecate the DAO types
* [Issue #1314](https://github.com/MorphiaOrg/morphia/issues/1314): mapPackage fail to load class from jar (by maven pacakge which with some module)
* [Issue #1316](https://github.com/MorphiaOrg/morphia/issues/1316): Deprecate generation of indexes from definitions found in embedded types
* [Issue #1324](https://github.com/MorphiaOrg/morphia/issues/1324): Rename packages
* [Issue #1331](https://github.com/MorphiaOrg/morphia/issues/1331): Deprecate alternate collection saves

#### ![](https://placehold.it/15/cc317c/000000?text=+) QUESTION
* [Issue #1213](https://github.com/MorphiaOrg/morphia/issues/1213): Query using elemMatch fails when elemMatch-query does specify some restrictions
* [Issue #1235](https://github.com/MorphiaOrg/morphia/issues/1235): Clarification on Datastore.merge() on versioned item.

#### ![](https://placehold.it/15/null/000000?text=+) UNCATEGORIZED
* [Issue #1210](https://github.com/MorphiaOrg/morphia/issues/1210): update in DatastoreImpl is not multi anymore

