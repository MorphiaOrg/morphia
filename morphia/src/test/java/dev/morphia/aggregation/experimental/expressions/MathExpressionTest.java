package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.TestBase;
import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.junit.Test;

import static dev.morphia.aggregation.experimental.expressions.Expression.field;
import static dev.morphia.aggregation.experimental.expressions.Expression.literal;
import static dev.morphia.aggregation.experimental.expressions.Expression.nullExpression;
import static org.bson.Document.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MathExpressionTest extends TestBase {

    @Test
    public void abs() {
        evaluate(parse("{ $abs: -1 }"), MathExpression.abs(literal(-1)));
        evaluate(parse("{ $abs: 1 }"), MathExpression.abs(literal(1)));
        evaluate(parse("{ $abs: null }"), MathExpression.abs(nullExpression()));
    }

    @Test
    public void add() {
        evaluate(parse("{ $add: [ \"$price\", \"$fee\" ] } "), MathExpression.add(field("price"), field("fee")));
    }

    @Test
    public void ceil() {
        evaluate(parse("{ $ceil: 1 }"), MathExpression.ceil(literal(1)));
        evaluate(parse("{ $ceil: 7.80 }"), MathExpression.ceil(literal(7.80)));
        evaluate(parse("{ $ceil: -2.8 }"), MathExpression.ceil(literal(-2.8)));
    }

    @Test
    public void divide() {
        evaluate(parse("{ $divide: [ \"$hours\", 8 ] } }"), MathExpression.divide(field("hours"), literal(8)));
    }

    @Test
    public void exp() {
        evaluate(parse("{ $exp: 0 } "), MathExpression.exp(literal(0)));
        evaluate(parse("{ $exp: 2 }"), MathExpression.exp(literal(2)));
    }

    @Test
    public void floor() {
        evaluate(parse("{ $floor: 1 }"), MathExpression.floor(literal(1)));
        evaluate(parse("{ $floor: 7.80 }"), MathExpression.floor(literal(7.80)));
        evaluate(parse("{ $floor: -2.8 }"), MathExpression.floor(literal(-2.8)));
    }

    @Test
    public void ln() {
        evaluate(parse("{ $ln: 1 }"), MathExpression.ln(literal(1)));
        evaluate(parse("{ $ln: 10  }"), MathExpression.ln(literal(10)));
    }

    @Test
    public void log() {
        evaluate(parse("{ $log: [ 100, 10 ] }"), MathExpression.log(literal(100), literal(10)));
    }

    @Test
    public void log10() {
        evaluate(parse("{ $log10: 100 }"), MathExpression.log10(literal(100)));
    }

    @Test
    public void mod() {
        evaluate(parse("{ $mod: [ \"$hours\", \"$tasks\" ] }"), MathExpression.mod(field("hours"), field("tasks")));
    }

    @Test
    public void multiply() {
        evaluate(parse("{ $multiply: [ \"$hours\", \"$tasks\", 25.2 ] }"), MathExpression.multiply(field("hours"), field("tasks"),
            literal(25.2)));
    }

    @Test
    public void pow() {
        evaluate(parse("{ $pow: [ 5, 0 ] }"), MathExpression.pow(literal(5), literal(0)));
    }

    @Test
    public void round() {
        evaluate(parse("{ $round: [ 17.3, 1 ] }"), MathExpression.round(literal(17.3), literal(1)));
        evaluate(parse("{ $round: [ null, 1 ] }"), MathExpression.round(nullExpression(), literal(1)));
    }

    @Test
    public void sqrt() {
        evaluate(parse("{ $sqrt: 100 }"), MathExpression.sqrt(literal(100)));
    }

    @Test
    public void subtract() {
        evaluate(parse("{ $subtract: [ { $add: [ \"$price\", \"$fee\" ] }, \"$discount\" ] }"),
            MathExpression.subtract(MathExpression.add(field("price"), field("fee")), field("discount")));
    }

    @Test
    public void trunc() {
        evaluate(parse("{ $trunc: [ 7.80, 1 ] }"), MathExpression.trunc(literal(7.80), literal(1)));
    }

    @SuppressWarnings("unchecked")
    private void evaluate(final Document expected, final Expression value) {
        DocumentWriter writer = new DocumentWriter();
        ((Codec) getMapper().getCodecRegistry()
                            .get(MathExpression.class))
            .encode(writer, value, EncoderContext.builder().build());
        Document actual = writer.getRoot();
        assertEquals(0, writer.getDocsLevel());
        assertEquals(0, writer.getArraysLevel());
        assertTrue(writer.getState().isEmpty());

        assertDocumentEquals(expected, actual);
    }
}