/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.morphia.utils;

import com.google.code.morphia.annotations.MongoDocument;
import com.google.code.morphia.annotations.MongoEmbedded;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Various reflection utility methods, used mainly in the Mapper.
 * 
 * @author Olafur Gauti Gudmundsson
 */
public class ReflectionUtils {

    /**
     * Get an array of all fields declared in the supplied class,
     * and all its superclasses (except java.lang.Object).
     *
     * @param type the class for which we want to retrieve the Fields
     * @param returnFinalFields specifies whether to return final fields
     * @return an array of all declared and inherited fields
     */
    public static Field[] getDeclaredAndInheritedFields(Class type, boolean returnFinalFields) {
        List<Field> allFields = new ArrayList<Field>();
        allFields.addAll(getValidFields(type.getDeclaredFields(), returnFinalFields));
        Class parent = type.getSuperclass();
        while (parent != null && parent != Object.class) {
            allFields.addAll(getValidFields(parent.getDeclaredFields(), returnFinalFields));
            parent = parent.getSuperclass();
        }
        return allFields.toArray(new Field[allFields.size()]);
    }

    public static List<Field> getValidFields(Field[] fields, boolean returnFinalFields) {
        List<Field> validFields = new ArrayList<Field>();
        // we ignore static and final fields
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers()) && (returnFinalFields || !Modifier.isFinal(field.getModifiers()))) {
                validFields.add(field);
            }
        }
        return validFields;
    }

    /**
     * Check if a class implements a specific interface.
     *
     * @param type the class we want to check
     * @param interfaceClass the interface class we want to check against
     * @return true if type implements interfaceClass, else false
     */
    public static boolean implementsInterface(Class type, Class interfaceClass) {
        if (type.isInterface()) {
            return type == interfaceClass;
        }

        for (Class ifc : type.getInterfaces()) {
            if (ifc == interfaceClass) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a class extends a specific class.
     *
     * @param type the class we want to check
     * @param superClass the super class we want to check against
     * @return true if type implements superClass, else false
     */
    public static boolean extendsClass(Class type, Class superClass) {
        if (type == superClass) {
            return true;
        }

        Class c = type.getSuperclass();
        while (c != null && c != Object.class) {
            if (c == superClass) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    /**
     * Check if the class supplied represents a valid property type.
     *
     * @param type the class we want to check
     * @return true if the class represents a valid property type
     */
    public static boolean isPropertyType(Class type) {
        return type == String.class
                || type == Date.class
                || type == Integer.class
                || type == int.class
                || type == Long.class
                || type == long.class
                || type == Double.class
                || type == double.class
                || type == Boolean.class
                || type == boolean.class
                || type == Locale.class
                || type.isEnum()
                ;
    }

    public static boolean isValidMapValueType(Class type) {
        return isPropertyType(type);
    }

    private static boolean isArrayOfType(Class c, Class type) {
        return c.isArray() && c.getComponentType() == type;
    }

    public static boolean isDateType(Class type) {
        return type == Date.class || type == Calendar.class || type == Timestamp.class;
    }

    /**
     * Get the (first) class that parameterizes the Field supplied.
     *
     * @param field the field
     * @return the class that parameterizes the field, or null if field is
     * not parameterized
     */
    public static Class getParameterizedClass(Field field) {
        return getParameterizedClass(field, 0);
    }

    /**
     * Get the class that parameterizes the Field supplied, at the index
     * supplied (field can be parameterized with multiple param classes).
     *
     * @param field the field
     * @param index the index of the parameterizing class
     * @return the class that parameterizes the field, or null if field is
     * not parameterized
     */
    public static Class getParameterizedClass(Field field, int index) {
        if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) field.getGenericType();
            if ( ptype.getActualTypeArguments() != null
                    && ptype.getActualTypeArguments().length <= index ) {
                return null;
            }
            Type paramType = ptype.getActualTypeArguments()[index];
            if (paramType instanceof GenericArrayType) {
                Class arrayType = (Class) ((GenericArrayType) paramType).getGenericComponentType();
                return Array.newInstance(arrayType, 0).getClass();
            } else {
                if (paramType instanceof ParameterizedType) {
                    ParameterizedType paramPType = (ParameterizedType) paramType;
                    return (Class) paramPType.getRawType();
                } else {
                    return (Class) paramType;
                }
            }
        }
        return null;
    }

    public static Class getTypeArgumentOfParameterizedClass(Field field, int index, int typeIndex) {
        if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) field.getGenericType();
            Type paramType = ptype.getActualTypeArguments()[index];
            if (!(paramType instanceof GenericArrayType)) {
                if (paramType instanceof ParameterizedType) {
                    ParameterizedType paramPType = (ParameterizedType) paramType;
                    Type paramParamType = paramPType.getActualTypeArguments()[typeIndex];
                    if (!(paramParamType instanceof ParameterizedType)) {
                        return (Class) paramParamType;
                    }
                }
            }
        }
        return null;
    }

    public static Class getParameterizedClass(Class c) {
        return getParameterizedClass(c, 0);
    }

    public static Class getParameterizedClass(Class c, int index) {
        TypeVariable[] typeVars = c.getTypeParameters();
        if (typeVars.length > 0) {
            return (Class) typeVars[index].getBounds()[0];
        } else {
            return null;
        }
    }

    /**
     * Check if a field is parameterized with a specific class.
     *
     * @param field the field
     * @param c the class to check against
     * @return true if the field is parameterized and c is the class that
     * parameterizes the field, or is an interface that the parameterized class
     * implements, else false
     */
    public static boolean isFieldParameterizedWithClass(Field field, Class c) {
        if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) field.getGenericType();
            for (Type type : ptype.getActualTypeArguments()) {
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
     * Check if the field supplied is parameterized with a valid JCR
     * property type.
     *
     * @param field the field
     * @return true if the field is parameterized with a valid JCR property
     * type, else false
     */
    public static boolean isFieldParameterizedWithPropertyType(Field field) {
        if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) field.getGenericType();
            for (Type type : ptype.getActualTypeArguments()) {
                if (isPropertyType((Class) type)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static MongoEmbedded getClassMongoEmbeddedAnnotation(Class c) {

        if (c.isAnnotationPresent(MongoEmbedded.class)) {
            return (MongoEmbedded) c.getAnnotation(MongoEmbedded.class);
        } else {
            // need to check all superclasses
            Class parent = c.getSuperclass();
            while (parent != null && parent != Object.class) {
                if (parent.isAnnotationPresent(MongoEmbedded.class)) {
                    return (MongoEmbedded) parent.getAnnotation(MongoEmbedded.class);
                }

                // ...and interfaces that the superclass implements
                for (Class interfaceClass : parent.getInterfaces()) {
                    if (interfaceClass.isAnnotationPresent(MongoEmbedded.class)) {
                        return (MongoEmbedded) interfaceClass.getAnnotation(MongoEmbedded.class);
                    }
                }

                parent = parent.getSuperclass();
            }

            // ...and all implemented interfaces
            for (Class interfaceClass : c.getInterfaces()) {
                if (interfaceClass.isAnnotationPresent(MongoEmbedded.class)) {
                    return (MongoEmbedded) interfaceClass.getAnnotation(MongoEmbedded.class);
                }
            }
        }
        // no annotation found, use the defaults
        return null;
    }

    public static MongoDocument getClassMongoDocumentAnnotation(Class c) {

        if (c.isAnnotationPresent(MongoDocument.class)) {
            return (MongoDocument) c.getAnnotation(MongoDocument.class);
        } else {
            // need to check all superclasses
            Class parent = c.getSuperclass();
            while (parent != null && parent != Object.class) {
                if (parent.isAnnotationPresent(MongoDocument.class)) {
                    return (MongoDocument) parent.getAnnotation(MongoDocument.class);
                }

                // ...and interfaces that the superclass implements
                for (Class interfaceClass : parent.getInterfaces()) {
                    if (interfaceClass.isAnnotationPresent(MongoDocument.class)) {
                        return (MongoDocument) interfaceClass.getAnnotation(MongoDocument.class);
                    }
                }

                parent = parent.getSuperclass();
            }

            // ...and all implemented interfaces
            for (Class interfaceClass : c.getInterfaces()) {
                if (interfaceClass.isAnnotationPresent(MongoDocument.class)) {
                    return (MongoDocument) interfaceClass.getAnnotation(MongoDocument.class);
                }
            }
        }
        // no annotation found, use the defaults
        return null;
    }

    private static String stripFilenameExtension( String filename ) {
        if ( filename.indexOf('.') != -1 ) {
            return filename.substring(0, filename.lastIndexOf('.'));
        } else {
            return filename;
        }
    }

    public static Set<Class<?>> getFromDirectory(File directory, String packageName) throws ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        if (directory.exists()) {
            for (String file : directory.list()) {
                if (file.endsWith(".class")) {
                    String name = packageName + '.' + stripFilenameExtension(file);
                    Class<?> clazz = Class.forName(name);
                    classes.add(clazz);
                }
            }
        }
        return classes;
    }

    public static Set<Class<?>> getFromJARFile(String jar, String packageName) 
            throws IOException, FileNotFoundException, ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        JarInputStream jarFile = new JarInputStream(new FileInputStream(jar));
        JarEntry jarEntry;
        do {
            jarEntry = jarFile.getNextJarEntry();
            if (jarEntry != null) {
                String className = jarEntry.getName();
                if (className.endsWith(".class")) {
                    className = stripFilenameExtension(className);
                    if (className.startsWith(packageName)) {
                        classes.add(Class.forName(className.replace('/', '.')));
                    }
                }
            }
        } while (jarEntry != null);
        return classes;
    }

    public static Set<Class<?>> getClasses(String packageName) throws IOException, ClassNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return getClasses(loader, packageName);
    }

    public static Set<Class<?>> getClasses(ClassLoader loader, String packageName) throws IOException, ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = loader.getResources(path);
        if (resources != null) {
            while (resources.hasMoreElements()) {
                String filePath = resources.nextElement().getFile();
                // WINDOWS HACK
                if (filePath.indexOf("%20") > 0) {
                    filePath = filePath.replaceAll("%20", " ");
                }
                if (filePath != null) {
                    if ((filePath.indexOf("!") > 0) & (filePath.indexOf(".jar") > 0)) {
                        String jarPath = filePath.substring(0, filePath.indexOf("!")).substring(filePath.indexOf(":") + 1);
                        // WINDOWS HACK
                        if (jarPath.indexOf(":") >= 0) {
                            jarPath = jarPath.substring(1);
                        }
                        classes.addAll(getFromJARFile(jarPath, path));
                    } else {
                        classes.addAll(getFromDirectory(new File(filePath), packageName));
                    }
                }
            }
        }
        return classes;
    }
}
