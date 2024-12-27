package dev.morphia.aggregation;

import java.util.List;

import com.mongodb.client.model.geojson.Point;

import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.AddFields;
import dev.morphia.aggregation.stages.AutoBucket;
import dev.morphia.aggregation.stages.Bucket;
import dev.morphia.aggregation.stages.ChangeStream;
import dev.morphia.aggregation.stages.CollectionStats;
import dev.morphia.aggregation.stages.Count;
import dev.morphia.aggregation.stages.CurrentOp;
import dev.morphia.aggregation.stages.Densify;
import dev.morphia.aggregation.stages.Densify.Range;
import dev.morphia.aggregation.stages.Documents;
import dev.morphia.aggregation.stages.Facet;
import dev.morphia.aggregation.stages.Fill;
import dev.morphia.aggregation.stages.GeoNear;
import dev.morphia.aggregation.stages.GraphLookup;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.aggregation.stages.Group.GroupId;
import dev.morphia.aggregation.stages.IndexStats;
import dev.morphia.aggregation.stages.Lookup;
import dev.morphia.aggregation.stages.Match;
import dev.morphia.aggregation.stages.Merge;
import dev.morphia.aggregation.stages.Out;
import dev.morphia.aggregation.stages.PlanCacheStats;
import dev.morphia.aggregation.stages.Projection;
import dev.morphia.aggregation.stages.Redact;
import dev.morphia.aggregation.stages.ReplaceRoot;
import dev.morphia.aggregation.stages.ReplaceWith;
import dev.morphia.aggregation.stages.Sample;
import dev.morphia.aggregation.stages.Set;
import dev.morphia.aggregation.stages.SetWindowFields;
import dev.morphia.aggregation.stages.Skip;
import dev.morphia.aggregation.stages.Sort;
import dev.morphia.aggregation.stages.SortByCount;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.aggregation.stages.UnionWith;
import dev.morphia.aggregation.stages.Unset;
import dev.morphia.aggregation.stages.Unwind;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.filters.Filter;

/**
 * @param <T> The initial type of the aggregation. Used for collection name resolution.
 * @since 2.0
 */
public interface Aggregation<T> {
    /**
     * Appends the stages to this aggregation's pipeline.
     * 
     * @param stages the stages to add
     * @return this
     * @since 3.0
     */
    Aggregation<T> pipeline(Stage... stages);

    default Aggregation<T> pipeline(List<Stage> stages) {
        return pipeline(stages.toArray(new Stage[0]));
    }

    /**
     * Execute the aggregation. This form and {@link #execute(AggregationOptions)} should be used for pipelines with $out and $merge
     * that do not expect any results to be returned.
     *
     * @see #execute(AggregationOptions)
     */
    void execute();

    /**
     * Execute the aggregation and get the results.
     *
     * @param resultType the type of the result
     * @param <S>        the output type
     * @return a MorphiaCursor
     */
    <S> MorphiaCursor<S> execute(Class<S> resultType);

    /**
     * Execute the aggregation. This form and {@link #execute()} should be used for pipelines with $out and $merge
     * that do no expect any results to be returned.
     *
     * @see #execute()
     */
    void execute(AggregationOptions options);

    /**
     * Execute the aggregation and get the results.
     *
     * @param resultType the type of the result
     * @param options    the options to apply
     * @param <S>        the output type
     * @return a MorphiaCursor
     */
    <S> MorphiaCursor<S> execute(Class<S> resultType, AggregationOptions options);

    /**
     * Writes the results of the aggregation pipeline to a specified collection. The $merge operator must be the last stage in the pipeline.
     *
     * @param merge the merge definition
     * @param <M>   the output collection type
     * @aggregation.stage $merge
     * @mongodb.server.release 4.2
     */
    <M> void merge(Merge<M> merge);

    /**
     * Writes the results of the aggregation pipeline to a specified collection. The $merge operator must be the last stage in the pipeline.
     *
     * @param merge   the merge definition
     * @param options the options to apply
     * @param <M>     the output collection type
     * @aggregation.stage $merge
     * @mongodb.server.release 3.4
     */
    <M> void merge(Merge<M> merge, AggregationOptions options);

    /**
     * Writes the results of the aggregation pipeline to a specified collection. The $out operator must be the last stage in the pipeline.
     *
     * @param out the out definition
     * @param <O> the output collection type
     * @aggregation.stage $out
     */
    <O> void out(Out<O> out);

    /**
     * Writes the results of the aggregation pipeline to a specified collection. The $out operator must be the last stage in the pipeline.
     *
     * @param out     the out definition
     * @param options the options to apply
     * @param <O>     the output collection type
     * @aggregation.stage $out
     */
    <O> void out(Out<O> out, AggregationOptions options);

    /**
     * Categorizes incoming documents into a specific number of groups, called buckets, based on a specified expression. Bucket
     * boundaries are automatically determined in an attempt to evenly distribute the documents into the specified number of buckets.
     * <p>
     * Each bucket is represented as a document in the output. The document for each bucket contains an _id field, whose value specifies
     * the inclusive lower bound and the exclusive upper bound for the bucket, and a count field that contains the number of documents in
     * the bucket. The count field is included by default when the output is not specified.
     *
     * @param bucket the bucket definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see AutoBucket#autoBucket()
     */
    Aggregation<T> autoBucket(AutoBucket bucket);

    /**
     * Categorizes incoming documents into groups, called buckets, based on a specified expression and bucket boundaries.
     * <p>
     * Each bucket is represented as a document in the output. The document for each bucket contains an _id field, whose value specifies
     * the inclusive lower bound of the bucket and a count field that contains the number of documents in the bucket. The count field is
     * included by default when the output is not specified.
     * <p>
     * $bucket only produces output documents for buckets that contain at least one input document.
     *
     * @param bucket the bucket definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Bucket#bucket()
     */
    Aggregation<T> bucket(Bucket bucket);

    /**
     * Returns statistics regarding a collection or view.
     *
     * @param stats the stats configuration
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see CollectionStats#collStats()
     */
    Aggregation<T> collStats(CollectionStats stats);

    /**
     * Passes a document to the next stage that contains a count of the number of documents input to the stage.
     *
     * @param name the field name for the resulting count value
     * @return this
     * @mongodb.server.release 3.4
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Count#count(String)
     */
    Aggregation<T> count(String name);

    /**
     * Returns a stream of documents containing information on active and/or dormant operations as well as inactive sessions that are
     * holding locks as part of a transaction. The stage returns a document for each operation or session. To run $currentOp, use the
     * db.aggregate() helper on the admin database.
     * <p>
     * The $currentOp aggregation stage is preferred over the currentOp command and its mongo shell helper db.currentOp(). Because
     * currentOp command and db.currentOp() helper returns the results in a single document, the total size of the currentOp result
     * set is subject to the maximum 16MB BSON size limit for documents. The $currentOp stage returns a cursor over a stream of
     * documents, each of which reports a single operation. Each operation document is subject to the 16MB BSON limit, but unlike the
     * currentOp command, there is no limit on the overall size of the result set.
     * <p>
     * $currentOp also enables you to perform arbitrary transformations of the results as the documents pass through the pipeline.
     *
     * @param currentOp the configuration
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see CurrentOp#currentOp()
     */
    Aggregation<T> currentOp(CurrentOp currentOp);

    /**
     * Creates new documents in a sequence of documents where certain values in a field are missing.
     *
     * @param densify the Densify stage
     * @return this
     * @since 2.3
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Densify#densify(String, Range)
     */
    Aggregation<T> densify(Densify densify);

    /**
     * Returns literal documents from input values.
     *
     * @param documents the documents to use
     * @return this
     * @since 2.3
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Documents#documents(DocumentExpression...)
     */
    Aggregation<T> documents(DocumentExpression... documents);

    /**
     * Processes multiple aggregation pipelines within a single stage on the same set of input documents. Each sub-pipeline has its own
     * field in the output document where its results are stored as an array of documents.
     * <p>
     * The $facet stage allows you to create multi-faceted aggregations which characterize data across multiple dimensions, or facets,
     * within a single aggregation stage. Multi-faceted aggregations provide multiple filters and categorizations to guide data browsing
     * and analysis. Retailers commonly use faceting to narrow search results by creating filters on product price, manufacturer, size, etc.
     * <p>
     * Input documents are passed to the $facet stage only once. $facet enables various aggregations on the same set of input documents,
     * without needing to retrieve the input documents multiple times.
     *
     * @param facet the facet definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Facet#facet()
     */
    Aggregation<T> facet(Facet facet);

    /**
     * Populates null and missing field values within documents.
     * <p>
     * You can use $fill to populate missing data points:
     * <ul>
     * <li>In a sequence based on surrounding values.
     * <li>With a fixed value.
     * </ul>
     *
     * @param fill the fill definition
     * @return this
     * @since 2.3
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Fill#fill()
     */
    Aggregation<T> fill(Fill fill);

    /**
     * Outputs documents in order of nearest to farthest from a specified point.
     *
     * @param near the geo query definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see GeoNear#geoNear(Point)
     * @see GeoNear#geoNear(double[])
     */
    Aggregation<T> geoNear(GeoNear near);

    /**
     * Performs a recursive search on a collection, with options for restricting the search by recursion depth and query filter.
     *
     * @param lookup the lookup configuration
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see GraphLookup#graphLookup(String)
     * @see GraphLookup#graphLookup(Class)
     */
    Aggregation<T> graphLookup(GraphLookup lookup);

    /**
     * Groups input documents by the specified _id expression and for each distinct grouping, outputs a document. The _id field of each
     * output document contains the unique group by value. The output documents can also contain computed fields that hold the values of
     * some accumulator expression.
     *
     * @param group the group definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Group#group(GroupId)
     * @see Group#group()
     */
    Aggregation<T> group(Group group);

    /**
     * Returns statistics regarding the use of each index for the collection. If running with access control, the user must have
     * privileges that include indexStats action.
     *
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see IndexStats#indexStats()
     */
    Aggregation<T> indexStats();

    /**
     * Limits the number of documents passed to the next stage in the pipeline.
     *
     * @param limit the maximum docs to pass along to the next stage
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see dev.morphia.aggregation.stages.Limit#limit(long)
     */
    Aggregation<T> limit(long limit);

    /**
     * Performs a left outer join to an unsharded collection in the same database to filter in documents from the “joined” collection for
     * processing. To each input document, the $lookup stage adds a new array field whose elements are the matching documents from the
     * “joined” collection. The $lookup stage passes these reshaped documents to the next stage.
     *
     * @param lookup the lookup definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Lookup#lookup()
     * @see Lookup#lookup(String)
     * @see Lookup#lookup(Class)
     */
    Aggregation<T> lookup(Lookup lookup);

    /**
     * Filters the document stream to allow only matching documents to pass unmodified into the next pipeline stage. $match uses standard
     * MongoDB queries. For each input document, outputs either one document (a match) or zero documents (no match).
     *
     * @param filters the filters to use when matching
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Match#match(Filter...)
     */
    Aggregation<T> match(Filter... filters);

    /**
     * Returns plan cache information for a collection. The stage returns a document for each plan cache entry.
     *
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see PlanCacheStats#planCacheStats()
     */
    Aggregation<T> planCacheStats();

    /**
     * Passes along the documents with the requested fields to the next stage in the pipeline. The specified fields can be existing fields
     * from the input documents or newly computed fields.
     *
     * @param projection the project definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Projection#project()
     */
    Aggregation<T> project(Projection projection);

    /**
     * Restricts the contents of the documents based on information stored in the documents themselves.
     *
     * @param redact the redaction definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Redact#redact(Expression)
     */
    Aggregation<T> redact(Redact redact);

    /**
     * Replaces the input document with the specified document. The operation replaces all existing fields in the input document,
     * including the _id field. You can promote an existing embedded document to the top level, or create a new document for promotion
     *
     * @param root the new root definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see ReplaceRoot#replaceRoot()
     */
    Aggregation<T> replaceRoot(ReplaceRoot root);

    /**
     * Replaces the input document with the specified document. The operation replaces all existing fields in the input document,
     * including the _id field. With $replaceWith, you can promote an embedded document to the top-level. You can also specify a new
     * document as the replacement.
     * <p>
     * The $replaceWith is an alias for $replaceRoot.
     *
     * @param with the replacement definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see ReplaceWith#replaceWith()
     */
    Aggregation<T> replaceWith(ReplaceWith with);

    /**
     * Randomly selects the specified number of documents from the previous pipeline stage.
     *
     * @param sample the sample definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Sample#sample(long)
     */
    Aggregation<T> sample(long sample);

    /**
     * Adds new fields to documents. $addFields outputs documents that contain all existing fields from the input documents and newly
     * added fields.
     * <p>
     * The $addFields stage is equivalent to a $project stage that explicitly specifies all existing fields in the input documents and
     * adds the new fields.
     *
     * @param fields the stage definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see AddFields#addFields()
     */
    @Deprecated(since = "3.0", forRemoval = true)
    Aggregation<T> addFields(AddFields fields);

    /**
     * Adds new fields to documents. $set outputs documents that contain all existing fields from the input documents and newly added
     * fields.
     * <p>
     * The $set stage is an alias for $addFields.
     * <p>
     * Both stages are equivalent to a $project stage that explicitly specifies all existing fields in the input documents and adds the
     * new fields.
     *
     * @param set the stage to add
     * @return this
     * @since 2.3
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Set#set()
     */
    Aggregation<T> set(Set set);

    /**
     * @param fields the window fields
     * @return this
     * @since 2.3
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see SetWindowFields#setWindowFields()
     */
    Aggregation<T> setWindowFields(SetWindowFields fields);

    /**
     * Skips over the specified number of documents that pass into the stage and passes the remaining documents to the next stage in the
     * pipeline.
     *
     * @param skip the skip definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Skip#skip(long)
     */
    Aggregation<T> skip(long skip);

    /**
     * Sorts all input documents and returns them to the pipeline in sorted order.
     *
     * @param sort the sort definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Sort#sort()
     */
    Aggregation<T> sort(Sort sort);

    /**
     * Groups incoming documents based on the value of a specified expression, then computes the count of documents in each distinct group.
     * <p>
     * Each output document contains two fields: an _id field containing the distinct grouping value, and a count field containing the
     * number of documents belonging to that grouping or category.
     * <p>
     * The documents are sorted by count in descending order.
     *
     * @param sort the sort definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see SortByCount#sortByCount(Object)
     */
    Aggregation<T> sortByCount(Expression sort);

    /**
     * Performs a union of two collections; i.e. $unionWith combines pipeline results from two collections into a single result set. The
     * stage outputs the combined result set (including duplicates) to the next stage.
     *
     * @param type   the type to perform the pipeline against
     * @param stages the pipeline stages
     * @return this
     * @since 2.1
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see UnionWith#unionWith(Class, Stage...)
     */
    Aggregation<T> unionWith(Class<?> type, Stage... stages);

    /**
     * Performs a union of two collections; i.e. $unionWith combines pipeline results from two collections into a single result set. The
     * stage outputs the combined result set (including duplicates) to the next stage.
     *
     * @param collection the collection to perform the pipeline against
     * @param stages     the pipeline stages
     * @return this
     * @aggregation.stage $unionWith
     * @mongodb.server.release 4.4
     * @since 2.1
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see UnionWith#unionWith(String, Stage...)
     */
    Aggregation<T> unionWith(String collection, Stage... stages);

    /**
     * Removes/excludes fields from documents. Names must not start with '$'.
     *
     * @param unset the unset definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Unset#unset(String, String...)
     */
    Aggregation<T> unset(Unset unset);

    /**
     * Deconstructs an array field from the input documents to output a document for each element. Each output document is the input
     * document with the value of the array field replaced by the element.
     *
     * @param unwind the unwind definition
     * @return this
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see Unwind#unwind(String)
     */
    Aggregation<T> unwind(Unwind unwind);

    /**
     * Returns a Change Stream cursor on a collection, a database, or an entire cluster. Must be used as the first stage in an
     * aggregation pipeline.
     *
     * @return this
     * @since 2.3
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see ChangeStream#changeStream()
     */
    Aggregation changeStream();

    /**
     * Returns a Change Stream cursor on a collection, a database, or an entire cluster. Must be used as the first stage in an
     * aggregation pipeline.
     *
     * @param stream the options to apply to the stage
     * @return this
     * @since 2.3
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see ChangeStream#changeStream()
     */
    Aggregation changeStream(ChangeStream stream);
}
