package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.TestBase;
import dev.morphia.aggregation.experimental.AggregationTest.Artwork;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Stage;
import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.junit.Test;

import static dev.morphia.aggregation.experimental.expressions.Accumulator.sum;
import static dev.morphia.aggregation.experimental.expressions.Expression.field;
import static dev.morphia.aggregation.experimental.expressions.Expression.literal;
import static dev.morphia.aggregation.experimental.expressions.Expression.push;
import static org.bson.Document.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class CodecStructureTest extends TestBase {
    @Test
    public void testBucket() {
        Stage stage = Bucket.of()
                            .groupBy(field("price"))
                            .boundaries(literal(0), literal(150), literal(200), literal(300), literal(400))
                            .defaultValue("Other")
                            .outputField("count", sum(literal(1)))
                            .outputField("titles", push().single(field("title")));

        evaluate(stage, parse(
            "{ $bucket: { groupBy: '$price', boundaries: [  0, 150, 200, 300, 400 ], default: 'Other', output: { 'count': { $sum: 1 },"
            + "'titles': { $push: '$title' } } } }"));
    }

    private void evaluate(final Object value, final Document expected) {
        Codec codec = getMapper().getCodecRegistry().get(value.getClass());
        DocumentWriter writer = new DocumentWriter();
        codec.encode(writer, value, EncoderContext.builder().build());
        Document root = writer.getRoot();
        assertEquals(0, writer.getDocsLevel());
        assertEquals(0, writer.getArraysLevel());
        assertTrue(writer.getState().isEmpty());

        assertEquals(expected, root);
    }

    @Test
    public void testMatch() {
        Stage stage = Match.of(getDs().find(Artwork.class)
                                      .field("price").exists());

        evaluate(stage, parse("{ $match: { price: { $exists: true } } }"));
    }

    @Test
    public void testPush() {
        Expression expression = Expression.push()
                                          .field("item", field("item"))
                                          .field("quantity", field("quantity"));

        evaluate(expression, parse("{ $push:  { item: \"$item\", quantity: \"$quantity\" } }"));

        expression = Expression.push()
                               .single(field("title"));

        evaluate(expression, parse("{ $push: '$title' }"));
    }
}
