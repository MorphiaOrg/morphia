package dev.morphia.utils;

import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import org.junit.Test;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
public class ReflectionUtilsTest extends TestBase {

    @Test
    public void shouldSupportGenericArrays() {
        getMapper().map(MyEntity.class);
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

    private static class Super1<T extends Object> {
        private T field;
    }

    private static class Super2<T extends Serializable> extends Super1<T> {
    }

    private static class Super3<T extends Number> extends Super2<T> {
    }

}
