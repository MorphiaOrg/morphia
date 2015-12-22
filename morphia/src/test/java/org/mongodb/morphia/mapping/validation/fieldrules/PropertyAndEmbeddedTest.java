package org.mongodb.morphia.mapping.validation.fieldrules;


import com.mongodb.DBObject;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.PreSave;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class PropertyAndEmbeddedTest extends TestBase {
    @Test(expected = ConstraintViolationException.class)
    public void testCheck() {

        final E e = new E();
        getDs().save(e);

        Assert.assertTrue(e.document.contains("myFunkyR"));

        getMorphia().map(E2.class);
    }

    public static class E extends TestEntity {
        @Embedded("myFunkyR")
        private R r = new R();
        @Transient
        private String document;

        @PreSave
        public void preSave(final DBObject o) {
            document = o.toString();
        }
    }

    public static class E2 extends TestEntity {
        @Embedded
        @Property("myFunkyR")
        private String s;
    }

    public static class R {
        private String foo = "bar";
    }
}
