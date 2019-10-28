/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */


package dev.morphia.utils;


import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import dev.morphia.Key;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.MappingException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.bson.types.CodeWScope;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;


/**
 * Various reflection utility methods, used mainly in the Mapper.
 *
 * @morphia.internal
 */
public final class ReflectionUtils {
    private ReflectionUtils() {
    }

    /**
     * Get an array of all fields declared in the supplied class, and all its superclasses (except java.lang.Object).
     *
     * @param type              the class for which we want to retrieve the Fields
     * @param returnFinalFields specifies whether to return final fields
     * @return an array of all declared and inherited fields
     */
    public static Field[] getDeclaredAndInheritedFields(final Class type, final boolean returnFinalFields) {
        final List<Field> allFields = new ArrayList<Field>();
        allFields.addAll(getValidFields(type.getDeclaredFields(), returnFinalFields));
        Class parent = type.getSuperclass();
        while ((parent != null) && (parent != Object.class)) {
            allFields.addAll(getValidFields(parent.getDeclaredFields(), returnFinalFields));
            parent = parent.getSuperclass();
        }
        return allFields.toArray(new Field[allFields.size()]);
    }

    /**
     * Scans the array fields and returns any fields that are not static or (optionally) final.
     *
     * @param fields            the fields to process
     * @param returnFinalFields include final fields in the results
     * @return the valid fields
     */
    public static List<Field> getValidFields(final Field[] fields, final boolean returnFinalFields) {
        final List<Field> validFields = new ArrayList<Field>();
        // we ignore static and final fields
        for (final Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers()) && (returnFinalFields || !Modifier.isFinal(field.getModifiers()))) {
                validFields.add(field);
            }
        }
        return validFields;
    }

    /**
     * Get a list of all methods declared in the supplied class, and all its superclasses (except java.lang.Object), recursively.
     *
     * @param type the class for which we want to retrieve the Methods
     * @return an array of all declared and inherited fields
     */
    public static List<Method> getDeclaredAndInheritedMethods(final Class type) {
        return getDeclaredAndInheritedMethods(type, new ArrayList<Method>());
    }

    private static List<Method> getDeclaredAndInheritedMethods(final Class type, final List<Method> methods) {
        if ((type == null) || (type == Object.class)) {
            return methods;
        }

        final Class parent = type.getSuperclass();
        final List<Method> list = getDeclaredAndInheritedMethods(parent, methods == null ? new ArrayList<Method>() : methods);

        for (final Method m : type.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                list.add(m);
            }
        }

        return list;
    }

    //    public static boolean implementsAnyInterface(final Class type, final Class... interfaceClasses)
    //    {
    //        for (Class iF : interfaceClasses)
    //        {
    //            if (implementsInterface(type, iF))
    //            {
    //                return true;
    //            }
    //        }
    //        return false;
    //    }

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
     * Check if the class supplied represents a valid property type.
     *
     * @param type the class we want to check
     * @return true if the class represents a valid property type
     */
    public static boolean isPropertyType(final Type type) {
        if (type instanceof GenericArrayType) {
            return isPropertyType(((GenericArrayType) type).getGenericComponentType());
        }
        if (type instanceof ParameterizedType) {
            return isPropertyType(((ParameterizedType) type).getRawType());
        }
        return type instanceof Class && isPropertyType((Class) type);
    }

    /**
     * Get the (first) class that parameterizes the Field supplied.
     *
     * @param field the field
     * @return the class that parameterizes the field, or null if field is not parameterized
     */
    public static Class getParameterizedClass(final Field field) {
        return getParameterizedClass(field, 0);
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
        return getParameterizedClass(field.getType(), index);
    }

    /**
     * Returns the parameterized type for a field
     *
     * @param field the field to examine
     * @param index the location of the parameter to return
     * @return the type
     */
    public static Type getParameterizedType(final Field field, final int index) {
        if (field != null) {
            if (field.getGenericType() instanceof ParameterizedType) {
                final ParameterizedType type = (ParameterizedType) field.getGenericType();
                if ((type.getActualTypeArguments() != null) && (type.getActualTypeArguments().length <= index)) {
                    return null;
                }
                final Type paramType = type.getActualTypeArguments()[index];
                if (paramType instanceof GenericArrayType) {
                    return paramType; //((GenericArrayType) paramType).getGenericComponentType();
                } else {
                    if (paramType instanceof ParameterizedType) {
                        return paramType;
                    } else {
                        if (paramType instanceof TypeVariable) {
                            // TODO: Figure out what to do... Walk back up the to
                            // the parent class and try to get the variable type
                            // from the T/V/X
                            // throw new MappingException("Generic Typed Class not supported:  <" + ((TypeVariable)
                            // paramType).getName() + "> = " + ((TypeVariable) paramType).getBounds()[0]);
                            return paramType;
                        } else if (paramType instanceof WildcardType) {
                            return paramType;
                        } else if (paramType instanceof Class) {
                            return paramType;
                        } else {
                            throw new MappingException("Unknown type... pretty bad... call for help, wave your hands... yeah!");
                        }
                    }
                }
            }

            // Not defined on field, but may be on class or super class...
            return getParameterizedClass(field.getType(), index);
        }

        return null;
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
                return getParameterizedClass((Class) superclass, index);
            } else {
                return null;
            }
        }
    }

    /**
     * Check if a field is parameterized with a specific class.
     *
     * @param field the field
     * @param c     the class to check against
     * @return true if the field is parameterized and c is the class that parameterizes the field, or is an interface that the parameterized
     * class implements, else false
     * @deprecated this class is unused in morphia and will be removed in a future release
     */
    public static boolean isFieldParameterizedWithClass(final Field field, final Class c) {
        if (field.getGenericType() instanceof ParameterizedType) {
            final ParameterizedType genericType = (ParameterizedType) field.getGenericType();
            for (final Type type : genericType.getActualTypeArguments()) {
                if (type == c) {
                    return true;
                }
                if (c.isInterface() && implementsInterface((Class) type, c)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a class implements a specific interface.
     *
     * @param type           the class we want to check
     * @param interfaceClass the interface class we want to check against
     * @return true if type implements interfaceClass, else false
     */
    public static boolean implementsInterface(final Class<?> type, final Class<?> interfaceClass) {
        return interfaceClass.isAssignableFrom(type);
    }

    /**
     * Check if the field supplied is parameterized with a valid JCR property type.
     *
     * @param field the field
     * @return true if the field is parameterized with a valid JCR property type, else false
     * @deprecated this class is unused in morphia and will be removed in a future release
     */
    public static boolean isFieldParameterizedWithPropertyType(final Field field) {
        if (field.getGenericType() instanceof ParameterizedType) {
            final ParameterizedType genericType = (ParameterizedType) field.getGenericType();
            for (final Type type : genericType.getActualTypeArguments()) {
                if (isPropertyType((Class) type)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the Class given is a property type
     *
     * @param type the Class to examine
     * @return true if the Class's type is considered a property type
     */
    public static boolean isPropertyType(final Class type) {
        return type != null && (isPrimitiveLike(type) || type == DBRef.class || type == Pattern.class
                                || type == CodeWScope.class || type == ObjectId.class || type == Key.class
                                || type == DBObject.class || type == BasicDBObject.class);

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
     * Returns the @Embedded annotation on a Class if present
     *
     * @param c the class to examine
     * @return the annotation.  may be null.
     */
    public static Embedded getClassEmbeddedAnnotation(final Class c) {
        return getAnnotation(c, Embedded.class);
    }

    /**
     * Returns an annotation on a Class if present
     *
     * @param c          the class to examine
     * @param annotation the annotation to find
     * @param <T>        the type of the annotation
     * @return the annotation.  may be null.
     */
    public static <T> T getAnnotation(final Class c, final Class<T> annotation) {
        final List<T> found = getAnnotations(c, annotation);
        if (found != null && !found.isEmpty()) {
            return found.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns the (first) instance of the annotation, on the class (or any superclass, or interfaces implemented).
     *
     * @param c          the class to examine
     * @param annotation the annotation to find
     * @param <T>        the type of the annotation
     * @return the list of annotations
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getAnnotations(final Class c, final Class<T> annotation) {
        final List<T> found = new ArrayList<T>();
        // TODO isn't that actually breaking the contract of @Inherited?
        if (c.isAnnotationPresent(annotation)) {
            found.add((T) c.getAnnotation(annotation));
        }

        Class parent = c.getSuperclass();
        while ((parent != null) && (parent != Object.class)) {
            if (parent.isAnnotationPresent(annotation)) {
                found.add((T) parent.getAnnotation(annotation));
            }

            // ...and interfaces that the superclass implements
            for (final Class interfaceClass : parent.getInterfaces()) {
                if (interfaceClass.isAnnotationPresent(annotation)) {
                    found.add((T) interfaceClass.getAnnotation(annotation));
                }
            }

            parent = parent.getSuperclass();
        }

        // ...and all implemented interfaces
        for (final Class interfaceClass : c.getInterfaces()) {
            if (interfaceClass.isAnnotationPresent(annotation)) {
                found.add((T) interfaceClass.getAnnotation(annotation));
            }
        }
        // no annotation found, use the defaults
        return found;
    }

    /**
     * Returns the @Entity annotation on a Class if present
     *
     * @param c the class to examine
     * @return the annotation.  may be null.
     */
    public static Entity getClassEntityAnnotation(final Class c) {
        return getAnnotation(c, Entity.class);
    }

    /**
     * Returns the classes in a package
     *
     * @param loader         the ClassLoader to use
     * @param packageName    the package to scan
     * @param mapSubPackages whether to map the sub-packages while scanning
     * @return the list of classes
     * @throws IOException            thrown if a class can not be loaded
     * @throws ClassNotFoundException thrown if a class can not be found
     */
    public static Set<Class<?>> getClasses(final ClassLoader loader, final String packageName, final boolean mapSubPackages)
        throws IOException, ClassNotFoundException {
        final Set<Class<?>> classes = new HashSet<Class<?>>();

        ClassGraph classGraph = new ClassGraph()
                                    .addClassLoader(loader)
                                    .enableAllInfo();
        if (mapSubPackages) {
            classGraph.whitelistPackages(packageName);
            classGraph.whitelistPackages(packageName + ".*");
        } else {
            classGraph.whitelistPackagesNonRecursive(packageName);
        }
        ScanResult scanResult = classGraph.scan();
        try {
            Iterator<ClassInfo> iterator = scanResult.getAllClasses().iterator();
            while (iterator.hasNext()) {
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

        final List<T> ar = new ArrayList<T>();
        for (final T o : it) {
            ar.add(o);
        }

        return ar;
    }

    /**
     * Converts a List to an array
     *
     * @param type   the Class type of the elements of the List
     * @param values the List to convert
     * @return the array
     */
    public static Object convertToArray(final Class type, final List<?> values) {
        final Object exampleArray = Array.newInstance(type, values.size());
        try {
            return values.toArray((Object[]) exampleArray);
        } catch (ClassCastException e) {
            for (int i = 0; i < values.size(); i++) {
                Array.set(exampleArray, i, values.get(i));
            }
            return exampleArray;
        }
    }


    /**
     * Get the underlying class for a type, or null if the type is a variable type.
     *
     * @param type the type
     * @return the underlying class
     */
    public static Class<?> getClass(final Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            final Type componentType = ((GenericArrayType) type).getGenericComponentType();
            final Class<?> componentClass = getClass(componentType);
            if (componentClass != null) {
                return Array.newInstance(componentClass, 0).getClass();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get the actual type arguments a child class has used to extend a generic base class.
     *
     * @param baseClass  the base class
     * @param childClass the child class
     * @param <T>        the type of the base class
     * @return a list of the raw classes for the actual type arguments.
     * @deprecated this class is unused in morphia and will be removed in a future release
     */
    public static <T> List<Class<?>> getTypeArguments(final Class<T> baseClass, final Class<? extends T> childClass) {
        final Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
        Type type = childClass;
        // start walking up the inheritance hierarchy until we hit baseClass
        while (!getClass(type).equals(baseClass)) {
            if (type instanceof Class) {
                // there is no useful information for us in raw types, so just
                // keep going.
                type = ((Class) type).getGenericSuperclass();
            } else {
                final ParameterizedType parameterizedType = (ParameterizedType) type;
                final Class<?> rawType = (Class) parameterizedType.getRawType();

                final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                final TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
                }

                if (!rawType.equals(baseClass)) {
                    type = rawType.getGenericSuperclass();
                }
            }
        }

        // finally, for each actual type argument provided to baseClass,
        // determine (if possible)
        // the raw class for that type argument.
        final Type[] actualTypeArguments;
        if (type instanceof Class) {
            actualTypeArguments = ((Class) type).getTypeParameters();
        } else {
            actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
        }
        final List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
        // resolve types by chasing down type variables.
        for (Type baseType : actualTypeArguments) {
            while (resolvedTypes.containsKey(baseType)) {
                baseType = resolvedTypes.get(baseType);
            }
            typeArgumentsAsClasses.add(getClass(baseType));
        }
        return typeArgumentsAsClasses;
    }

    /**
     * Returns the type argument
     *
     * @param clazz the Class to examine
     * @param tv    the TypeVariable to look for
     * @param <T>   the type of the Class
     * @return the Class type
     */
    public static <T> Class<?> getTypeArgument(final Class<? extends T> clazz, final TypeVariable<? extends GenericDeclaration> tv) {
        final Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
        Type type = clazz;
        // start walking up the inheritance hierarchy until we hit the end
        while (type != null && !Object.class.equals(getClass(type))) {
            if (type instanceof Class) {
                // there is no useful information for us in raw types, so just
                // keep going.
                type = ((Class) type).getGenericSuperclass();
            } else {
                final ParameterizedType parameterizedType = (ParameterizedType) type;
                final Class<?> rawType = (Class) parameterizedType.getRawType();

                final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                final TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    if (typeParameters[i].equals(tv)) {
                        final Class cls = getClass(actualTypeArguments[i]);
                        if (cls != null) {
                            return cls;
                        }
                        //We don't know that the type we want is the one in the map, if this argument has been
                        //passed through multiple levels of the hierarchy.  Walk back until we run out.
                        Type typeToTest = resolvedTypes.get(actualTypeArguments[i]);
                        while (typeToTest != null) {
                            final Class classToTest = getClass(typeToTest);
                            if (classToTest != null) {
                                return classToTest;
                            }
                            typeToTest = resolvedTypes.get(typeToTest);
                        }
                    }
                    resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
                }

                if (!rawType.equals(Object.class)) {
                    type = rawType.getGenericSuperclass();
                }
            }
        }

        return null;
    }
}
