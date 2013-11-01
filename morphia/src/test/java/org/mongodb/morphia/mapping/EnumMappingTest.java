package org.mongodb.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PreSave;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class EnumMappingTest extends TestBase {
    public static class ContainsEnum {
        @Id
        private ObjectId id;
        private Foo foo = Foo.BAR;

        @PreSave
        void testMapping() {

        }
    }

    enum Foo {
        BAR() {
        },
        BAZ
    }

    @Test
    public void testEnumMapping() throws Exception {
        getMorphia().map(ContainsEnum.class);

        getDs().save(new ContainsEnum());
        Assert.assertEquals(1, getDs().createQuery(ContainsEnum.class).field("foo").equal(Foo.BAR).countAll());
        Assert.assertEquals(1, getDs().createQuery(ContainsEnum.class).filter("foo", Foo.BAR).countAll());
        Assert.assertEquals(1, getDs().createQuery(ContainsEnum.class).disableValidation().filter("foo", Foo.BAR).countAll());
    }

}
