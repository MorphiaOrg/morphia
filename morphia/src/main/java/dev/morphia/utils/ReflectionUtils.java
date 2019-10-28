package dev.morphia.utils;


import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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
        final Set<Class<?>> classes = new HashSet<>();

        ClassGraph classGraph = new ClassGraph()
                                    .addClassLoader(loader)
                                    .enableAllInfo();
        if(mapSubPackages) {
            classGraph.whitelistPackages(packageName);
            classGraph.whitelistPackages(packageName + ".*");
        } else {
            classGraph.whitelistPackagesNonRecursive(packageName);
        }

        try (ScanResult scanResult = classGraph.scan()) {
            for (final ClassInfo classInfo : scanResult.getAllClasses()) {
                classes.add(Class.forName(classInfo.getName(), true, loader));
            }
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
