package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.TestBase;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.testmodel.User;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.junit.Assert;
import org.junit.Before;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpressionsTest extends TestBase {
    @Before
    public void seed() {
        getMapper().getCollection(User.class).drop();
        getDs().save(new User("", new Date()));
    }

    @SuppressWarnings("unchecked")
    protected void evaluate(final String expectedString, final Expression value, final Object expectedValue) {
        Document expected = Document.parse(expectedString);
        DocumentWriter writer = new DocumentWriter();
        ((Codec) getMapper().getCodecRegistry()
                            .get(MathExpression.class))
            .encode(writer, value, EncoderContext.builder().build());
        Document actual = writer.getRoot();
        assertEquals(0, writer.getDocsLevel());
        assertEquals(0, writer.getArraysLevel());
        assertTrue(writer.getState().isEmpty());
        assertDocumentEquals(expected, actual);

        Document test = getDs().aggregate(User.class)
                               .project(Projection.of()
                                                  .include("test", value))
                               .execute(Document.class)
                               .next();
        Assert.assertEquals(expectedValue, test.get("test"));
    }
}
