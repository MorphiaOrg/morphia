## Migrating to 1.5
*  Indexing of `@Embedded` fields is being deprecated in favor of declaring all indexes at the top level `@Entity` level.  The new index 
definitions just need to use the dotted path names to recreate these indexes.  To turn off the warning in the logs about embedded 
indexing, simply pass `false` to `MapperOptions#setDisableEmbeddedIndexes`.
*  `Query#asList()` is deprecated.  Usages should be updated to use `find().toList()` instead.