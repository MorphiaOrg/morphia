package org.mongodb.morphia.mapping.validation.fieldrules;


import com.mongodb.DBObject;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.PreSave;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class SerializedNameTest extends TestBase {
    @Test
    public void testCheck() {

        final E e = new E();
        getDs().save(e);

        Assert.assertTrue(e.document.contains("changedName"));
    }

    public static class E extends TestEntity {
        @Serialized("changedName")
        private final byte[] b = "foo".getBytes();
        @Transient
        private String document;

        @PreSave
        public void preSave(final DBObject o) {
            document = o.toString();
        }
    }
}
