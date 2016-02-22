package org.mongodb.morphia.utils;

import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.mapping.Mapper;

import java.io.Serializable;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mongodb.morphia.testutil.ExactClassMatcher.exactClass;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author Scott Hernandez
 */
@SuppressWarnings("UnusedDeclaration")
public class ReflectionUtilsTest extends TestBase {

    @Test
    public void shouldAcceptMapWithoutItsOwnGenericParameters() {
        Class parameterizedClass = ReflectionUtils.getParameterizedClass(MapWithoutGenericTypes.class);

        assertThat(parameterizedClass, is(exactClass(Integer.class)));
    }

    @Test
    public void shouldSupportGenericArrays() {
        getMorphia().map(MyEntity.class);
    }

    /**
     * Tests that in a class hierarchy of arbitrary depth, we can get the correct declared field type
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGenericFieldTypeResolution() throws Exception {
        Class<?> typeArgument = ReflectionUtils.getTypeArgument(Sub.class,
                                                                (TypeVariable) Super1.class.getDeclaredField("field").getGenericType());
        assertThat(typeArgument, is(exactClass(Integer.class)));
    }

    @Test
    public void testGetFromJarFileOnlyLoadsClassesInSpecifiedPackage() throws Exception {
        //we need a jar to test with so use JUnit since it will always be there
        String rootPath = Test.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        Set<Class<?>> result = ReflectionUtils.getFromJARFile(Thread.currentThread().getContextClassLoader(), rootPath, "org/junit", true);

        for (Class clazz : result) {
            assertThat(clazz.getPackage().getName().startsWith("org.junit"), is(true));
        }
        assertThat(result.contains(org.junit.Assert.class), is(true));
        assertThat(result.contains(org.junit.rules.RuleChain.class), is(true));
    }

    @Test
    public void testGetParameterizedClassInheritance() throws Exception {
        // Work before fix...
        assertThat(ReflectionUtils.getParameterizedClass(Set.class), isA(Object.class));
        assertThat(ReflectionUtils.getParameterizedClass(Book.class.getDeclaredField("authorsSet")), is(exactClass(Author.class)));

        // Works now...
        assertThat(ReflectionUtils.getParameterizedClass(Book.class.getDeclaredField("authors")), is(exactClass(Author.class)));

        assertThat(ReflectionUtils.getParameterizedClass(Authors.class), is(exactClass(Author.class)));

        assertThat(ReflectionUtils.getParameterizedClass(WritingTeam.class), is(is(exactClass(Author.class))));
    }

    /**
     * Test method for {@link ReflectionUtils#implementsInterface(Class, Class)} .
     */
    @Test
    public void testImplementsInterface() {
        assertThat(ReflectionUtils.implementsInterface(ArrayList.class, List.class), is(true));
        assertThat(ReflectionUtils.implementsInterface(ArrayList.class, Collection.class), is(true));
        assertThat(ReflectionUtils.implementsInterface(ArrayList.class, Collection.class), is(true));

        assertThat(ReflectionUtils.implementsInterface(Set.class, List.class), is(false));
        assertThat(ReflectionUtils.implementsInterface(List.class, ArrayList.class), is(false));
    }

    @Test
    public void testInheritedClassAnnotations() {
        final List<Indexes> annotations = ReflectionUtils.getAnnotations(Foobie.class, Indexes.class);
        assertThat(annotations.size(), is(2));
        assertThat(ReflectionUtils.getAnnotation(Foobie.class, Indexes.class) != null, is(true));

        assertThat("Base".equals(ReflectionUtils.getClassEntityAnnotation(Foo.class).value()), is(true));

        assertThat("Sub".equals(ReflectionUtils.getClassEntityAnnotation(Foobie.class).value()), is(true));

        assertThat(ReflectionUtils.getClassEntityAnnotation(Fooble.class).value(), is(Mapper.IGNORED_FIELDNAME));
    }

    private interface MapWithoutGenericTypes extends Map<Integer, String> {
    }

    @Entity("generic_arrays")
    static class MyEntity {
        @Id
        private String id;
        private Integer[] integers;
        private Super3<Integer>[] super3s;
    }

    @Entity("Base")
    @Indexes(@Index(fields = @Field("id")))
    private static class Foo {
        @Id
        private int id;
    }

    @Entity("Sub")
    @Indexes(@Index(fields = @Field("test")))
    private static class Foobie extends Foo {
        private String test;
    }

    @Entity
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

    private static class Super1<T extends Object> {
        private T field;
    }

    private static class Super2<T extends Serializable> extends Super1<T> {
    }

    private static class Super3<T extends Number> extends Super2<T> {
    }

    private static class Sub extends Super3<Integer> {
    }
}
