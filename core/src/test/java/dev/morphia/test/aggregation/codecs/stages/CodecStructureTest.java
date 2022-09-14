package dev.morphia.test.aggregation.codecs.stages;

import com.mongodb.client.model.MergeOptions.WhenMatched;
import com.mongodb.client.model.MergeOptions.WhenNotMatched;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.aggregation.expressions.AccumulatorExpressions;
import dev.morphia.aggregation.expressions.ConditionalExpressions;
import dev.morphia.aggregation.expressions.MathExpressions;
import dev.morphia.aggregation.expressions.ObjectExpressions;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.AddFields;
import dev.morphia.aggregation.stages.Bucket;
import dev.morphia.aggregation.stages.CollectionStats;
import dev.morphia.aggregation.stages.CurrentOp;
import dev.morphia.aggregation.stages.GeoNear;
import dev.morphia.aggregation.stages.GraphLookup;
import dev.morphia.aggregation.stages.Match;
import dev.morphia.aggregation.stages.Merge;
import dev.morphia.aggregation.stages.Redact;
import dev.morphia.aggregation.stages.ReplaceWith;
import dev.morphia.aggregation.stages.Sample;
import dev.morphia.aggregation.stages.Skip;
import dev.morphia.aggregation.stages.SortByCount;
import dev.morphia.aggregation.stages.Unset;
import dev.morphia.aggregation.stages.Unwind;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.test.TestBase;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.ArrayExpressions.size;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.expressions.SystemVariables.DESCEND;
import static dev.morphia.aggregation.expressions.SystemVariables.NOW;
import static dev.morphia.aggregation.expressions.SystemVariables.PRUNE;
import static dev.morphia.aggregation.expressions.SystemVariables.ROOT;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;

public class CodecStructureTest extends TestBase {
    @Test
    public void testBucket() {
        evaluate(parse("{ $bucket: { groupBy: '$price', boundaries: [  0, 150, 200, 300, 400 ], default: 'Other', output: { 'count': { "
                + "$sum: 1 },'titles': { $push: '$title' } } } }"),
                Bucket.bucket()
                        .groupBy(field("price"))
                        .boundaries(value(0), value(150), value(200), value(300), value(400))
                        .defaultValue("Other")
                        .outputField("count", sum(value(1)))
                        .outputField("titles", push().single(field("title"))));
    }

    @Test
    public void testCollectionStats() {
        evaluate(parse("{ $collStats: { latencyStats: { histograms: true }, storageStats: { scale: 42 }, count: {} } }"),
                CollectionStats.collStats()
                        .histogram(true)
                        .scale(42)
                        .count(true));

    }

    @Test
    public void testCurrentOp() {
        evaluate(parse("{ $currentOp: { allUsers: true, idleConnections: true, idleCursors: true, idleSessions: true, localOps: true } }"),
                CurrentOp.currentOp()
                        .allUsers(true)
                        .idleConnections(true)
                        .idleCursors(true)
                        .idleSessions(true)
                        .localOps(true));
        evaluate(parse("{ $currentOp: { idleConnections: true, idleCursors: true, idleSessions: true, localOps: true } }"),
                CurrentOp.currentOp()
                        .idleConnections(true)
                        .idleCursors(true)
                        .idleSessions(true)
                        .localOps(true));
        evaluate(parse("{ $currentOp: { idleCursors: true, idleSessions: true, localOps: true } }"),
                CurrentOp.currentOp()
                        .idleCursors(true)
                        .idleSessions(true)
                        .localOps(true));
        evaluate(parse("{ $currentOp: { idleSessions: true, localOps: true } }"),
                CurrentOp.currentOp()
                        .idleSessions(true)
                        .localOps(true));
        evaluate(parse("{ $currentOp: { localOps: true } }"),
                CurrentOp.currentOp()
                        .localOps(true));
        evaluate(parse("{ $currentOp: {  } }"),
                CurrentOp.currentOp());
    }

    @Test
    public void testGeoNear() {
        evaluate(parse("{ $geoNear: { near: { type: 'Point', coordinates: [ -73.98142 , 40.71782 ] }, key: 'location', distanceField: "
                + "'dist.calculated', query: { 'category': 'Parks' } } }"),
                GeoNear.geoNear(new Point(new Position(-73.98142, 40.71782)))
                        .key("location")
                        .distanceField("dist.calculated")
                        .query(eq("category", "Parks")));
    }

    @Test
    public void testGraphLookup() {
        Document document = parse("{$graphLookup: {from: 'employees',startWith: '$reportsTo',connectFromField: 'reportsTo',"
                + "connectToField: 'name',as: 'reportingHierarchy' }}");
        evaluate(document,
                GraphLookup.graphLookup("employees")
                        .startWith(field("reportsTo"))
                        .connectFromField("reportsTo")
                        .connectToField("name")
                        .as("reportingHierarchy"));
    }

    @Test
    public void testMatch() {
        evaluate(parse("{ $match: { price: { $exists: true } } }"),
                Match.match(exists("price")));
    }

    @Test
    public void testIfNull() {
        evaluate(parse("{ $ifNull: [ \"$name\", { _id: \"$_id\", missingName: true} ] }"),
                ConditionalExpressions.ifNull()
                        .target(field("name"))
                        .field("_id", field("_id"))
                        .field("missingName", value(true)));
    }

    @Test
    public void testMerge() {
        evaluate(parse("{ $merge : { into: { db: 'reporting', coll: 'budgets' }, on: '_id',  whenMatched: 'replace', "
                + "whenNotMatched: 'insert' } }"),
                Merge.into("reporting", "budgets")
                        .on("_id")
                        .whenMatched(WhenMatched.REPLACE)
                        .whenNotMatched(WhenNotMatched.INSERT));

        evaluate(parse("{ $merge: { into: 'monthlytotals', on: '_id', whenMatched:  [ { $addFields: { thumbsup: { $add:[ '$thumbsup', "
                + "'$$new.thumbsup' ] }, thumbsdown: { $add: [ '$thumbsdown', '$$new.thumbsdown' ] } } } ], whenNotMatched: "
                + "'insert' } }"),
                Merge.into("monthlytotals")
                        .on("_id")
                        .whenMatched(List.of(
                                AddFields.addFields()
                                        .field("thumbsup", add(field("thumbsup"), value("$$new.thumbsup")))
                                        .field("thumbsdown", add(field("$thumbsdown"), value("$$new.thumbsdown")))))
                        .whenNotMatched(WhenNotMatched.INSERT));
    }

    @Test
    public void testRedact() {
        evaluate(parse("{ $redact: { $cond: [ { $gt: [ { $size: { $setIntersection: [ '$tags', [ 'STLW', 'G' ] ] } }, 0 ] }, "
                + "'$$DESCEND', '$$PRUNE']}}"),
                Redact.redact(condition(
                        gt(size(setIntersection(field("tags"), array(value("STLW"), value("G")))), value(0)),
                        DESCEND, PRUNE)));
    }

    @Test
    public void testMergeObjects() {
        evaluate(parse("{ $mergeObjects: [ { $arrayElemAt: [ \"$fromItems\", 0 ] }, \"$$ROOT\" ] } "),
                ObjectExpressions.mergeObjects()
                        .add(elementAt(field("fromItems"), value(0)))
                        .add(ROOT));
    }

    @Test
    public void testPush() {
        evaluate(parse("{ $push:  { item: \"$item\", quantity: \"$quantity\" } }"),
                AccumulatorExpressions.push()
                        .field("item", field("item"))
                        .field("quantity", field("quantity")));

        evaluate(parse("{ $push: '$title' }"),
                AccumulatorExpressions.push()
                        .single(field("title")));
    }

    @Test
    public void testReplaceWith() {
        evaluate(parse("{ $replaceWith: \"$grades\" }"),
                ReplaceWith.replaceWith(field("grades")));

        evaluate(parse("{ $replaceWith: { _id: '$_id', item: '$item', amount: { $multiply: [ '$price', '$quantity']}, status: 'Complete', "
                + "asofDate: '$$NOW' } }"),
                ReplaceWith.replaceWith()
                        .field("_id", field("_id"))
                        .field("item", field("item"))
                        .field("amount", MathExpressions.multiply(field("price"), field("quantity")))
                        .field("status", value("Complete"))
                        .field("asofDate", NOW));
    }

    @Test
    public void testSample() {
        DocumentWriter writer = new DocumentWriter(getMapper());
        getDs().getCodecRegistry()
                .get(Sample.class)
                .encode(writer, Sample.sample(15L), EncoderContext.builder().build());
        Document actual = writer.getDocument();
        assertEquals(((Document) actual.get("$sample")).getLong("size").longValue(), 15L);
    }

    @Test
    public void testSkip() {
        DocumentWriter writer = new DocumentWriter(getMapper());
        getDs().getCodecRegistry()
                .get(Skip.class)
                .encode(writer, Skip.skip(15L), EncoderContext.builder().build());
        Document actual = writer.getDocument();
        assertEquals(actual.getLong("$skip").longValue(), 15L);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void evaluate(Document expected, Object value) {
        DocumentWriter writer = new DocumentWriter(getMapper());
        ((Codec) getDs().getCodecRegistry()
                .get(value.getClass()))
                .encode(writer, value, EncoderContext.builder().build());
        Document actual = writer.getDocument();

        assertDocumentEquals(actual, expected);
    }

    private void evaluate(Document expected, Expression value) {
        DocumentWriter writer = new DocumentWriter(getMapper());
        document(writer, () -> {
            value.encode(getDs(), writer, EncoderContext.builder().build());
        });
        Document actual = writer.getDocument();

        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testSortByCount() {
        evaluate(parse("{ $sortByCount: \"$tags\" }"),
                SortByCount.sortByCount(field("tags")));
    }

    @Test
    public void testUnset() {
        evaluate(parse("{ $unset:  'single' }"),
                Unset.unset("single"));

        evaluate(parse("{ $unset:  [\"more\", \"than\", \"one\"] }"),
                Unset.unset("more", "than", "one"));
    }

    @Test
    public void testUnwind() {
        evaluate(parse("{ $unwind : \"$sizes\" }"),
                Unwind.unwind("sizes"));
    }

}
