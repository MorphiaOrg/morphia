package com.google.code.morphia.converters;


import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.testutil.TestEntity;

/**
 * @author Uwe Schaefer
 */
public class CustomConverterDefault extends TestBase {

  private static class E extends TestEntity {
    private static final long serialVersionUID = 1L;

    // FIXME issue 100 :
    // http://code.google.com/p/morphia/issues/detail?id=100
    // check default inspection: if not declared as property,
    // morphia fails due to defaulting to embedded and expecting a non-arg
    // constructor.
    //
    // @Property
    Foo foo;

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

  @SuppressWarnings("rawtypes")
  public static class FooConverter extends TypeConverter implements SimpleValueConverter {

    public boolean done;

    public FooConverter() {
      super(Foo.class);
    }

    @Override
    public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
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
    morphia.getMapper().getConverters().addConverter(fc);
    morphia.map(E.class);
    E e = new E();
    e.foo = new Foo("test");
    ds.save(e);

    Assert.assertTrue(fc.didConversion());

    e = ds.find(E.class).get();
    Assert.assertNotNull(e.foo);
    Assert.assertEquals("test", e.foo.string);
  }

}
