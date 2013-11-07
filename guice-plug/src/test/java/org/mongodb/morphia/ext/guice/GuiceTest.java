package org.mongodb.morphia.ext.guice;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * @author us@thomas-daily.de
 */
public class GuiceTest extends TestBase {
  private Injector i;

  @Override
  public void setUp() {

    super.setUp();
    i = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(Foo.class).toInstance(new Bar());
      }
    });
    new GuiceExtension(getMorphia(), i);
    getMorphia().map(E1.class);
    getMorphia().map(E2.class);
  }

  @Test
  public void testE1() throws Exception {

    final E1 initialEntity = i.getInstance(E1.class);
    final Key<E1> k = getDs().save(initialEntity);
    final E1 loadedEntity = getDs().getByKey(E1.class, k);

    Assert.assertNotNull(loadedEntity);
    Assert.assertNotNull(loadedEntity.foo);
    Assert.assertEquals(Bar.class, loadedEntity.foo
      .getClass());
    Assert.assertEquals(ArrayList.class, loadedEntity.l
      .getClass());

  }

  @Test
  public void testE2() throws Exception {

    final E2 initialEntity = new E2();
    final Key<E2> k = getDs().save(initialEntity);
    final E2 loadedEntity = getDs().getByKey(E2.class, k);

    Assert.assertNotNull(loadedEntity);

  }

  @Test
  public void testE3() throws Exception {

    final E3 initialEntity = i.getInstance(E3.class);
    final Key<E3> k = getDs().save(initialEntity);
    final E3 loadedEntity = getDs().getByKey(E3.class, k);

    Assert.assertNotNull(loadedEntity);
    Assert.assertEquals(MyList.class, loadedEntity.l
      .getClass());
    Assert.assertNotNull(loadedEntity.l.foo);

  }

  @Test
  public void testE4() throws Exception {

    final E4 initialEntity = i.getInstance(E4.class);
    final Key<E4> k = getDs().save(initialEntity);
    final E4 loadedEntity = getDs().getByKey(E4.class, k);

    Assert.assertNotNull(loadedEntity);
    Assert.assertNotNull(loadedEntity.foo);

  }

  public interface Foo {
    int sum(int a, int b);
  }

  static class Bar implements Foo {
    public int sum(final int a, final int b) {
      return a + b;
    }
  }

  @Entity
  public static class E1 {
    @Id
    private ObjectId id;

    @Transient
    private Foo foo;

    private List<Integer> l = Arrays.asList(1, 3, 4);

    private String s = "";

    @Inject
    E1(final Foo f) {
      foo = f;
    }
  }

  @Entity
  public static class E2 {
    @Id
    private ObjectId id;

    private String s = "";
  }

  @Entity
  public static class E3 {
    @Id
    private ObjectId id;

    private MyList<Integer> l = new MyList<Integer>(new Bar());

    private String s = "";

    /**
     *
     */
    public E3() {
      l.add(1);
      l.add(2);
    }
  }

  @Entity
  public static class E4 {
    @Id
    private ObjectId id;

    private String s = "";

    @Inject
    @Transient
    private Foo foo;
  }

  static class MyList<E> extends ArrayList<E> {
    private Foo foo;

    @Inject
    public MyList(final Foo foo) {
      this.foo = foo;
    }
  }

}
