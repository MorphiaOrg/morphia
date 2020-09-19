package dev.morphia.test.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.MathExpression;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.User;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ExpressionsTestBase extends TestBase {
    @BeforeEach
    public void seed() {
        getMapper().getCollection(User.class).drop();
        getDs().save(new User("", LocalDate.now()));
    }

    @SuppressWarnings("unchecked")
    protected void assertAndCheckDocShape(String expectedString, Expression value, Object expectedValue) {
        Document expected = Document.parse(expectedString);
        DocumentWriter writer = new DocumentWriter();
        ((Codec) getMapper().getCodecRegistry()
                            .get(MathExpression.class))
            .encode(writer, value, EncoderContext.builder().build());
        Document actual = writer.getDocument();
        assertEquals(0, writer.getDocsLevel());
        assertEquals(0, writer.getArraysLevel());
        assertDocumentEquals(expected, actual);

        Document test = getDs().aggregate(User.class)
                               .project(Projection.of()
                                                  .include("test", value))
                               .execute(Document.class)
                               .next();
        assertEquals(expectedValue, test.get("test"));
    }
}
