package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.TestBase;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.testmodel.User;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

import static dev.morphia.aggregation.experimental.expressions.Expression.literal;
import static dev.morphia.aggregation.experimental.expressions.Expression.nullExpression;
import static org.bson.Document.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MathExpressionTest extends TestBase {

    @Test
    public void abs() {
        evaluate(parse("{ $abs: -1 }"), MathExpression.abs(literal(-1)), 1);
        evaluate(parse("{ $abs: 1 }"), MathExpression.abs(literal(1)), 1);
        evaluate(parse("{ $abs: null }"), MathExpression.abs(nullExpression()), null);
    }

    @Test
    public void add() {
        evaluate(parse("{ $add: [ 4, 5 ] } "), MathExpression.add(literal(4), literal(5)), 9);
    }

    @Test
    public void ceil() {
        evaluate(parse("{ $ceil: 7.80 }"), MathExpression.ceil(literal(7.80)), 8.0);
    }

    @Test
    public void divide() {
        evaluate(parse("{ $divide: [ 16, 8 ] } }"), MathExpression.divide(literal(16), literal(8)), 2.0);
    }

    @Test
    public void exp() {
        evaluate(parse("{ $exp: 0 } "), MathExpression.exp(literal(0)), 1.0);
    }

    @Test
    public void floor() {
        evaluate(parse("{ $floor: 1.5 }"), MathExpression.floor(literal(1.5)), 1.0);
    }

    @Test
    public void ln() {
        evaluate(parse("{ $ln: 1 }"), MathExpression.ln(literal(1)), 0.0);
    }

    @Test
    public void log() {
        evaluate(parse("{ $log: [ 100, 10 ] }"), MathExpression.log(literal(100), literal(10)), 2.0);
    }

    @Test
    public void log10() {
        evaluate(parse("{ $log10: 100 }"), MathExpression.log10(literal(100)), 2.0);
    }

    @Test
    public void mod() {
        evaluate(parse("{ $mod: [ 12, 5 ] }"), MathExpression.mod(literal(12), literal(5)), 2);
    }

    @Test
    public void multiply() {
        evaluate(parse("{ $multiply: [ 3, 4, 5 ] }"), MathExpression.multiply(literal(3), literal(4), literal(5)), 60);
    }

    @Test
    public void pow() {
        evaluate(parse("{ $pow: [ 5, 2 ] }"), MathExpression.pow(literal(5), literal(2)), 25);
    }

    @Test
    public void round() {
        evaluate(parse("{ $round: [ 19.25, 1 ] }"), MathExpression.round(literal(19.25), literal(1)), 19.2);
    }

    @Test
    public void sqrt() {
        evaluate(parse("{ $sqrt: 25 }"), MathExpression.sqrt(literal(25)), 5.0);
    }

    @Test
    public void subtract() {
        evaluate(parse("{ $subtract: [ { $add: [ 4 , 5 ] }, 6 ] }"),
            MathExpression.subtract(MathExpression.add(literal(4), literal(5)), literal(6)), 3);
    }

    @Test
    public void trunc() {
        evaluate(parse("{ $trunc: [ 7.85, 1 ] }"), MathExpression.trunc(literal(7.85), literal(1)), 7.8);
    }

    @SuppressWarnings("unchecked")
    private void evaluate(final Document expected, final Expression value, final Object expectedValue) {
        DocumentWriter writer = new DocumentWriter();
        ((Codec) getMapper().getCodecRegistry()
                            .get(MathExpression.class))
            .encode(writer, value, EncoderContext.builder().build());
        Document actual = writer.getRoot();
        assertEquals(0, writer.getDocsLevel());
        assertEquals(0, writer.getArraysLevel());
        assertTrue(writer.getState().isEmpty());

        assertDocumentEquals(expected, actual);

        getMapper().getCollection(User.class).drop();
        getDs().save(new User("", new Date()));
        Document test = getDs().aggregate(User.class)
                               .project(Projection.of()
                                                  .include("test", value))
                               .execute(Document.class)
                               .next();
        Assert.assertEquals(expectedValue, test.get("test"));
    }
}