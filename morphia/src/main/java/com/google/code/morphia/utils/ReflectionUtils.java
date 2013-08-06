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


import java.io.File;
import java.io.FileInputStream;
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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

import org.bson.types.CodeWScope;
import org.bson.types.ObjectId;

import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.mapping.MappingException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;


/**
 * Various reflection utility methods, used mainly in the Mapper.
 *
 * @author Olafur Gauti Gudmundsson
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ReflectionUtils {

  /**
   * Get an array of all fields declared in the supplied class, and all its superclasses (except java.lang.Object).
   *
   * @param type the class for which we want to retrieve the Fields
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
   * Get a list of all methods declared in the supplied class, and all its superclasses (except java.lang.Object), recursively.
   *
   * @param type the class for which we want to retrieve the Methods
   * @return an array of all declared and inherited fields
   */
  public static List<Method> getDeclaredAndInheritedMethods(final Class type) {
    return getDeclaredAndInheritedMethods(type, new ArrayList());
  }

  protected static List<Method> getDeclaredAndInheritedMethods(final Class type, List<Method> methods) {
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
   * Check if a class implements a specific interface.
   *
   * @param type the class we want to check
   * @param interfaceClass the interface class we want to check against
   * @return true if type implements interfaceClass, else false
   */
  public static boolean implementsInterface(final Class type, final Class interfaceClass) {
    return interfaceClass.isAssignableFrom(type);
  }

  /**
   * Check if a class extends a specific class.
   *
   * @param type
   *            the class we want to check
   * @param superClass
   *            the super class we want to check against
   * @return true if type implements superClass, else false
   */
  //    public static boolean extendsClass(final Class type, final Class superClass)
  //    {
  //        return superClass.isAssignableFrom(type);
  //    }

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
    if (type instanceof Class) {
      return isPropertyType((Class) type);
    }

    throw new RuntimeException("bad type, not parameterized...");
  }

  public static boolean isPropertyType(final Class type) {
    if (type == null) {
      return false;
    }

    return isPrimitiveLike(type) || (type == DBRef.class) || (type == Pattern.class) ||
      (type == CodeWScope.class) || (type == ObjectId.class) || (type == Key.class) ||
      (type == DBObject.class) || (type == BasicDBObject.class);
  }

  public static boolean isPrimitiveLike(final Class type) {
    if (type == null) {
      return false;
    }

    return (type == String.class) || (type == char.class) || (type == Character.class) || (type == short.class) || (type == Short.class)
      || (type == Integer.class) || (type == int.class) || (type == Long.class) || (type == long.class) || (type == Double.class) || (type
      == double.class) || (type == float.class) || (type == Float.class) || (type == Boolean.class) || (type == boolean.class) || (type
      == Byte.class) || (type == byte.class) || (type == Date.class) || (type == Locale.class) || (type == Class.class) || (type
      == UUID.class) || (type == URI.class) || type.isEnum();
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
        return Array.newInstance(arrayType, 0).getClass();
      } else {
        if (paramType instanceof ParameterizedType) {
          final ParameterizedType paramPType = (ParameterizedType) paramType;
          return (Class) paramPType.getRawType();
        } else {
          if (paramType instanceof TypeVariable) {
            // TODO: Figure out what to do... Walk back up the to
            // the parent class and try to get the variable type
            // from the T/V/X
            throw new MappingException(
              "Generic Typed Class not supported:  <" + ((TypeVariable) paramType).getName() + "> = " + ((TypeVariable) paramType)
                .getBounds()[0]);
          } else if (paramType instanceof Class) {
            return (Class) paramType;
          } else {
            throw new MappingException("Unknown type... pretty bad... call for help, wave your hands... yeah!");
          }
        }
      }
    }

    // Not defined on field, but may be on class or super class...
	return getParameterizedClass(field.getType());
  }

  public static Type getParameterizedType(final Field field, final int index) {
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
            //						throw new MappingException("Generic Typed Class not supported:  <" + ((TypeVariable) paramType).getName() + "> = " + ((TypeVariable) paramType).getBounds()[0]);
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
	return getParameterizedClass(field.getType());
  }

  public static Class getTypeArgumentOfParameterizedClass(final Field field, final int index, final int typeIndex) {
    if (field.getGenericType() instanceof ParameterizedType) {
      final ParameterizedType type = (ParameterizedType) field.getGenericType();
      final Type paramType = type.getActualTypeArguments()[index];
      if (!(paramType instanceof GenericArrayType)) {
        if (paramType instanceof ParameterizedType) {
          final ParameterizedType paramPType = (ParameterizedType) paramType;
          final Type paramParamType = paramPType.getActualTypeArguments()[typeIndex];
          if (!(paramParamType instanceof ParameterizedType)) {
            return (Class) paramParamType;
          }
        }
      }
    }
    return null;
  }

  public static Class getParameterizedClass(final Class c) {
    return getParameterizedClass(c, 0);
  }

  public static Class getParameterizedClass(final Class c, final int index) {
    final TypeVariable[] typeVars = c.getTypeParameters();
    if (typeVars.length > 0) {
      final TypeVariable typeVariable = typeVars[index];
      final Type[] bounds = typeVariable.getBounds();

      final Type type = bounds[0];
      if (type instanceof Class) {
        return (Class) type;// broke for EnumSet, cause bounds contain
        // type instead of class
      } else {
        return null;
      }
    } else if (c.getGenericSuperclass() instanceof ParameterizedType) {
      final Type[] actualTypeArguments = ((ParameterizedType) c.getGenericSuperclass()).getActualTypeArguments();
	  return actualTypeArguments.length > index ? (Class<?>)actualTypeArguments[index] : null;
    } else {
      return null;
    }
  }

  /**
   * Check if a field is parameterized with a specific class.
   *
   * @param field the field
   * @param c the class to check against
   * @return true if the field is parameterized and c is the class that parameterizes the field, or is an interface that the parameterized
   *         class implements, else false
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
   * Check if the field supplied is parameterized with a valid JCR property type.
   *
   * @param field the field
   * @return true if the field is parameterized with a valid JCR property type, else false
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

  public static <T> T getAnnotation(final Class c, final Class<T> annClass) {
    final ArrayList<T> found = getAnnotations(c, annClass);
    if (found != null && !found.isEmpty()) {
      return found.get(0);
    } else {
      return null;
    }
  }

  /**
   * Returns the (first) instance of the annotation, on the class (or any superclass, or interfaces implemented).
   */
  public static <T> ArrayList<T> getAnnotations(final Class c, final Class<T> annClass) {
    final ArrayList<T> found = new ArrayList<T>();
    // TODO isn't that actually breaking the contract of @Inherited?
    if (c.isAnnotationPresent(annClass)) {
      found.add((T) c.getAnnotation(annClass));
    }
    //        else
    //        {
    // need to check all superclasses

    Class parent = c.getSuperclass();
    while ((parent != null) && (parent != Object.class)) {
      if (parent.isAnnotationPresent(annClass)) {
        found.add((T) parent.getAnnotation(annClass));
      }

      // ...and interfaces that the superclass implements
      for (final Class interfaceClass : parent.getInterfaces()) {
        if (interfaceClass.isAnnotationPresent(annClass)) {
          found.add((T) interfaceClass.getAnnotation(annClass));
        }
      }

      parent = parent.getSuperclass();
    }

    // ...and all implemented interfaces
    for (final Class interfaceClass : c.getInterfaces()) {
      if (interfaceClass.isAnnotationPresent(annClass)) {
        found.add((T) interfaceClass.getAnnotation(annClass));
      }
    }
    //        }
    // no annotation found, use the defaults
    return found;
  }

  public static Embedded getClassEmbeddedAnnotation(final Class c) {
    return getAnnotation(c, Embedded.class);
  }

  public static Entity getClassEntityAnnotation(final Class c) {
    return getAnnotation(c, Entity.class);
  }

  private static String stripFilenameExtension(final String filename) {
    if (filename.indexOf('.') != -1) {
      return filename.substring(0, filename.lastIndexOf('.'));
    } else {
      return filename;
    }
  }

  public static Set<Class<?>> getFromDirectory(final File directory, final String packageName) throws ClassNotFoundException {
    final Set<Class<?>> classes = new HashSet<Class<?>>();
    if (directory.exists()) {
      for (final String file : directory.list()) {
        if (file.endsWith(".class")) {
          final String name = packageName + '.' + stripFilenameExtension(file);
          final Class<?> clazz = Class.forName(name);
          classes.add(clazz);
        }
      }
    }
    return classes;
  }

  public static Set<Class<?>> getFromJARFile(final String jar, final String packageName)
    throws IOException, ClassNotFoundException {
    final Set<Class<?>> classes = new HashSet<Class<?>>();
    final JarInputStream jarFile = new JarInputStream(new FileInputStream(jar));
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
    }
    while (jarEntry != null);
    return classes;
  }

  public static Set<Class<?>> getClasses(final String packageName) throws IOException, ClassNotFoundException {
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return getClasses(loader, packageName);
  }

  public static Set<Class<?>> getClasses(final ClassLoader loader, final String packageName) throws IOException, ClassNotFoundException {
    final Set<Class<?>> classes = new HashSet<Class<?>>();
    final String path = packageName.replace('.', '/');
    final Enumeration<URL> resources = loader.getResources(path);
    if (resources != null) {
      while (resources.hasMoreElements()) {
        String filePath = resources.nextElement().getFile();
        // WINDOWS HACK
        if (filePath.indexOf("%20") > 0) {
          filePath = filePath.replaceAll("%20", " ");
        }
        // # in the jar name
        if (filePath.indexOf("%23") > 0) {
          filePath = filePath.replaceAll("%23", "#");
        }

        if (filePath != null) {
          if ((filePath.indexOf("!") > 0) & (filePath.indexOf(".jar") > 0)) {
            String jarPath = filePath.substring(0, filePath.indexOf("!")).substring(filePath.indexOf(":") + 1);
            // WINDOWS HACK
            if (jarPath.contains(":")) {
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

  /**
   * create a new instance of the entity, first using the dbObject field, then
   * by calling createInstance based on the type
   */
  //	public static Object createInstance(final MappedField mf, final DBObject dbObject) {
  //		// see if there is a className value
  //		return createInstance(mf.getConcreteType(), dbObject);
  //	}

  //    public static Object createInstance(final Class type)
  //    {
  //        try
  //        {
  //            return getNoArgsConstructor(type).newInstance();
  //        }
  //        catch (Exception e)
  //        {
  //            throw new RuntimeException(e);
  //        }
  //    }

  /**
   * gets the Class for some classname, or if the className is not found,
   * return the defaultClass instance
   */
  //	public static Class getClassForName(final String className, final Class defaultClass) {
  //		try {
  //			Class c = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
  //			return c;
  //		} catch (ClassNotFoundException ex) {
  //			return defaultClass;
  //		}
  //	}

  /**
   * create a new instance of the entity, first using the dbObject field, then by calling createInstance based on the type
   */
  //	public static Object createInstance(final Class entityClass, final DBObject dbObject) {
  //		// see if there is a className value
  //		String className = (String) dbObject.get(Mapper.CLASS_NAME_FIELDNAME);
  //		Class c = entityClass;
  //		if (className != null) {
  //			// try to Class.forName(className) as defined in the dbObject first,
  //			// otherwise return the entityClass
  //			c = getClassForName(className, entityClass);
  //		}
  //		return createInstance(c);
  //	}

  //	public static Object newInstance(final Class<?> c, final Class<?> fallbackType) {
  //		return newInstance(getNoArgsConstructor(c), fallbackType);
  //	}

  //	private static Constructor getNoArgsConstructor(final Class type) {
  //		try {
  //			Constructor constructor = type.getDeclaredConstructor();
  //			constructor.setAccessible(true);
  //			return constructor;
  //		} catch (NoSuchMethodException e) {
  //			throw new MappingException("No usable constructor for " + type.getName(), e);
  //		}
  //	}
  public static ArrayList iterToList(final Iterable it) {
    if (it instanceof ArrayList) {
      return (ArrayList) it;
    }
    if (it == null) {
      return null;
    }

    final ArrayList ar = new ArrayList();
    for (final Object o : it) {
      ar.add(o);
    }

    return ar;
  }

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
   * @param baseClass the base class
   * @param childClass the child class
   * @return a list of the raw classes for the actual type arguments.
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

  public static <T> Class<?> getTypeArgument(final Class<? extends T> clazz, final TypeVariable<? extends GenericDeclaration> tv) {
    final Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
    Type type = clazz;
    // start walking up the inheritance hierarchy until we hit the end
    while (!getClass(type).equals(Object.class)) {
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
            return getClass(resolvedTypes.get(actualTypeArguments[i]));
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
