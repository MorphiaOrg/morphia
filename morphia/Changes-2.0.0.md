
## Version 2.0.0 (Oct 31, 2019)

### Notes

### Downloads
Binaries can be found on maven central.

### Docs
Full documentation and javadoc can be found at https://github.com/MorphiaOrg/morphia and https://morphia.dev/1.5/javadoc/.

### 42 Issues Resolved
#### ![](https://placehold.it/15/eb6420/000000?text=+) BREAKING
* [Issue #1122](https://github.com/MorphiaOrg/morphia/issues/1122): Remove org.mongodb.morphia.query.Query#order(java.lang.String) 
* [Issue #1230](https://github.com/MorphiaOrg/morphia/issues/1230): FindAndModify Default Behaviour
* [Issue #1275](https://github.com/MorphiaOrg/morphia/issues/1275): remove QueryImpl#parseFieldsString from the public API
* [Issue #1284](https://github.com/MorphiaOrg/morphia/issues/1284): Remove MapReduce functionality
* [Issue #1294](https://github.com/MorphiaOrg/morphia/issues/1294): Remove support for snapshots and $isolate
* [Issue #1298](https://github.com/MorphiaOrg/morphia/issues/1298): Remove exists methods from Datastore
* [Issue #1300](https://github.com/MorphiaOrg/morphia/issues/1300): Rename methods
* [Issue #1350](https://github.com/MorphiaOrg/morphia/issues/1350): Drop MongoIterable from Query's parent types
* [Issue #1351](https://github.com/MorphiaOrg/morphia/issues/1351): Replace all uses of DBObject with Document and remove all exposures of these types from the API where possible
* [Issue #1352](https://github.com/MorphiaOrg/morphia/issues/1352): Remove morphia logging code and migrate to slf4j

#### ![](https://placehold.it/15/fc2929/000000?text=+) BUG
* [Issue #553](https://github.com/MorphiaOrg/morphia/issues/553): Generics mapping throws NPE
* [Issue #921](https://github.com/MorphiaOrg/morphia/issues/921): Datastore.ensureIndexes() breaks morphia
* [Issue #1230](https://github.com/MorphiaOrg/morphia/issues/1230): FindAndModify Default Behaviour
* [Issue #1350](https://github.com/MorphiaOrg/morphia/issues/1350): Drop MongoIterable from Query's parent types

#### ![](https://placehold.it/15/fbca04/000000?text=+) DOCS
* [Issue #1266](https://github.com/MorphiaOrg/morphia/issues/1266): Update all documentation urls
* [Issue #1269](https://github.com/MorphiaOrg/morphia/issues/1269): add an @internal taglet

#### ![](https://placehold.it/15/84b6eb/000000?text=+) ENHANCEMENT
* [Issue #22](https://github.com/MorphiaOrg/morphia/issues/22): Add @Polymorphic [moved]
* [Issue #364](https://github.com/MorphiaOrg/morphia/issues/364): Polymorphic lazy reference [moved]
* [Issue #735](https://github.com/MorphiaOrg/morphia/issues/735): Query.order(String) API design, to overwrite values set in previous calls, is flawed
* [Issue #749](https://github.com/MorphiaOrg/morphia/issues/749): [enhancement] Support the new driver API
* [Issue #1121](https://github.com/MorphiaOrg/morphia/issues/1121): Remove deprecated elements
* [Issue #1122](https://github.com/MorphiaOrg/morphia/issues/1122): Remove org.mongodb.morphia.query.Query#order(java.lang.String) 
* [Issue #1141](https://github.com/MorphiaOrg/morphia/issues/1141): Remove EntityScanner
* [Issue #1143](https://github.com/MorphiaOrg/morphia/issues/1143): Remove methods exposing MorphiaIterator and MorphiaKeyIterator
* [Issue #1269](https://github.com/MorphiaOrg/morphia/issues/1269): add an @internal taglet
* [Issue #1272](https://github.com/MorphiaOrg/morphia/issues/1272): Upgrade minimum JVM version to 11
* [Issue #1275](https://github.com/MorphiaOrg/morphia/issues/1275): remove QueryImpl#parseFieldsString from the public API
* [Issue #1284](https://github.com/MorphiaOrg/morphia/issues/1284): Remove MapReduce functionality
* [Issue #1294](https://github.com/MorphiaOrg/morphia/issues/1294): Remove support for snapshots and $isolate
* [Issue #1298](https://github.com/MorphiaOrg/morphia/issues/1298): Remove exists methods from Datastore
* [Issue #1299](https://github.com/MorphiaOrg/morphia/issues/1299): Deprecate getCount() methods in favor of Query.count()
* [Issue #1300](https://github.com/MorphiaOrg/morphia/issues/1300): Rename methods
* [Issue #1301](https://github.com/MorphiaOrg/morphia/issues/1301): explore replacements for com.mongodb.client.model.FindOptions
* [Issue #1302](https://github.com/MorphiaOrg/morphia/issues/1302): Add deprecated placeholders for old methods on *Datastore and Query
* [Issue #1319](https://github.com/MorphiaOrg/morphia/issues/1319): Please update to recent mongodb driver MongoClient interface
* [Issue #1351](https://github.com/MorphiaOrg/morphia/issues/1351): Replace all uses of DBObject with Document and remove all exposures of these types from the API where possible
* [Issue #1352](https://github.com/MorphiaOrg/morphia/issues/1352): Remove morphia logging code and migrate to slf4j
* [Issue #1353](https://github.com/MorphiaOrg/morphia/issues/1353): Update to use Codecs instead of current mapping code
* [Issue #1354](https://github.com/MorphiaOrg/morphia/issues/1354): Use MongoCollection rather than DBCollection
* [Issue #1355](https://github.com/MorphiaOrg/morphia/issues/1355): Add remove() to Query
* [Issue #1356](https://github.com/MorphiaOrg/morphia/issues/1356): Add update() to Query
* [Issue #1359](https://github.com/MorphiaOrg/morphia/issues/1359): add modify() to Query
* [Issue #1360](https://github.com/MorphiaOrg/morphia/issues/1360): add delete() to Query
* [Issue #1364](https://github.com/MorphiaOrg/morphia/issues/1364): Options classes should subclass the driver options classes
* [Issue #1366](https://github.com/MorphiaOrg/morphia/issues/1366): Replace ReflectionUtils
* [Issue #1372](https://github.com/MorphiaOrg/morphia/issues/1372): Delete @Serialized and remove support
* [Issue #1374](https://github.com/MorphiaOrg/morphia/issues/1374): Remove @PreSave
* [Issue #1377](https://github.com/MorphiaOrg/morphia/issues/1377): Remove @NotSaved
* [Issue #1379](https://github.com/MorphiaOrg/morphia/issues/1379): Drop returning instances/lists of Keys
* [Issue #1390](https://github.com/MorphiaOrg/morphia/issues/1390): Remove support for CodeWScope
* [Issue #1393](https://github.com/MorphiaOrg/morphia/issues/1393): Move getCollection(Class) implementation from Mapper to Datastore

#### ![](https://placehold.it/15/null/000000?text=+) UNCATEGORIZED
* [Issue #935](https://github.com/MorphiaOrg/morphia/issues/935): Morphia does not uses registered Codec
* [Issue #1293](https://github.com/MorphiaOrg/morphia/issues/1293): Generic Entity

