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

package com.google.code.morphia;

import com.google.code.morphia.annotations.MongoCollectionName;
import com.google.code.morphia.annotations.MongoEmbedded;
import com.google.code.morphia.annotations.MongoID;
import com.google.code.morphia.annotations.MongoReference;
import com.google.code.morphia.annotations.MongoTransient;
import com.google.code.morphia.annotations.MongoValue;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.ObjectId;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class Mapper {

    private static final String CLASS_NAME = "className";

    /** Set of classes that have been validated for mapping by this mapper */
    private final CopyOnWriteArraySet<Class> mappedClasses = new CopyOnWriteArraySet<Class>();

    private final ThreadLocal<Map<String, Object>> history = new ThreadLocal<Map<String, Object>>();

    Mapper() {
    }

    boolean isMapped(Class c) {
        return mappedClasses.contains(c);
    }

    void addMappedClass(Class c) {
        mappedClasses.add(c);
    }

    CopyOnWriteArraySet<Class> getMappedClasses() {
        return mappedClasses;
    }

    void clearHistory() {
        history.remove();
    }

    private Field findAnnotatedField(Object obj, Class annotationClass) {
        for (Field field : ReflectionUtils.getDeclaredAndInheritedFields(obj.getClass(), false)) {
            if (field.isAnnotationPresent(annotationClass)) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private String getCollectionName(Object object) throws IllegalAccessException {
        return (String) findAnnotatedField(object, MongoCollectionName.class).get(object);
    }

    private String getID(Object object) throws IllegalAccessException {
        return (String) findAnnotatedField(object, MongoID.class).get(object);
    }

    Class getClassForName( String className, Class defaultClass ) {
        for ( Class c : mappedClasses ) {
            if ( className.equals(c.getCanonicalName()) ) {
                return c;
            }
        }
        try {
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch ( ClassNotFoundException ex ) {
            return defaultClass;
        }
    }

    Object createEntityInstanceForDbObject( Class entityClass, BasicDBObject dbObject ) throws Exception {
        // see if there is a className value
        String className = (String) dbObject.get(CLASS_NAME);
        if ( className != null ) {
            return getClassForName(className, entityClass).newInstance();
        } else {
            return entityClass.newInstance();
        }
    }

    Object fromDBObject(Class entityClass, BasicDBObject dbObject) throws Exception {
        history.set(new HashMap<String, Object>());
        
        Object entity = createEntityInstanceForDbObject(entityClass, dbObject);
        mapDBObjectToEntity(dbObject, entity);

        history.remove();
        return entity;
    }

    DBObject toDBObject( Object entity ) throws Exception {
        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put(CLASS_NAME, entity.getClass().getCanonicalName());

        for (Field field : ReflectionUtils.getDeclaredAndInheritedFields(entity.getClass(), true)) {
            field.setAccessible(true);

            if ( field.isAnnotationPresent(MongoTransient.class) ) {
                // ignore fields marked as transient
                continue;
            }

            if ( field.isAnnotationPresent(MongoID.class) ) {
                String value = (String) field.get(entity);
                if ( value != null && value.length() > 0 ) {
                    dbObject.put("_id", new ObjectId(value));
                }

            } else if ( field.isAnnotationPresent(MongoCollectionName.class) ) {
                String value = (String) field.get(entity);
                if ( value != null && value.length() > 0 ) {
                    dbObject.put("_ns", value);
                }

            } else if ( field.isAnnotationPresent(MongoReference.class) ) {
                mapReferencesToDBObject(entity, field, dbObject);

            } else if ( field.isAnnotationPresent(MongoEmbedded.class) ) {
                mapEmbeddedToDBObject(entity, field, dbObject);

            } else if ( field.isAnnotationPresent(MongoValue.class)
                    || ReflectionUtils.isPropertyType(field.getType())
                    || ReflectionUtils.implementsInterface(field.getType(), List.class)
                    || ReflectionUtils.implementsInterface(field.getType(), Set.class)
                    || ReflectionUtils.implementsInterface(field.getType(), Map.class) ) {
                mapValuesToDBObject(entity, field, dbObject);
            }
        }
        return dbObject;
    }

    void mapReferencesToDBObject( Object entity, Field field, BasicDBObject dbObject) throws Exception {
        MongoReference mongoReference = field.getAnnotation(MongoReference.class);
        String name = getMongoName(field);
        if (ReflectionUtils.implementsInterface(field.getType(), List.class)) {
            List list = (List) field.get(entity);
            if ( list != null ) {
                List values = new ArrayList();
                for ( Object o : list ) {
                    values.add(new DBRef(null, getCollectionName(o), new ObjectId(getID(o))));
                }
                dbObject.put(name, values);
            } else {
                dbObject.removeField(name);
            }

        } else if (ReflectionUtils.implementsInterface(field.getType(), Set.class)) {
            Set set = (Set) field.get(entity);
            if ( set != null ) {
                List values = new ArrayList();
                for ( Object o : set ) {
                    values.add(new DBRef(null, getCollectionName(o), new ObjectId(getID(o))));
                }
                dbObject.put(name, values);
            } else {
                dbObject.removeField(name);
            }

        } else if (ReflectionUtils.implementsInterface(field.getType(), Map.class)) {
            Map<Object,Object> map = (Map<Object,Object>) field.get(entity);
            if ( map != null ) {
                Map values = mongoReference.mapClass().newInstance();
                for ( Map.Entry<Object,Object> entry : map.entrySet() ) {
                    values.put(entry.getKey(), new DBRef(null, getCollectionName(entry.getValue()), new ObjectId(getID(entry.getValue()))));
                }
                dbObject.put(name, values);
            } else {
                dbObject.removeField(name);
            }

        } else {
            Object o = field.get(entity);
            if ( o != null ) {
                dbObject.put(name, new DBRef(null, getCollectionName(o), new ObjectId(getID(o))));
            } else {
                dbObject.removeField(name);
            }
        }
    }

    void mapEmbeddedToDBObject( Object entity, Field field, BasicDBObject dbObject ) throws Exception {
        MongoEmbedded mongoEmbedded = field.getAnnotation(MongoEmbedded.class);
        String name = getMongoName(field);
        if (ReflectionUtils.implementsInterface(field.getType(), List.class)) {
            List list = (List) field.get(entity);
            if ( list != null ) {
                List values = new ArrayList();
                for ( Object o : list ) {
                    values.add(toDBObject(o));
                }
                dbObject.put(name, values);
            } else {
                dbObject.removeField(name);
            }

        } else if (ReflectionUtils.implementsInterface(field.getType(), Set.class)) {
            Set set = (Set) field.get(entity);
            if ( set != null ) {
                List values = new ArrayList();
                for ( Object o : set ) {
                    values.add(toDBObject(o));
                }
                dbObject.put(name, values);
            } else {
                dbObject.removeField(name);
            }

        } else if (ReflectionUtils.implementsInterface(field.getType(), Map.class)) {
            Map<String, Object> map = (Map<String, Object>) field.get(entity);
            if ( map != null ) {
                BasicDBObject mapObj = new BasicDBObject();
                for ( Map.Entry<String,Object> entry : map.entrySet() ) {
                    mapObj.put(entry.getKey(), toDBObject(entry.getValue()));
                }
                dbObject.put(name, mapObj);
            } else {
                dbObject.removeField(name);
            }

        } else {
            Object o = field.get(entity);
            if ( o != null ) {
                dbObject.put(name, toDBObject(o));
            } else {
                dbObject.removeField(name);
            }
        }
    }

    void mapValuesToDBObject( Object entity, Field field, BasicDBObject dbObject ) throws Exception {
        MongoValue mongoValue = field.getAnnotation(MongoValue.class);
        String name = getMongoName(field);

        if (ReflectionUtils.implementsInterface(field.getType(), List.class)) {
            Class paramClass = ReflectionUtils.getParameterizedClass(field);
            List list = (List) field.get(entity);
            if ( list != null ) {
                List values = mongoValue != null ? mongoValue.listClass().newInstance() : new ArrayList();
                if ( paramClass != null ) {
                    for ( Object o : list ) {
                        if ( paramClass.isEnum() ) {
                            values.add(((Enum) o).name());
                        } else if ( paramClass == Locale.class ) {
                            values.add(((Locale) o).toString());
                        } else {
                            values.add(o);
                        }
                    }
                } else {
                    for ( Object o : list ) {
                        if ( o.getClass().isEnum() ) {
                            values.add(((Enum) o).name());
                        } else if ( o.getClass() == Locale.class ) {
                            values.add(((Locale) o).toString());
                        } else {
                            values.add(o);
                        }
                    }
                }
                dbObject.put(name, values);
            } else {
                dbObject.removeField(name);
            }

        } else if (ReflectionUtils.implementsInterface(field.getType(), Set.class)) {
            Class paramClass = ReflectionUtils.getParameterizedClass(field);
            Set set = (Set) field.get(entity);
            if ( set != null ) {
                List values = new ArrayList();
                if ( paramClass != null ) {
                    for ( Object o : set ) {
                        if ( paramClass.isEnum() ) {
                            values.add(((Enum) o).name());
                        } else if ( paramClass == Locale.class ) {
                            values.add(((Locale) o).toString());
                        } else {
                            values.add(o);
                        }
                    }
                } else {
                    for ( Object o : set ) {
                        if ( o.getClass().isEnum() ) {
                            values.add(((Enum) o).name());
                        } else if ( o.getClass() == Locale.class ) {
                            values.add(((Locale) o).toString());
                        } else {
                            values.add(o);
                        }
                    }
                }
                dbObject.put(name, values);
            } else {
                dbObject.removeField(name);
            }

        } else if (ReflectionUtils.implementsInterface(field.getType(), Map.class)) {
            Map<Object,Object> map = (Map<Object,Object>) field.get(entity);
            if ( map != null ) {
                Map mapForDb = new HashMap();
                for ( Map.Entry<Object,Object> entry : map.entrySet() ) {
                    if ( entry.getValue().getClass().isEnum() ) {
                        mapForDb.put(entry.getKey(), ((Enum) entry.getValue()).name());
                    } else if ( entry.getValue().getClass() == Locale.class ) {
                        mapForDb.put(entry.getKey(), ((Locale) entry.getValue()).toString());
                    } else {
                        mapForDb.put(entry.getKey(), entry.getValue());
                    }
                }
                dbObject.put(name, mapForDb);
            } else {
                dbObject.removeField(name);
            }

        } else {
            Object value = field.get(entity);
            if ( value != null ) {
                Class c = field.getType();
                if (c.isEnum()) {
                    dbObject.put(name, ((Enum)value).name());
                } else if ( c == Locale.class ) {
                    dbObject.put(name, ((Locale)value).toString());
                } else {
                    dbObject.put(name, value);
                }
            } else {
                dbObject.removeField(name);
            }
        }
    }

    Object mapDBObjectToEntity( BasicDBObject dbObject, Object entity ) throws Exception {
        // check the history key
        String key = dbObject.containsField("_id") ? dbObject.get("_id").toString() : null;
        if (history.get() == null) {
            history.set(new HashMap<String, Object>());
        }
        if ( key != null ) {
            if (history.get().containsKey(key)) {
                return history.get().get(key);
            } else {
                history.get().put(key, entity);
            }
        }

        for (Field field : ReflectionUtils.getDeclaredAndInheritedFields(entity.getClass(), false)) {
            field.setAccessible(true);

            if ( field.isAnnotationPresent(MongoTransient.class) ) {
                // ignore fields marked as transient
                continue;
            }

            if ( field.isAnnotationPresent(MongoID.class) ) {
                if ( dbObject.get("_id") != null ) {
                    field.set(entity, dbObject.get("_id").toString());
                }
            } else if ( field.isAnnotationPresent(MongoCollectionName.class) ) {
                if ( dbObject.get("_ns") != null ) {
                    field.set(entity, dbObject.get("_ns").toString());
                }

            } else if ( field.isAnnotationPresent(MongoReference.class) ) {
                mapReferencesFromDBObject(dbObject, field, entity);

            } else if ( field.isAnnotationPresent(MongoEmbedded.class) ) {
                mapEmbeddedFromDBObject(dbObject, field, entity);
                
            } else if ( field.isAnnotationPresent(MongoValue.class)
                    || ReflectionUtils.isPropertyType(field.getType())
                    || ReflectionUtils.implementsInterface(field.getType(), List.class)
                    || ReflectionUtils.implementsInterface(field.getType(), Set.class)
                    || ReflectionUtils.implementsInterface(field.getType(), Map.class) ) {
                mapValuesFromDBObject(dbObject, field, entity);
            }
        }
        return entity;
    }

    void mapValuesFromDBObject( BasicDBObject dbObject, Field field, Object entity ) throws Exception {
        MongoValue mongoValue = field.getAnnotation(MongoValue.class);
        String name = getMongoName(field);

        if (ReflectionUtils.implementsInterface(field.getType(), List.class)) {
            if ( dbObject.containsField(name) ) {
                Class paramClass = ReflectionUtils.getParameterizedClass(field);
                List list = (List) dbObject.get(name);
                List values = mongoValue != null ? mongoValue.listClass().newInstance() : new ArrayList();

                if ( paramClass != null ) {
                    if (paramClass == Locale.class) {
                        for ( Object o : list ) {
                            values.add(parseLocale((String)o));
                        }
                        field.set(entity, values);
                    } else if (paramClass.isEnum()) {
                        for ( Object o : list ) {
                            values.add(Enum.valueOf(paramClass, (String)o));
                        }
                        field.set(entity, values);
                    } else {
                        for ( Object o : list ) {
                            values.add(o);
                        }
                        field.set(entity, values);
                    }
                } else {
                    field.set(entity, list);
                }

            } else {
                field.set(entity, mongoValue.listClass().newInstance());
            }

        } else if (ReflectionUtils.implementsInterface(field.getType(), Set.class)) {
            if ( dbObject.containsField(name) ) {
                Class paramClass = ReflectionUtils.getParameterizedClass(field);
                List set = (List) dbObject.get(name);
                Set values = mongoValue != null ? mongoValue.setClass().newInstance() : new HashSet();

                if ( paramClass != null ) {
                    if (paramClass == Locale.class) {
                        for ( Object o : set ) {
                            values.add(parseLocale((String)o));
                        }
                        field.set(entity, values);
                    } else if (paramClass.isEnum()) {
                        for ( Object o : set ) {
                            values.add(Enum.valueOf(paramClass, (String)o));
                        }
                        field.set(entity, values);
                    } else {
                        for ( Object o : set ) {
                            values.add(o);
                        }
                        field.set(entity, values);
                    }
                } else {
                    field.set(entity, set);
                }

            } else {
                field.set(entity, mongoValue.setClass().newInstance());
            }

        } else if (ReflectionUtils.implementsInterface(field.getType(), Map.class)) {
            if ( dbObject.containsField(name) ) {
                Map<Object,Object> map = (Map<Object,Object>) dbObject.get(name);
                Map values = mongoValue != null ? mongoValue.mapClass().newInstance() : new HashMap();
                for ( Map.Entry<Object,Object> entry : map.entrySet() ) {
                    if ( entry.getValue().getClass() == Locale.class ) {
                        values.put(entry.getKey(), parseLocale((String)entry.getValue()));
                    } else if ( entry.getValue().getClass().isEnum() ) {
                        Class enumClass = entry.getValue().getClass();
                        values.put(entry.getKey(), Enum.valueOf(enumClass, (String)entry.getValue()));
                    } else {
                        values.put(entry.getKey(), entry.getValue());
                    }
                }
                field.set(entity, values);

            } else {
                field.set(entity, mongoValue.mapClass().newInstance());
            }

        } else {
            if ( dbObject.containsField(name) ) {
                field.set(entity, objectFromValue(field.getType(), dbObject, name));
            }
        }
    }

    Object objectFromValue( Class c, BasicDBObject dbObject, String name ) {
        if (c == String.class) {
            return dbObject.getString(name);
        } else if (c == Date.class) {
            return (Date)dbObject.get(name);
        } else if (c == Integer.class || c == int.class) {
            return dbObject.getInt(name);
        } else if (c == Long.class || c == long.class) {
            return dbObject.getLong(name);
        } else if (c == Double.class || c == double.class) {
            return (Double)dbObject.get(name);
        } else if (c == Boolean.class || c == boolean.class) {
            return (Boolean)dbObject.get(name);
        } else if (c == Locale.class) {
            return parseLocale(dbObject.getString(name));
        } else if (c.isEnum()) {
            return Enum.valueOf(c, dbObject.getString(name));
        }
        return null;
    }

    void mapEmbeddedFromDBObject( BasicDBObject dbObject, Field field, Object entity ) throws Exception {
        MongoEmbedded mongoEmbedded = field.getAnnotation(MongoEmbedded.class);
        String name = getMongoName(field);

        if ( ReflectionUtils.implementsInterface(field.getType(), List.class) ) {
            // multiple documents in a List
            Class docObjClass = ReflectionUtils.getParameterizedClass(field);
            List docs = mongoEmbedded.listClass().newInstance();
            if ( dbObject.containsField(name) ) {
                Object value = dbObject.get(name);
                if ( value instanceof List ) {
                    List refList = (List) value;
                    for ( Object docDbObject : refList ) {
                        Object docObj = createEntityInstanceForDbObject(docObjClass, (BasicDBObject)docDbObject);
                        docObj = mapDBObjectToEntity((BasicDBObject)docDbObject, docObj);
                        docs.add(docObj);
                    }
                } else {
                    BasicDBObject docDbObject = (BasicDBObject) dbObject.get(name);
                    Object docObj = createEntityInstanceForDbObject(docObjClass, docDbObject);
                    docObj = mapDBObjectToEntity(docDbObject, docObj);
                    docs.add(docObj);
                }
            }
            field.set(entity, docs);

        } else if ( ReflectionUtils.implementsInterface(field.getType(), Set.class) ) {
            // multiple documents in a Set
            Class docObjClass = ReflectionUtils.getParameterizedClass(field);
            Set docs = mongoEmbedded.setClass().newInstance();
            if ( dbObject.containsField(name) ) {
                Object value = dbObject.get(name);
                if ( value instanceof List ) {
                    List refs = (List) value;
                    for ( Object docDbObject : refs ) {
                        Object docObj = createEntityInstanceForDbObject(docObjClass, (BasicDBObject)docDbObject);
                        docObj = mapDBObjectToEntity((BasicDBObject)docDbObject, docObj);
                        docs.add(docObj);
                    }
                } else {
                    BasicDBObject docDbObject = (BasicDBObject) dbObject.get(name);
                    Object docObj = createEntityInstanceForDbObject(docObjClass, docDbObject);
                    docObj = mapDBObjectToEntity(docDbObject, docObj);
                    docs.add(docObj);
                }
            }
            field.set(entity, docs);

        } else if (ReflectionUtils.implementsInterface(field.getType(), Map.class)) {
            Class docObjClass = ReflectionUtils.getParameterizedClass(field, 1);
            Map map = mongoEmbedded.mapClass().newInstance();
            if ( dbObject.containsField(name) ) {
                BasicDBObject value = (BasicDBObject) dbObject.get(name);
                for ( Map.Entry entry : value.entrySet() ) {
                    Object docObj = createEntityInstanceForDbObject(docObjClass, (BasicDBObject)entry.getValue());
                    docObj = mapDBObjectToEntity((BasicDBObject)entry.getValue(), docObj);
                    map.put(entry.getKey(), docObj);
                }
            }
            field.set(entity, map);

        } else {
            // single document
            Class docObjClass = field.getType();
            if ( dbObject.containsField(name) ) {
                BasicDBObject docDbObject = (BasicDBObject) dbObject.get(name);
                Object refObj = createEntityInstanceForDbObject(docObjClass, docDbObject);
                refObj = mapDBObjectToEntity(docDbObject, refObj);
                field.set(entity, refObj);
            }
        }
    }

    void mapReferencesFromDBObject( BasicDBObject dbObject, Field field, Object entity ) throws Exception {
        MongoReference mongoReference = field.getAnnotation(MongoReference.class);
        String name = getMongoName(field);

        if ( ReflectionUtils.implementsInterface(field.getType(), List.class) ) {
            // multiple references in a List
            Class referenceObjClass = ReflectionUtils.getParameterizedClass(field);
            List references = mongoReference.listClass().newInstance();
            if ( dbObject.containsField(name) ) {
                Object value = dbObject.get(name);
                if ( value instanceof List ) {
                    List refList = (List) value;
                    for ( Object dbRefObj : refList ) {
                        DBRef dbRef = (DBRef) dbRefObj;
                        BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

                        Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                        refObj = mapDBObjectToEntity(refDbObject, refObj);
                        references.add(refObj);
                    }
                } else {
                    DBRef dbRef = (DBRef) dbObject.get(name);
                    BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();
                    Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                    refObj = mapDBObjectToEntity(refDbObject, refObj);
                    references.add(refObj);
                }
            }
            field.set(entity, references);

        } else if ( ReflectionUtils.implementsInterface(field.getType(), Set.class) ) {
            // multiple references in a Set
            Class referenceObjClass = ReflectionUtils.getParameterizedClass(field);
            Set references = mongoReference.setClass().newInstance();
            if ( dbObject.containsField(name) ) {
                Object value = dbObject.get(name);
                if ( value instanceof Set ) {
                    List refs = (List) value;
                    for ( Object dbRefObj : refs ) {
                        DBRef dbRef = (DBRef) dbRefObj;
                        BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

                        Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                        refObj = mapDBObjectToEntity(refDbObject, refObj);
                        references.add(refObj);
                    }
                } else {
                    DBRef dbRef = (DBRef) dbObject.get(name);
                    BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();
                    Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                    refObj = mapDBObjectToEntity(refDbObject, refObj);
                    references.add(refObj);
                }
            }
            field.set(entity, references);

        } else if (ReflectionUtils.implementsInterface(field.getType(), Map.class)) {
            Class referenceObjClass = ReflectionUtils.getParameterizedClass(field, 1);
            Map map = mongoReference.mapClass().newInstance();
            if ( dbObject.containsField(name) ) {
                BasicDBObject value = (BasicDBObject) dbObject.get(name);
                for ( Map.Entry entry : value.entrySet() ) {
                    DBRef dbRef = (DBRef) entry.getValue();
                    BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

                    Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                    refObj = mapDBObjectToEntity(refDbObject, refObj);
                    map.put(entry.getKey(), refObj);
                }
            }
            field.set(entity, map);
            
        } else {
            // single reference
            Class referenceObjClass = field.getType();
            if ( dbObject.containsField(name) ) {
                DBRef dbRef = (DBRef) dbObject.get(name);
                BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

                Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                refObj = mapDBObjectToEntity(refDbObject, refObj);
                field.set(entity, refObj);
            }
        }
    }

    private String getMongoName( Field field ) {
        String name = null;
        if ( field.isAnnotationPresent(MongoValue.class) ) {
            MongoValue mv = field.getAnnotation(MongoValue.class);
            name = mv.value();
        } else if ( field.isAnnotationPresent(MongoEmbedded.class) ) {
            MongoEmbedded me = field.getAnnotation(MongoEmbedded.class);
            name = me.value();
        } else if ( field.isAnnotationPresent(MongoReference.class) ) {
            MongoReference mr = field.getAnnotation(MongoReference.class);
            name = mr.value();
        }

        if ( name == null || name.equals("fieldName") ) {
            return field.getName();
        } else {
            return name;
        }
    }

    private Locale parseLocale(String localeString) {
        if (localeString != null && localeString.length() > 0) {
            StringTokenizer st = new StringTokenizer(localeString, "_");
            String language = st.hasMoreElements() ? st.nextToken() : Locale.getDefault().getLanguage();
            String country = st.hasMoreElements() ? st.nextToken() : "";
            String variant = st.hasMoreElements() ? st.nextToken() : "";
            return new Locale(language, country, variant);
        }
        return null;
    }
}
