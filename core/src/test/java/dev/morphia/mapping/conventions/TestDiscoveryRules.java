package dev.morphia.mapping.conventions;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import dev.morphia.annotations.Transient;
import dev.morphia.mapping.conventions.DiscoveryRules.AccessorPair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestDiscoveryRules {
    @Test
    public void propertyNames() {
        assertEquals("name", DiscoveryRules.propertyName("getName"));
        assertEquals("name", DiscoveryRules.propertyName("setName"));
        assertEquals("active", DiscoveryRules.propertyName("isActive"));
        assertEquals("uRL", DiscoveryRules.propertyName("getURL"));
        assertNull(DiscoveryRules.propertyName("get"));
        assertNull(DiscoveryRules.propertyName("set"));
        assertNull(DiscoveryRules.propertyName("is"));
        assertNull(DiscoveryRules.propertyName("name"));
    }

    @Test
    public void accessorPairs() {
        List<AccessorPair<Method>> pairs = DiscoveryRules.accessorPairs(List.of(Accessors.class.getDeclaredMethods()),
                Method::getName, Method::getModifiers);

        assertEquals(List.of("active:isActive/setActive", "name:getName/setName"), pairs.stream()
                .map(p -> p.property() + ":" + p.getter().getName() + "/" + p.setter().getName())
                .sorted()
                .toList());
    }

    @Test
    public void bridgeMethods() {
        List<Method> methods = List.of(Bridged.class.getDeclaredMethods());
        assertTrue(methods.stream().anyMatch(Method::isBridge), "expected the compiler to add bridge methods");

        List<AccessorPair<Method>> pairs = DiscoveryRules.accessorPairs(methods, Method::getName, Method::getModifiers);

        assertEquals(1, pairs.size());
        assertEquals("value", pairs.get(0).property());
        assertFalse(pairs.get(0).getter().isBridge());
        assertFalse(pairs.get(0).setter().isBridge());
    }

    @Test
    public void ignoredFields() {
        assertFalse(DiscoveryRules.isIgnoredField(Modifier.PRIVATE, List.of()));
        assertTrue(DiscoveryRules.isIgnoredField(Modifier.STATIC, List.of()));
        assertTrue(DiscoveryRules.isIgnoredField(Modifier.TRANSIENT, List.of()));
        assertTrue(DiscoveryRules.isIgnoredField(0, List.of(Transient.class.getName())));
        assertTrue(DiscoveryRules.isIgnoredField(0, List.of(java.beans.Transient.class.getName())));
    }

    @Test
    public void cases() {
        assertEquals("Name", DiscoveryRules.titleCase("name"));
        assertEquals("name", DiscoveryRules.methodCase("Name"));
        assertEquals("", DiscoveryRules.titleCase(""));
    }

    @SuppressWarnings("unused")
    private static class Accessors {
        private String name;
        private boolean active;
        private int count;

        // a static pair isn't a property
        private static int total;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        // three methods named for the property: not a pair
        public int getCount() {
            return count;
        }

        public boolean isCount() {
            return count != 0;
        }

        public void setCount(int count) {
            this.count = count;
        }

        // a getter without a setter
        public int getSize() {
            return 0;
        }

        public static int getTotal() {
            return total;
        }

        public static void setTotal(int total) {
            Accessors.total = total;
        }

        // bare names
        public Object get() {
            return null;
        }

        public void set(Object value) {
        }
    }

    @SuppressWarnings("unused")
    private static class Generic<T> {
        private T value;

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }

    private static class Bridged extends Generic<String> {
        @Override
        public String getValue() {
            return super.getValue();
        }

        @Override
        public void setValue(String value) {
            super.setValue(value);
        }
    }
}
