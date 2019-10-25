package dev.morphia.utils;

import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static dev.morphia.testutil.ExactClassMatcher.exactClass;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author Scott Hernandez
 */
@SuppressWarnings("UnusedDeclaration")
public class ReflectionUtilsTest extends TestBase {

    @Test
    public void shouldSupportGenericArrays() {
        getMapper().map(MyEntity.class);
    }

    @Test
    public void testGetFromJarFileOnlyLoadsClassesInSpecifiedPackage() throws IOException, ClassNotFoundException {
        //we need a jar to test with so use JUnit since it will always be there
        String rootPath = Test.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        Set<Class<?>> result = ReflectionUtils.getFromJarFile(Thread.currentThread().getContextClassLoader(), rootPath, "org/junit", true);

        for (Class clazz : result) {
            assertThat(clazz.getPackage().getName().startsWith("org.junit"), is(true));
        }
        assertThat(result.contains(org.junit.Assert.class), is(true));
        assertThat(result.contains(org.junit.rules.RuleChain.class), is(true));
    }

    @Test
    public void testGetFromJarFileWithUnicodePath() throws IOException, ClassNotFoundException {
        //we need a jar to test with so use JUnit since it will always be there
        String rootPath = Test.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        final File input = new File(rootPath);
        final Path output = Paths.get("/tmp/我的路径/something.jar");
        output.toFile().delete();
        output.getParent().toFile().mkdirs();
        final Path jar = Files.copy(input.toPath(), output);
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()});
        Set<Class<?>> result = ReflectionUtils.getFromJarFile(classLoader, jar.toString(),
            "org/junit", true);

        for (Class clazz : result) {
            assertThat(clazz.getPackage().getName().startsWith("org.junit"), is(true));
        }
        assertThat(result.contains(org.junit.Assert.class), is(true));
        assertThat(result.contains(org.junit.rules.RuleChain.class), is(true));
    }

    private interface MapWithoutGenericTypes extends Map<Integer, String> {
    }

    // jar can be built using git@github.com:em14Vito/morphia-demo-project.git
    @Test
    @Ignore
    public void nestedJars() throws IOException, ClassNotFoundException {
        final File file = new File("lib/morphia-test-executable.jar");
        Assume.assumeTrue(file.exists());

        final Set<Class<?>> classes = ReflectionUtils.readFromNestedJar(new URLClassLoader(new URL[] { file.toURI().toURL() }),
            "lib/morphia-test-executable.jar!/BOOT-INF/lib/repository-1.0-SNAPSHOT.jar",
            "com.github.em14vito.repository.mongodb", true);

        assertFalse(classes.isEmpty());
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
