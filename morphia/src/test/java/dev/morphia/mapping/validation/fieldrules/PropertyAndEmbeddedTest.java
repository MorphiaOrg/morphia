package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.PreSave;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Transient;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.testutil.TestEntity;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class PropertyAndEmbeddedTest extends TestBase {
    @Test(expected = ConstraintViolationException.class)
    public void testCheck() {

        final E e = new E();
        getDs().save(e);

        Assert.assertTrue(e.document.contains("myFunkyR"));

        getMapper().map(E2.class);
    }

    public static class E extends TestEntity {
        @Property("myFunkyR")
        private R r = new R();
        @Transient
        private String document;

        @PreSave
        public void preSave(final Document o) {
            document = o.toString();
        }
    }

    public static class E2 extends TestEntity {
        @Property("myFunkyR")
        private String s;
    }

    @Embedded
    public static class R {
        private String foo = "bar";
    }
}
