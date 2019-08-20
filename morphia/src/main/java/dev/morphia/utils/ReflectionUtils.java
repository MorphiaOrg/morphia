package dev.morphia.utils;


import dev.morphia.mapping.MappingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;


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
     * Get the class that parameterizes the Field supplied, at the index supplied (field can be parameterized with multiple param classes).
     *
     * @param field the field
     * @param index the index of the parameterizing class
     * @return the class that parameterizes the field, or null if field is not parameterized
     */
    public static Class getParameterizedClass(final Field field, final int index) {
        if (field.getGenericType() instanceof ParameterizedType) {
            final ParameterizedType type = (ParameterizedType) field.getGenericType();
            if ((type.getActualTypeArguments() != null) && (type.getActualTypeArguments().length <= index)) {
                return null;
            }
            final Type paramType = type.getActualTypeArguments()[index];
            if (paramType instanceof GenericArrayType) {
                final Class arrayType = (Class) ((GenericArrayType) paramType).getGenericComponentType();
                return Array.newInstance(arrayType, 0)
                            .getClass();
            } else {
                if (paramType instanceof ParameterizedType) {
                    final ParameterizedType paramPType = (ParameterizedType) paramType;
                    return (Class) paramPType.getRawType();
                } else {
                    if (paramType instanceof TypeVariable) {
                        // TODO: Figure out what to do... Walk back up the to
                        // the parent class and try to get the variable type
                        // from the T/V/X
                        throw new MappingException("Generic Typed Class not supported:  <" + ((TypeVariable) paramType).getName() + "> = "
                                                   + ((TypeVariable) paramType).getBounds()[0]);
                    } else if (paramType instanceof Class) {
                        return (Class) paramType;
                    } else {
                        throw new MappingException("Unknown type... pretty bad... call for help, wave your hands... yeah!");
                    }
                }
            }
        }
        return getParameterizedClass(field.getType());
    }

    /**
     * Returns the parameterized type of a Class
     *
     * @param c the class to examine
     * @return the type
     */
    public static Class getParameterizedClass(final Class c) {
        return getParameterizedClass(c, 0);
    }

    /**
     * Returns the parameterized type in the given position
     *
     * @param c     the class to examine
     * @param index the position of the type to return
     * @return the type
     */
    public static Class getParameterizedClass(final Class c, final int index) {
        final TypeVariable[] typeVars = c.getTypeParameters();
        if (typeVars.length > 0) {
            final TypeVariable typeVariable = typeVars[index];
            final Type[] bounds = typeVariable.getBounds();

            final Type type = bounds[0];
            if (type instanceof Class) {
                return (Class) type; // broke for EnumSet, cause bounds contain
                // type instead of class
            } else {
                return null;
            }
        } else {
            Type superclass = c.getGenericSuperclass();
            if (superclass == null && c.isInterface()) {
                Type[] interfaces = c.getGenericInterfaces();
                if (interfaces.length > 0) {
                    superclass = interfaces[index];
                }
            }
            if (superclass instanceof ParameterizedType) {
                final Type[] actualTypeArguments = ((ParameterizedType) superclass).getActualTypeArguments();
                return actualTypeArguments.length > index ? (Class<?>) actualTypeArguments[index] : null;
            } else if (!Object.class.equals(superclass)) {
                return getParameterizedClass((Class) superclass);
            } else {
                return null;
            }
        }
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
     * @throws IOException            thrown if an error is encountered scanning packages
     * @throws ClassNotFoundException thrown if a class can not be found
     */
    public static Set<Class<?>> getClasses(final ClassLoader loader, final String packageName, final boolean mapSubPackages)
        throws IOException, ClassNotFoundException {
        final Set<Class<?>> classes = new HashSet<>();
        final String path = packageName.replace('.', '/');
        final Enumeration<URL> resources = loader.getResources(path);
        if (resources != null) {
            while (resources.hasMoreElements()) {
                String filePath = resources.nextElement()
                                           .getFile();
                // WINDOWS HACK
                if (filePath.indexOf("%20") > 0) {
                    filePath = filePath.replaceAll("%20", " ");
                }
                // # in the jar name
                if (filePath.indexOf("%23") > 0) {
                    filePath = filePath.replaceAll("%23", "#");
                }

                if ((filePath.indexOf("!") > 0) && (filePath.indexOf(".jar") > 0)) {
                    String jarPath = filePath.substring(0, filePath.lastIndexOf("!"))
                                             .substring(filePath.indexOf(":") + 1);
                    // WINDOWS HACK
                    if (jarPath.contains(":")) {
                        jarPath = jarPath.substring(1);
                    }
                    if (jarPath.contains("!")) {
                        classes.addAll(readFromNestedJar(loader, jarPath, path, mapSubPackages));
                    } else {
                        classes.addAll(getFromJarFile(loader, jarPath, path, mapSubPackages));
                    }
                } else {
                    classes.addAll(getFromDirectory(loader, new File(filePath), packageName, mapSubPackages));
                }
            }
        }
        return classes;
    }

    /**
     * @param loader
     * @param jarPath
     * @param packageName
     * @param mapSubPackages
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @morphia.internal
     */
    protected static Set<Class<?>> readFromNestedJar(final ClassLoader loader,
                                                     final String jarPath,
                                                     final String packageName,
                                                     final boolean mapSubPackages) throws IOException, ClassNotFoundException {
        final Set<Class<?>> classes = new HashSet<>();
        final JarFile jarFile = new JarFile(new File(jarPath.substring(0, jarPath.indexOf("!"))));
        final InputStream inputStream = jarFile.getInputStream(jarFile.getEntry(
            jarPath.substring(jarPath.indexOf("!") + 2)));
        final String packagePath = packageName.replace('.', '/');
        final JarInputStream jarStream = new JarInputStream(inputStream);
        try {
            JarEntry jarEntry;
            do {
                jarEntry = jarStream.getNextJarEntry();
                if (jarEntry != null) {
                    String className = jarEntry.getName();
                    if (className.endsWith(".class")) {
                        String classPackageName = getPackageName(className);
                        if (classPackageName.equals(packagePath) || (mapSubPackages && isSubPackage(classPackageName, packagePath))) {
                            className = stripFilenameExtension(className);
                            classes.add(Class.forName(className.replace('/', '.'), true, loader));
                        }
                    }
                }
            } while (jarEntry != null);
        } finally {
            jarFile.close();
            jarStream.close();
        }
        return classes;
    }

    /**
     * Returns the classes in a package found in a jar
     *
     * @param loader         the ClassLoader to use
     * @param jar            the jar to scan
     * @param packageName    the package to scan
     * @param mapSubPackages whether to map the sub-packages while scanning
     * @return the list of classes
     * @throws IOException            thrown if an error is encountered scanning packages
     * @throws ClassNotFoundException thrown if a class can not be found
     * @morphia.internal
     */
    public static Set<Class<?>> getFromJarFile(final ClassLoader loader, final String jar, final String packageName, final boolean
                                                                                                                         mapSubPackages)
        throws IOException, ClassNotFoundException {
        final Set<Class<?>> classes = new HashSet<>();
        try (JarInputStream jarFile = new JarInputStream(new FileInputStream(jar))) {
            JarEntry jarEntry;
            do {
                jarEntry = jarFile.getNextJarEntry();
                if (jarEntry != null) {
                    String className = jarEntry.getName();
                    if (className.endsWith(".class")) {
                        String classPackageName = getPackageName(className);
                        if (classPackageName.equals(packageName) || (mapSubPackages && isSubPackage(classPackageName, packageName))) {
                            className = stripFilenameExtension(className);
                            classes.add(Class.forName(className.replace('/', '.'), true, loader));
                        }
                    }
                }
            } while (jarEntry != null);
        }
        return classes;
    }

    /**
     * Returns the classes in a package found in a directory
     *
     * @param loader         the ClassLoader to use
     * @param directory      the directory to scan
     * @param packageName    the package to scan
     * @param mapSubPackages whether to map the sub-packages while scanning
     * @return the list of classes
     * @throws ClassNotFoundException thrown if a class can not be found
     */
    public static Set<Class<?>> getFromDirectory(final ClassLoader loader, final File directory, final String packageName,
                                                 final boolean mapSubPackages) throws ClassNotFoundException {
        final Set<Class<?>> classes = new HashSet<>();
        if (directory.exists()) {
            for (final String file : getFileNames(directory, packageName, mapSubPackages)) {
                if (file.endsWith(".class")) {
                    final String name = stripFilenameExtension(file);
                    final Class<?> clazz = Class.forName(name, true, loader);
                    classes.add(clazz);
                }
            }
        }
        return classes;
    }

    private static Set<String> getFileNames(final File directory, final String packageName, final boolean mapSubPackages) {
        Set<String> fileNames = new HashSet<>();
        final File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileNames.add(packageName + '.' + file.getName());
                } else if (mapSubPackages) {
                    fileNames.addAll(getFileNames(file, packageName + '.' + file.getName(), true));
                }
            }
        }
        return fileNames;
    }

    private static String getPackageName(final String filename) {
        return filename.contains("/") ? filename.substring(0, filename.lastIndexOf('/')) : filename;
    }

    private static String stripFilenameExtension(final String filename) {
        if (filename.indexOf('.') != -1) {
            return filename.substring(0, filename.lastIndexOf('.'));
        } else {
            return filename;
        }
    }

    private static boolean isSubPackage(final String fullPackageName, final String parentPackageName) {
        return fullPackageName.startsWith(parentPackageName);
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
