package org.mongodb.morphia.converters;


import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * @author Uwe Schaefer
 */
@SuppressWarnings("rawtypes")
public class CustomConverterInEmbed extends TestBase {

    public static class E1 extends TestEntity {
        private static final long serialVersionUID = 1L;
        private final List<Foo> foo = new LinkedList<Foo>();
    }

    public static class E2 extends TestEntity {
        private static final long serialVersionUID = 1L;
        private final Map<String, Foo> foo = new HashMap<String, Foo>();
    }

    // unknown type to convert
    public static class Foo {
        private String string;

        Foo() {
        }

        public Foo(final String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    public static class FooConverter extends TypeConverter implements SimpleValueConverter {

        private boolean done;

        public FooConverter() {
            super(Foo.class);
        }

        @Override
        public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
            return new Foo((String) fromDBObject);
        }

        @Override
        public Object encode(final Object value, final MappedField optionalExtraInfo) {
            done = true;
            return value.toString();
        }

        public boolean didConversion() {
            return done;
        }
    }

    //FIXME issue 101

    @Test
    public void testConversionInList() throws Exception {
        final FooConverter fc = new FooConverter();
        getMorphia().getMapper().getConverters().addConverter(fc);
        final E1 e = new E1();
        e.foo.add(new Foo("bar"));
        getDs().save(e);
        Assert.assertTrue(fc.didConversion());
    }

    @Test
    public void testConversionInMap() throws Exception {
        final FooConverter fc = new FooConverter();
        getMorphia().getMapper().getConverters().addConverter(fc);
        E2 e = new E2();
        e.foo.put("bar", new Foo("bar"));
        getDs().save(e);

        Assert.assertTrue(fc.didConversion());

        e = getDs().find(E2.class).get();
        Assert.assertNotNull(e.foo);
        Assert.assertFalse(e.foo.isEmpty());
        Assert.assertTrue(e.foo.containsKey("bar"));
        Assert.assertEquals("bar", e.foo.get("bar").string);
    }

}
