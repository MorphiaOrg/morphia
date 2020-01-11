package dev.morphia.aggregation.experimental.codecs.stages;

import com.mongodb.client.model.MergeOptions.WhenMatched;
import com.mongodb.client.model.MergeOptions.WhenNotMatched;
import dev.morphia.TestBase;
import dev.morphia.aggregation.experimental.AggregationTest.Artwork;
import dev.morphia.aggregation.experimental.GraphLookup;
import dev.morphia.aggregation.experimental.expressions.ArrayExpression;
import dev.morphia.aggregation.experimental.expressions.ConditionalExpression;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.MathExpression;
import dev.morphia.aggregation.experimental.expressions.ObjectExpressions;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.CollectionStats;
import dev.morphia.aggregation.experimental.stages.CurrentOp;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Merge;
import dev.morphia.aggregation.experimental.stages.Redact;
import dev.morphia.aggregation.experimental.stages.ReplaceWith;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.Skip;
import dev.morphia.aggregation.experimental.stages.SortByCount;
import dev.morphia.aggregation.experimental.stages.Unset;
import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.junit.Test;

import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.Accumulator.add;
import static dev.morphia.aggregation.experimental.expressions.Accumulator.sum;
import static dev.morphia.aggregation.experimental.expressions.ArrayExpression.array;
import static dev.morphia.aggregation.experimental.expressions.ArrayExpression.size;
import static dev.morphia.aggregation.experimental.expressions.Comparison.gt;
import static dev.morphia.aggregation.experimental.expressions.ConditionalExpression.condition;
import static dev.morphia.aggregation.experimental.expressions.Expression.field;
import static dev.morphia.aggregation.experimental.expressions.Expression.literal;
import static dev.morphia.aggregation.experimental.expressions.Expression.push;
import static dev.morphia.aggregation.experimental.expressions.SetExpression.setIntersection;
import static org.bson.Document.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class CodecStructureTest extends TestBase {
    @Test
    public void testBucket() {
        evaluate(parse("{ $bucket: { groupBy: '$price', boundaries: [  0, 150, 200, 300, 400 ], default: 'Other', output: { 'count': { "
                       + "$sum: 1 },'titles': { $push: '$title' } } } }"),
            Bucket.of()
                  .groupBy(field("price"))
                  .boundaries(literal(0), literal(150), literal(200), literal(300), literal(400))
                  .defaultValue("Other")
                  .outputField("count", sum(literal(1)))
                  .outputField("titles", push().single(field("title"))));
    }

    @SuppressWarnings("unchecked")
    private void evaluate(final Document expected, final Object value) {
        DocumentWriter writer = new DocumentWriter();
        ((Codec) getMapper().getCodecRegistry()
                            .get(value.getClass()))
            .encode(writer, value, EncoderContext.builder().build());
        Document actual = writer.getRoot();
        assertEquals(0, writer.getDocsLevel());
        assertEquals(0, writer.getArraysLevel());
        assertTrue(writer.getState().isEmpty());

        assertDocumentEquals(expected, actual);
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
    public void testGraphLookup() {
        Document document = parse("{$graphLookup: {from: 'employees',startWith: '$reportsTo',connectFromField: 'reportsTo',"
                                   + "connectToField: 'name',as: 'reportingHierarchy' }}");
        evaluate(document,
            GraphLookup.with()
                       .from("employees")
                       .startWith(field("reportsTo"))
                       .connectFromField("reportsTo")
                       .connectToField("name")
                       .as("reportingHierarchy"));
    }

    @Test
    public void testMatch() {
        evaluate(parse("{ $match: { price: { $exists: true } } }"),
            Match.on(getDs().find(Artwork.class)
                            .field("price").exists()));
    }

    @Test
    public void testPush() {
        evaluate(parse("{ $push:  { item: \"$item\", quantity: \"$quantity\" } }"),
            Expression.push()
                      .field("item", field("item"))
                      .field("quantity", field("quantity")));

        evaluate(parse("{ $push: '$title' }"),
            Expression.push()
                      .single(field("title")));
    }

    @Test
    public void testReplaceWith() {
        evaluate(parse("{ $replaceWith: \"$grades\" }"),
            ReplaceWith.with()
                       .value(field("grades")));

        evaluate(parse("{ $replaceWith: { _id: '$_id', item: '$item', amount: { $multiply: [ '$price', '$quantity']}, status: 'Complete', "
                       + "asofDate: '$$NOW' } }"),
            ReplaceWith.with()
                       .field("_id", field("_id"))
                       .field("item", field("item"))
                       .field("amount", MathExpression.multiply(field("price"), field("quantity")))
                       .field("status", literal("Complete"))
                       .field("asofDate", literal("$$NOW"))
                );
    }

    @Test
    public void testSample() {
        evaluate(parse("{ $sample : { size: 15 } }"),
            Sample.of(15));
    }

    @Test
    public void testSkip() {
        evaluate(parse("{ $skip : 15 }"),
            Skip.of(15));
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

    @Test
    public void testIfNull() {
        evaluate(parse("{ $ifNull: [ \"$name\", { _id: \"$_id\", missingName: true} ] }"),
            ConditionalExpression.ifNull()
            .target(field("name"))
            .field("_id", field("_id"))
            .field("missingName", literal(true)));
    }

    @Test
    public void testMergeObjects() {
        evaluate(parse("{ $mergeObjects: [ { $arrayElemAt: [ \"$fromItems\", 0 ] }, \"$$ROOT\" ] } "),
            ObjectExpressions.mergeObjects()
                .add(ArrayExpression.elementAt(field("fromItems"), literal(0)))
                .add(literal("$$ROOT")));
    }

    @Test
    public void testMerge() {
        evaluate(parse("{ $merge : { into: { db: 'reporting', coll: 'budgets' }, on: '_id',  whenMatched: 'replace', "
                       + "whenNotMatched: 'insert' } }"),
            Merge.merge()
                 .into("reporting", "budgets")
                 .on("_id")
                 .whenMatched(WhenMatched.REPLACE)
                 .whenNotMatched(WhenNotMatched.INSERT));


        evaluate(parse("{ $merge: { into: 'monthlytotals', on: '_id', whenMatched:  [ { $addFields: { thumbsup: { $add:[ '$thumbsup', "
                       + "'$$new.thumbsup' ] }, thumbsdown: { $add: [ '$thumbsdown', '$$new.thumbsdown' ] } } } ], whenNotMatched: "
                       + "'insert' } }"),
            Merge.merge()
                 .into("monthlytotals")
                 .on("_id")
                 .whenMatched(List.of(
                     AddFields.of()
                              .field("thumbsup", add(field("thumbsup"), literal("$$new.thumbsup")))
                              .field("thumbsdown", add(field("$thumbsdown"), literal("$$new.thumbsdown")))))
                .whenNotMatched(WhenNotMatched.INSERT));
    }

    @Test
    public void testRedact() {
        evaluate(parse("{ $redact: { $cond: [ { $gt: [ { $size: { $setIntersection: [ '$tags', [ 'STLW', 'G' ] ] } }, 0 ] }, "
                       + "'$$DESCEND', '$$PRUNE']}}"),
            Redact.on(condition(
                gt(size(setIntersection(field("tags"), array(literal("STLW"), literal("G")))), literal(0)),
                literal("$$DESCEND"),
                literal("$$PRUNE"))));
    }

}
