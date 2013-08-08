package com.google.code.morphia.utils;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.mapping.Mapper;


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
  public void testGetParameterizedClassInheritance() throws Exception {
    // Work before fix...
    Assert.assertEquals(Object.class, ReflectionUtils.getParameterizedClass(Set.class));
    Assert.assertEquals(Author.class, ReflectionUtils.getParameterizedClass(Book.class.getDeclaredField("authorsSet")));

    // Works now...
    Assert.assertEquals(Author.class, ReflectionUtils.getParameterizedClass(Book.class.getDeclaredField("authors")));

    Assert.assertEquals(Author.class, ReflectionUtils.getParameterizedClass(Authors.class));
    
    Assert.assertEquals(Author.class, ReflectionUtils.getParameterizedClass(WritingTeam.class));
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

  private static class Author {
  }

  private static class Authors extends HashSet<Author> {
    // Can contain utils methods
  }
  
  private static class WritingTeam extends Authors {
  }

  private static class Book {
    private Authors authors;

    private Set<Author> authorsSet;
  }
}
