package dev.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.query.FindOptions;

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

        Assert.assertNull(getDs().find(E.class).field("testClass2").equal(ArrayList.class).find(new FindOptions().limit(1)).tryNext());
    }

    @Test
    public void testMapping() {
        E e = new E();

        e.testClass = LinkedList.class;
        getDs().save(e);

        e = getDs().get(e);
        Assert.assertEquals(LinkedList.class, e.testClass);
    }

    @Test
    public void testMappingWithoutAnnotation() {
        E e = new E();

        e.testClass2 = LinkedList.class;
        getDs().save(e);

        e = getDs().get(e);
        Assert.assertEquals(LinkedList.class, e.testClass2);
    }

    public static class E {
        @Id
        private ObjectId id;

        @Property
        private Class<? extends Collection> testClass;
        private Class<? extends Collection> testClass2;
    }
}
