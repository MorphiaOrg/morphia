package dev.morphia.utils;


import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


/**
 * Various reflection utility methods, used mainly in the Mapper.
 *
 * @morphia.internal
 */
public final class ReflectionUtils {
    private ReflectionUtils() {
    }

    /**
     * Get a list of all methods declared in the supplied class, and all its superclasses (except java.lang.Object), recursively.
     *
     * @param type the class for which we want to retrieve the Methods
     * @return an array of all declared and inherited fields
     */
    public static List<Method> getDeclaredAndInheritedMethods(final Class type) {
        final List<Method> methods = new ArrayList<>();
        if ((type == null) || (type == Object.class)) {
            return methods;
        }

        final Class parent = type.getSuperclass();
        methods.addAll(getDeclaredAndInheritedMethods(parent));

        for (final Method m : type.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                methods.add(m);
            }
        }

        return methods;
    }

    /**
     * Checks if the class is an integer type, i.e., is numeric but not a floating point type.
     *
     * @param type the class we want to check
     * @return true if the type is an integral type
     */
    public static boolean isIntegerType(final Class type) {
        return Arrays.<Class>asList(Integer.class, int.class, Long.class, long.class, Short.class, short.class, Byte.class,
            byte.class).contains(type);
    }

    /**
     * Checks if the Class given is a primitive type.  This includes the Java primitive types and their wrapper types.
     *
     * @param type the Class to examine
     * @return true if the Class's type is considered a primitive type
     */
    public static boolean isPrimitiveLike(final Class type) {
        return type != null && (type == String.class || type == char.class
                                || type == Character.class || type == short.class || type == Short.class
                                || type == Integer.class || type == int.class || type == Long.class || type == long.class
                                || type == Double.class || type == double.class || type == float.class || type == Float.class
                                || type == Boolean.class || type == boolean.class || type == Byte.class || type == byte.class
                                || type == Date.class || type == Locale.class || type == Class.class || type == UUID.class
                                || type == URI.class || type.isEnum());

    }

    /**
     * Returns the classes in a package
     *
     * @param loader         the ClassLoader to use
     * @param packageName    the package to scan
     * @param mapSubPackages whether to map the sub-packages while scanning
     * @return the list of classes
     * @throws ClassNotFoundException thrown if a class can not be found
     */
    public static Set<Class<?>> getClasses(final ClassLoader loader, final String packageName, final boolean mapSubPackages)
        throws ClassNotFoundException {
        final Set<Class<?>> classes = new HashSet<Class<?>>();

        ClassGraph classGraph = new ClassGraph()
                                    .addClassLoader(loader)
                                    .enableAllInfo();
        if(mapSubPackages) {
            classGraph.whitelistPackages(packageName);
            classGraph.whitelistPackages(packageName + ".*");
        } else {
            classGraph.whitelistPackagesNonRecursive(packageName);
        }
        ScanResult scanResult = classGraph
                .scan();
        try {
            Iterator<ClassInfo> iterator = scanResult.getAllClasses().iterator();
            while(iterator.hasNext()) {
                classes.add(Class.forName(iterator.next().getName(), true, loader));
            }
        } finally {
            scanResult.close();
        }
        return classes;
    }

    /**
     * Converts an Iterable to a List
     *
     * @param it  the Iterable
     * @param <T> the types of the elements in the Iterable
     * @return the List
     */
    public static <T> List<T> iterToList(final Iterable<T> it) {
        if (it instanceof List) {
            return (List<T>) it;
        }
        if (it == null) {
            return null;
        }

        final List<T> ar = new ArrayList<>();
        for (final T o : it) {
            ar.add(o);
        }

        return ar;
    }

}
