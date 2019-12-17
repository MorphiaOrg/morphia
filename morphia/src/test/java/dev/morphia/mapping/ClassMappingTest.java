package dev.morphia.mapping;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.query.FindOptions;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ClassMappingTest extends TestBase {

    @Test
    public void testClassQueries() {
        E e = new E();

        e.testClass2 = LinkedList.class;
        getDs().save(e);

        Assert.assertNull(getDs().find(E.class)
                                 .field("testClass2").equal(ArrayList.class)
                                 .execute(new FindOptions()
                                              .limit(1))
                                 .tryNext());
    }

    @Test
    public void testMapping() {
        E e = new E();

        e.testClass = LinkedList.class;
        getDs().save(e);

        e = getDs().find(E.class)
                   .filter("_id", e.id)
                   .first();
        Assert.assertEquals(LinkedList.class, e.testClass);
    }

    @Entity
    public static class E {
        @Id
        private ObjectId id;

        @Property
        private Class<? extends Collection> testClass;
        private Class<? extends Collection> testClass2;
    }
}
