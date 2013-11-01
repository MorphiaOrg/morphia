package org.mongodb.morphia.converters;


import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.testutil.TestEntity;

/**
 * @author Uwe Schaefer
 */
public class CustomConverterDefault extends TestBase {

    private static class E extends TestEntity {
        // FIXME issue 100 :
        // http://code.google.com/p/morphia/issues/detail?id=100
        // check default inspection: if not declared as property,
        // morphia fails due to defaulting to embedded and expecting a non-arg
        // constructor.
        //
        // @Property
        private Foo foo;

    }

    // unknown type to convert
    private static class Foo {
        private final String string;

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

    @Test
    public void testConversion() throws Exception {
        final FooConverter fc = new FooConverter();
        getMorphia().getMapper().getConverters().addConverter(fc);
        getMorphia().map(E.class);
        E e = new E();
        e.foo = new Foo("test");
        getDs().save(e);

        Assert.assertTrue(fc.didConversion());

        e = getDs().find(E.class).get();
        Assert.assertNotNull(e.foo);
        Assert.assertEquals("test", e.foo.string);
    }

}
