package com.google.code.morphia.utils;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.mapping.Mapper;
import org.junit.Assert;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author Scott Hernandez
 */
public class ReflectionUtilsTest extends TestBase {

  /**
   * Test method for {@link ReflectionUtils#implementsInterface(Class, Class)} .
   */
  @Test
  public void testImplementsInterface() {
    Assert.assertTrue(ReflectionUtils.implementsInterface(ArrayList.class, List.class));
    Assert.assertTrue(ReflectionUtils.implementsInterface(ArrayList.class, Collection.class));
    Assert.assertFalse(ReflectionUtils.implementsInterface(Set.class, List.class));
  }

  @Test
  public void testInheritedClassAnnotations() {
    final List<Indexes> annotations = ReflectionUtils.getAnnotations(Foobie.class, Indexes.class);
    Assert.assertEquals(2, annotations.size());
    Assert.assertTrue(ReflectionUtils.getAnnotation(Foobie.class, Indexes.class) != null);

    Assert.assertTrue("Base".equals(ReflectionUtils.getClassEntityAnnotation(Foo.class).value()));

    Assert.assertTrue("Sub".equals(ReflectionUtils.getClassEntityAnnotation(Foobie.class).value()));

    Assert.assertEquals(Mapper.IGNORED_FIELDNAME, ReflectionUtils.getClassEntityAnnotation(Fooble.class).value());
  }

  @Test
  public void testGetParameterizedClass_inherence() throws Exception {
    // Work before fix...
    Assert.assertEquals(Object.class, ReflectionUtils.getParameterizedClass(Set.class));
    Assert.assertEquals(Author.class, ReflectionUtils.getParameterizedClass(Book.class.getDeclaredField("authorsSet")));

    // Works now...
    Assert.assertEquals(Author.class, ReflectionUtils.getParameterizedClass(Book.class.getDeclaredField("authors")));

    Authors authors = new Authors();
    Assert.assertEquals(Author.class, ReflectionUtils.getParameterizedClass(authors.getClass()));

  }

  @Entity("Base")
  @Indexes(@Index("id"))
  private static class Foo {
    @Id
    int id;
  }

  @Entity("Sub")
  @Indexes(@Index("test"))
  private static class Foobie extends Foo {
    String test;
  }

  @Entity()
  private static class Fooble extends Foobie {
  }

  private class Author {
  }

  @SuppressWarnings("serial")
  private class Authors extends HashSet<Author> {
    // Can contain utils methods
  }

  @SuppressWarnings("unused")
  private class Book {
    private Authors authors;

    private Set<Author> authorsSet;
  }
}
