package com.google.code.morphia.utils;


import java.lang.Class;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import junit.framework.Assert;


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
  }

  @Indexes(@Index("id"))
  private static class Foo {
    @Id int id;
  }

  @Indexes(@Index("test"))
  private static class Foobie extends Foo {
    String test;
  }
}
