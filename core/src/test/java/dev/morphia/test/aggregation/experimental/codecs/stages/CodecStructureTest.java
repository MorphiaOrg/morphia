package dev.morphia.test.aggregation.experimental.codecs.stages;

import com.mongodb.client.model.MergeOptions.WhenMatched;
import com.mongodb.client.model.MergeOptions.WhenNotMatched;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions;
import dev.morphia.aggregation.experimental.expressions.ConditionalExpressions;
import dev.morphia.aggregation.experimental.expressions.MathExpressions;
import dev.morphia.aggregation.experimental.expressions.ObjectExpressions;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.CollectionStats;
import dev.morphia.aggregation.experimental.stages.CurrentOp;
import dev.morphia.aggregation.experimental.stages.GraphLookup;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Merge;
import dev.morphia.aggregation.experimental.stages.Redact;
import dev.morphia.aggregation.experimental.stages.ReplaceWith;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.Skip;
import dev.morphia.aggregation.experimental.stages.SortByCount;
import dev.morphia.aggregation.experimental.stages.Unset;
import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.test.TestBase;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.experimental.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.experimental.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.experimental.expressions.ArrayExpressions.size;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.experimental.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.aggregation.experimental.expressions.MathExpressions.add;
import static dev.morphia.aggregation.experimental.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.experimental.expressions.SystemVariables.DESCEND;
import static dev.morphia.aggregation.experimental.expressions.SystemVariables.NOW;
import static dev.morphia.aggregation.experimental.expressions.SystemVariables.PRUNE;
import static dev.morphia.aggregation.experimental.expressions.SystemVariables.ROOT;
import static dev.morphia.aggregation.experimental.stages.GeoNear.to;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.exists;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;


public class CodecStructureTest extends TestBase {
    @Test
    public void testBucket() {
        evaluate(parse("{ $bucket: { groupBy: '$price', boundaries: [  0, 150, 200, 300, 400 ], default: 'Other', output: { 'count': { "
                       + "$sum: 1 },'titles': { $push: '$title' } } } }"),
            Bucket.of()
                  .groupBy(field("price"))
                  .boundaries(value(0), value(150), value(200), value(300), value(400))
                  .defaultValue("Other")
                  .outputField("count", sum(value(1)))
                  .outputField("titles", push().single(field("title"))));
    }

    @Test
    public void testGeoNear() {
        evaluate(parse("{ $geoNear: { near: { type: 'Point', coordinates: [ -73.98142 , 40.71782 ] }, key: 'location', distanceField: "
                       + "'dist.calculated', query: { 'category': 'Parks' } } }"),
            to(new Point(new Position(-73.98142, 40.71782)))
                .key("location")
                .distanceField("dist.calculated")
                .query(eq("category", "Parks")));
    }

    @Test
    public void testCollectionStats() {
        evaluate(parse("{ $collStats: { latencyStats: { histograms: true }, storageStats: { scale: 42 }, count: {} } }"),
            CollectionStats.with()
                           .histogram(true)
                           .scale(42)
                           .count(true));

    }

    @Test
    public void testCurrentOp() {
        evaluate(parse("{ $currentOp: { allUsers: true, idleConnections: true, idleCursors: true, idleSessions: true, localOps: true } }"),
            CurrentOp.of()
                     .allUsers(true)
                     .idleConnections(true)
                     .idleCursors(true)
                     .idleSessions(true)
                     .localOps(true));
        evaluate(parse("{ $currentOp: { idleConnections: true, idleCursors: true, idleSessions: true, localOps: true } }"),
            CurrentOp.of()
                     .idleConnections(true)
                     .idleCursors(true)
                     .idleSessions(true)
                     .localOps(true));
        evaluate(parse("{ $currentOp: { idleCursors: true, idleSessions: true, localOps: true } }"),
            CurrentOp.of()
                     .idleCursors(true)
                     .idleSessions(true)
                     .localOps(true));
        evaluate(parse("{ $currentOp: { idleSessions: true, localOps: true } }"),
            CurrentOp.of()
                     .idleSessions(true)
                     .localOps(true));
        evaluate(parse("{ $currentOp: { localOps: true } }"),
            CurrentOp.of()
                     .localOps(true));
        evaluate(parse("{ $currentOp: {  } }"),
            CurrentOp.of());
    }

    @Test
    public void testMatch() {
        evaluate(parse("{ $match: { price: { $exists: true } } }"),
            Match.on(exists("price")));
    }

    @Test
    public void testGraphLookup() {
        Document document = parse("{$graphLookup: {from: 'employees',startWith: '$reportsTo',connectFromField: 'reportsTo',"
                                  + "connectToField: 'name',as: 'reportingHierarchy' }}");
        evaluate(document,
            GraphLookup.from("employees")
                       .startWith(field("reportsTo"))
                       .connectFromField("reportsTo")
                       .connectToField("name")
                       .as("reportingHierarchy"));
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
    public void testSample() {
        DocumentWriter writer = new DocumentWriter();
        getMapper().getCodecRegistry()
                   .get(Sample.class)
                   .encode(writer, Sample.of(15L), EncoderContext.builder().build());
        Document actual = writer.getDocument();
        assertEquals(writer.getDocsLevel(), 0);
        assertEquals(writer.getArraysLevel(), 0);
        assertEquals(((Document) actual.get("$sample")).getLong("size").longValue(), 15L);
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
                     AddFields.of()
                              .field("thumbsup", add(field("thumbsup"), value("$$new.thumbsup")))
                              .field("thumbsdown", add(field("$thumbsdown"), value("$$new.thumbsdown")))))
                 .whenNotMatched(WhenNotMatched.INSERT));
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
    public void testRedact() {
        evaluate(parse("{ $redact: { $cond: [ { $gt: [ { $size: { $setIntersection: [ '$tags', [ 'STLW', 'G' ] ] } }, 0 ] }, "
                       + "'$$DESCEND', '$$PRUNE']}}"),
            Redact.on(condition(
                gt(size(setIntersection(field("tags"), array(value("STLW"), value("G")))), value(0)),
                DESCEND, PRUNE)));
    }

    @Test
    public void testReplaceWith() {
        evaluate(parse("{ $replaceWith: \"$grades\" }"),
            ReplaceWith.with(field("grades")));

        evaluate(parse("{ $replaceWith: { _id: '$_id', item: '$item', amount: { $multiply: [ '$price', '$quantity']}, status: 'Complete', "
                       + "asofDate: '$$NOW' } }"),
            ReplaceWith.with()
                       .field("_id", field("_id"))
                       .field("item", field("item"))
                       .field("amount", MathExpressions.multiply(field("price"), field("quantity")))
                       .field("status", value("Complete"))
                       .field("asofDate", NOW)
                );
    }

    @Test
    public void testSkip() {
        DocumentWriter writer = new DocumentWriter();
        getMapper().getCodecRegistry()
                   .get(Skip.class)
                   .encode(writer, Skip.of(15L), EncoderContext.builder().build());
        Document actual = writer.getDocument();
        assertEquals(writer.getDocsLevel(), 0);
        assertEquals(writer.getArraysLevel(), 0);
        assertEquals(actual.getLong("$skip").longValue(), 15L);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void evaluate(Document expected, Object value) {
        DocumentWriter writer = new DocumentWriter();
        ((Codec) getMapper().getCodecRegistry()
                            .get(value.getClass()))
            .encode(writer, value, EncoderContext.builder().build());
        Document actual = writer.getDocument();
        assertEquals(writer.getDocsLevel(), 0);
        assertEquals(writer.getArraysLevel(), 0);

        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testSortByCount() {
        evaluate(parse("{ $sortByCount: \"$tags\" }"),
            SortByCount.on(field("tags")));
    }

    @Test
    public void testUnset() {
        evaluate(parse("{ $unset:  'single' }"),
            Unset.fields("single"));

        evaluate(parse("{ $unset:  [\"more\", \"than\", \"one\"] }"),
            Unset.fields("more", "than", "one"));
    }

    @Test
    public void testUnwind() {
        evaluate(parse("{ $unwind : \"$sizes\" }"),
            Unwind.on("sizes"));
    }

}
