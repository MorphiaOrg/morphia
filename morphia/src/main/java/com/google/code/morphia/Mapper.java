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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.code.morphia.MappedClass.MappedField;
import com.google.code.morphia.annotations.CollectionName;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.utils.Key;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.ObjectId;

/**
 *
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class Mapper {
    private static final Logger logger = Logger.getLogger(Mapper.class.getName());

	private static final String CLASS_NAME_KEY = "className";
	
	public static final String ID_KEY = "_id";
	public static final String COLLECTION_NAME_KEY = "_ns";	
	public static final String IGNORED_FIELDNAME = ".";

    /** Set of classes that have been validated for mapping by this mapper */
    private final ConcurrentHashMap<String,MappedClass> mappedClasses = new ConcurrentHashMap<String, MappedClass>();
    
    private final ThreadLocal<Map<String, Object>> entityCache = new ThreadLocal<Map<String, Object>>();


    Mapper() {
    }

    boolean isMapped(Class c) {
        return mappedClasses.containsKey(c.getName());
    }

    void addMappedClass(Class c) {
    	MappedClass mc = new MappedClass(c);
    	mc.validate();
        mappedClasses.put(c.getName(), mc);
    }

    void addMappedClass(MappedClass mc) {
    	mc.validate();
        mappedClasses.put(mc.clazz.getName(), mc);
    }

    Map<String, MappedClass> getMappedClasses() {
        return mappedClasses;
    }

    public MappedClass getMappedClass(Object obj) {
		if (obj == null) return null;
		return mappedClasses.get(obj.getClass().getName());
	}

    void clearHistory() {
        entityCache.remove();
    }

    public String getCollectionName(Object object) {
    	if (object instanceof Class) return getCollectionName((Class) object);
    	
    	MappedClass mc = getMappedClass(object);
    	if (mc == null) mc = new MappedClass(object.getClass());

    	try {
    		return (mc.collectionNameField != null && mc.collectionNameField.get(object) != null) ? (String)mc.collectionNameField.get(object) : mc.defCollName;
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
	public String getCollectionName(Class clazz) {
	  	MappedClass mc = getMappedClass(clazz);
    	if (mc == null) mc = new MappedClass(clazz);
    	return mc.defCollName;
    }

    private String getId(Object entity) {
        try {
            return (String)getMappedClass(entity).idField.get(entity);
        } catch ( IllegalAccessException iae ) {
            throw new RuntimeException(iae);
        }
    }

    /**
     * Updates the {@code @Id} and {@code @CollectionName} fields.
     * @param entity The object to update
     * @param dbId Value to update with; null means skip
     * @param dbNs Value to update with; null or empty means skip
     */
	void updateKeyInfo(Object entity, Object dbId, String dbNs) {
		MappedClass mc = getMappedClass(entity);

		//update id field, if there.
		if (mc.idField != null && dbId != null) {
			try {
				Object value = mc.idField.get(entity);
				if ( value != null ) {
					//The entity already had an id set. Check to make sure it hasn't changed. That would be unexpected, and could indicate a bad state.
			    	if (!dbId.equals(value))
			    		throw new RuntimeException("id mismatch: " + value + " != " + dbId + " for " + entity.getClass().getSimpleName());
				} else {
					//set the id field with the "new" value
					if (dbId instanceof ObjectId && mc.idField.getType().isAssignableFrom(String.class)) {
						dbId = dbId.toString();
					}
		    		mc.idField.set(entity, dbId);
				}

			} catch (Exception e) {
				if (e.getClass().equals(RuntimeException.class)) throw (RuntimeException)e;

				throw new RuntimeException(e);
			}
		}

		//update ns (collectionName)
		if (mc.collectionNameField != null && !(dbNs == null || dbNs.isEmpty())) {
			try {
				String value = (String) mc.collectionNameField.get(entity);
				if ( value != null && value.length() > 0 ) {
			    	if (value != null && !value.equals(dbNs))
			    		throw new RuntimeException("ns mismatch: " + value + " != " + dbNs + " for " + entity.getClass().getSimpleName());
				} else if (value == null)
		    		mc.collectionNameField.set(entity, dbNs);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

    Class getClassForName(String className, Class defaultClass) {
    	if (mappedClasses.containsKey(className)) return mappedClasses.get(className).clazz;
        try {
            Class c = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            return c;
        } catch ( ClassNotFoundException ex ) {
            return defaultClass;
        }
    }

    Object createEntityInstanceForDbObject( Class entityClass, BasicDBObject dbObject ) {
        // see if there is a className value
        String className = (String) dbObject.get(CLASS_NAME_KEY);
        Class c = entityClass;
        if ( className != null ) {
        	//try to Class.forName(className) as defined in the dbObject first, otherwise return the entityClass
            c = getClassForName(className, entityClass);
        }
        try {
        	//allows private/protected constructors
	        Constructor constructor = c.getDeclaredConstructor();
	        constructor.setAccessible(true);
	        return constructor.newInstance();
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    Object fromDBObject(Class entityClass, BasicDBObject dbObject) {
        entityCache.set(new HashMap<String, Object>());
        
        Object entity = createEntityInstanceForDbObject(entityClass, dbObject);
        
        mapDBObjectToEntity(dbObject, entity);

        entityCache.remove();
        return entity;
    }

    DBObject toDBObject( Object entity ) {
    	BasicDBObject dbObject = new BasicDBObject();
    	try {
	        dbObject.put(CLASS_NAME_KEY, entity.getClass().getCanonicalName());
	
	        MappedClass mc = getMappedClass(entity);
	        if (mc == null) mc = new MappedClass(entity.getClass());
	        
	        String collName = (mc.collectionNameField == null) ? null :  (String)mc.collectionNameField.get(entity);
	        if (collName != null && collName.length() > 0 ) dbObject.put(COLLECTION_NAME_KEY, collName);
	
	
	        dbObject = (BasicDBObject) mc.callLifecycleMethods(PrePersist.class, entity, dbObject);
	        for (MappedField mf : mc.persistenceFields) {
	            Field field = mf.field;
	
	            field.setAccessible(true);
	
	            if ( mf.hasAnnotation(Id.class) ) {
	                Object value = field.get(entity);
	                if ( value != null ) {
	                    dbObject.put(ID_KEY, asObjectIdMaybe(value));
	                }
	            } else if ( mf.hasAnnotation(Reference.class) ) {
	                mapReferencesToDBObject(entity, mf, dbObject);
	            } else  if (mf.hasAnnotation(Embedded.class)){
	                mapEmbeddedToDBObject(entity, mf, dbObject);
	            } else if (mf.isMongoTypeCompatible()) {
	            	mapValuesToDBObject(entity, mf, dbObject);
	            } else {
	            	logger.warning("Ignoring field: " + field.getName() + " [" + field.getType().getSimpleName() + "]");
	            }
	            
	        }	        
        } catch (Exception e) {throw new RuntimeException(e);}
        return dbObject;

    }

    void mapReferencesToDBObject( Object entity, MappedField mf, BasicDBObject dbObject) {
    	try {
	    	Reference mongoReference = (Reference)mf.getAnnotation(Reference.class);
	        String name = mf.name;
	
	        Object fieldValue = mf.field.get(entity);
	        
	        if (mf.isMap()) {
	            Map<Object,Object> map = (Map<Object,Object>) fieldValue;
	            if ( map != null ) {
	                Map values = mongoReference.mapClass().newInstance();
	                for ( Map.Entry<Object,Object> entry : map.entrySet() ) {
	                    values.put(entry.getKey(), new DBRef(null, getCollectionName(entry.getValue()), asObjectIdMaybe(getId(entry.getValue()))));
	                }
	                if (values.size() > 0) dbObject.put(name, values);
	            } else {
	                dbObject.removeField(name);
	            }
	    	} else if (mf.isMultipleValues()) {
	            Collection coll = (Collection) fieldValue;
	            if ( coll != null ) {
	                List values = new ArrayList();
	                for ( Object o : coll ) {
	                    values.add(new DBRef(null, getCollectionName(o), asObjectIdMaybe(getId(o))));
	                }
	                if (values.size() > 0) dbObject.put(name, values);
	            }
	        } else {
	            if ( fieldValue != null ) {
	                dbObject.put(name, new DBRef(null, getCollectionName(fieldValue), asObjectIdMaybe(getId(fieldValue))));
	            }
	        }
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    void mapEmbeddedToDBObject( Object entity, MappedField mf, BasicDBObject dbObject ) {
        String name = mf.name;

        Object fieldValue = null;
		try {
			fieldValue = mf.field.get(entity);
        } catch (Exception e) {throw new RuntimeException(e);}


	    if (mf.isMap()) {
	        Map<String, Object> map = (Map<String, Object>) fieldValue;
	        if ( map != null ) {
	            BasicDBObject values = new BasicDBObject();
	            for ( Map.Entry<String,Object> entry : map.entrySet() ) {
	                values.put(entry.getKey(), toDBObject(entry.getValue()));
	            }
	            if (values.size()>0) dbObject.put(name, values);
	        }
	
	    } else if (mf.isMultipleValues()) {
            Iterable coll = (Iterable)fieldValue;
            if ( coll != null ) {
                List values = new ArrayList();
                for ( Object o : coll ) {
                    values.add(toDBObject(o));
                }
                if (values.size()>0) dbObject.put(name, values);
            }
        } else {
            if ( fieldValue != null ) {
                dbObject.put(name, toDBObject(fieldValue));
            }
        }
    }

    void mapValuesToDBObject( Object entity, MappedField mf, BasicDBObject dbObject ) {
        try {
	    	String name = mf.name;
	        Class fieldType = mf.field.getType();
	        Object fieldValue = mf.field.get(entity);
	
	        //sets and list are stored in mongodb as ArrayLists
	        if (mf.isMap()) {
	            Map<Object,Object> map = (Map<Object,Object>) mf.field.get(entity);
	            if ( map != null ) {
	                Map mapForDb = new HashMap();
	                for ( Map.Entry<Object,Object> entry : map.entrySet() ) {
	                	mapForDb.put(entry.getKey(), objectToValue(entry.getValue()));
	                }
	                dbObject.put(name, mapForDb);
	            }
	        } else if (mf.isMultipleValues()) {
	        	Class paramClass = mf.subType;
	            if ( fieldValue != null ) {
	            	Iterable iterableValues = null;
	
	            	if (fieldType.isArray()) {
	            		Object[] objects = null;
	            		try {
	            			objects = (Object[]) fieldValue;
	            		} catch (ClassCastException e) {
	                		//store the primitive array without making it into a list.
	            			if (Array.getLength(fieldValue) == 0) return;
	            			dbObject.put(name, fieldValue);
	            			return;
	            		}
	            		//convert array into arraylist
	            		iterableValues = new ArrayList(objects.length);
	            		for(Object obj :objects)
	            			((ArrayList)iterableValues).add(obj);
	            	} else {
	            		//cast value to a common interface
	            		iterableValues = (Iterable) fieldValue;
	            	}
	        	
	        		//cast value to a common interface
	        		List values = new ArrayList();
	                
	            	if ( paramClass != null ) {
	                    for ( Object o : iterableValues )
	                    	values.add(objectToValue(paramClass, o));
	                } else {
	                    for ( Object o : iterableValues )
	                    	values.add(objectToValue(o));
	                }
	        		if (values.size() > 0) dbObject.put(name, values);
	            }
	        
	        } else {
	            if ( fieldValue != null ) {
	            	dbObject.put(name, objectToValue(fieldValue));
	            }
	        }
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    Object mapDBObjectToEntity( BasicDBObject dbObject, Object entity ) {
        // check the history key (a key is the namespace + id)
        String cacheKey = (!dbObject.containsField(ID_KEY)) ? null : dbObject.getString(COLLECTION_NAME_KEY) + "[" + dbObject.getString(ID_KEY) + "]";
        if (entityCache.get() == null) {
            entityCache.set(new HashMap<String, Object>());
        }
        if ( cacheKey != null ) {
            if (entityCache.get().containsKey(cacheKey)) {
                return entityCache.get().get(cacheKey);
            } else {
                entityCache.get().put(cacheKey, entity);
            }
        }

        MappedClass mc = getMappedClass(entity);
        if (mc == null) mc = new MappedClass(entity.getClass());

        dbObject = (BasicDBObject) mc.callLifecycleMethods(PreLoad.class, entity, dbObject);
        try {
	        for (MappedField mf : mc.persistenceFields) {
	            Field field = mf.field;
	//            String name = mf.name;
	            field.setAccessible(true);
	
	            if ( mf.hasAnnotation(Id.class) ) {
	                if ( dbObject.get(ID_KEY) != null ) {
	                    field.set(entity, objectFromValue(field.getType(), dbObject, ID_KEY));
	                }
	            } else if ( mf.hasAnnotation(CollectionName.class) ) {
	                if ( dbObject.get(COLLECTION_NAME_KEY) != null ) {
	                    field.set(entity, dbObject.get(COLLECTION_NAME_KEY).toString());
	                }
	
	            } else if ( mf.hasAnnotation(Reference.class) ) {
	                mapReferencesFromDBObject(dbObject, mf, entity);
	
	            } else if ( mf.hasAnnotation(Embedded.class) ) {
	                mapEmbeddedFromDBObject(dbObject, mf, entity);
	                
	            } else if ( mf.hasAnnotation(Property.class) || mf.isMongoTypeCompatible()) {
	            	mapValuesFromDBObject(dbObject, mf, entity);
	            } else {
	            	logger.warning("Ignoring field: " + field.getName() + " [" + field.getType().getSimpleName() + "]");
	            }
	        }
        } catch (Exception e) {throw new RuntimeException(e);}

        mc.callLifecycleMethods(PostLoad.class, entity, dbObject);
        return entity;
    }

    void mapValuesFromDBObject( BasicDBObject dbObject, MappedField mf, Object entity ) {
        Property propAnnotation = (Property)mf.getAnnotation(Property.class);
        String name = mf.name;
        try {
	        Class fieldType = mf.field.getType();
	        
	        if (mf.isMap()) {
		        if ( dbObject.containsField(name) ) {
		            Map<Object,Object> map = (Map<Object,Object>) dbObject.get(name);
		            Map values = propAnnotation != null ? propAnnotation.mapClass().newInstance() : new HashMap();
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
		            mf.field.set(entity, values);
		
		        } else {
		        	mf.field.set(entity, propAnnotation != null ? propAnnotation.mapClass().newInstance() : new HashMap());
		        }
	    	}else if (mf.isMultipleValues()) {
	            boolean bSet = ReflectionUtils.implementsInterface(fieldType, Set.class);
	
	            if ( dbObject.containsField(name) ) {
	                Class subtype = mf.subType;
	                
	                //for byte[] don't treat it as a multiple values.
	                if (subtype == byte.class && fieldType.isArray()) {
	                	mf.field.set(entity, dbObject.get(name));
	                	return;
	                }
	                //List and Sets are stored as List in mongodb
	                List list = (List) dbObject.get(name);
	                
	                if ( subtype != null ) {
	                    //map back to the java datatype (List/Set/Array[])
	                    Collection values;
	                    if (!bSet)
	                    	values = propAnnotation != null ? propAnnotation.listClass().newInstance() : new ArrayList();
	                    else
	                    	values = propAnnotation != null ? propAnnotation.setClass().newInstance() : new HashSet();
	                    
	                    if (subtype == Locale.class) {
	                        for ( Object o : list )
	                            values.add(parseLocale((String)o));
	                    } else if (subtype == Key.class) {
	                        for ( Object o : list )
	                            values.add(new Key((DBRef)o));
	                    } else if (subtype.isEnum()) {
	                        for ( Object o : list )
	                            values.add(Enum.valueOf(subtype, (String)o));
	                    } else {
	                        for ( Object o : list ) 
	                            values.add(o);
	                    }
	                    if (fieldType.isArray()) {
	                    	Object exampleArray = Array.newInstance(subtype, 1);
	                    	
	                    	if (subtype == Long.class) {
	                    		Object[] array = ((ArrayList)values).toArray((Object[]) exampleArray);
	                    		mf.field.set(entity, array);
	                    	}
	                    }
	                    else
	                    	mf.field.set(entity, values);
	                } else {
	                	mf.field.set(entity, list);
	                }
	
	            } else {
	                if (!bSet)
	            	    mf.field.set(entity, propAnnotation != null ? propAnnotation.listClass().newInstance() : new ArrayList());
	                else
	                    mf.field.set(entity, propAnnotation != null ? propAnnotation.listClass().newInstance() : new HashSet());
	            }
	        } else {
	            if ( dbObject.containsField(name) ) {
	            	mf.field.set(entity, objectFromValue(mf.field.getType(), dbObject, name));
	            }
	        }
    	} catch (Exception e) {throw new RuntimeException(e);}
    }

	void mapEmbeddedFromDBObject( BasicDBObject dbObject, MappedField mf, Object entity ) {
        Embedded mongoEmbedded = (Embedded)mf.getAnnotation(Embedded.class);
        String name = mf.name;

        Class fieldType = mf.field.getType();
        try {
	        if (mf.isMap()) {
	            Class docObjClass = ReflectionUtils.getParameterizedClass(mf.field, 1);
	            Map map = mongoEmbedded.mapClass().newInstance();
	            if ( dbObject.containsField(name) ) {
	                BasicDBObject value = (BasicDBObject) dbObject.get(name);
	                for ( Map.Entry entry : value.entrySet() ) {
	                    Object docObj = createEntityInstanceForDbObject(docObjClass, (BasicDBObject)entry.getValue());
	                    docObj = mapDBObjectToEntity((BasicDBObject)entry.getValue(), docObj);
	                    map.put(entry.getKey(), docObj);
	                }
	            }
	            mf.field.set(entity, map);
	        } else if (mf.isMultipleValues()) {
	            boolean bList = ReflectionUtils.implementsInterface(fieldType, List.class);
	
	        	// multiple documents in a List
	            Class docObjClass = mf.subType;
	            Collection docs = (bList) ? mongoEmbedded.listClass().newInstance() : mongoEmbedded.setClass().newInstance();
	
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
	            mf.field.set(entity, docs);
	        }  else {
	            // single document
	            Class docObjClass = fieldType;
	            if ( dbObject.containsField(name) ) {
	                BasicDBObject docDbObject = (BasicDBObject) dbObject.get(name);
	                Object refObj = createEntityInstanceForDbObject(docObjClass, docDbObject);
	                refObj = mapDBObjectToEntity(docDbObject, refObj);
	                mf.field.set(entity, refObj);
	            }
	        }
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    void mapReferencesFromDBObject( BasicDBObject dbObject, MappedField mf, Object entity ) {
        Reference mongoReference = (Reference)mf.getAnnotation(Reference.class);
        String name = mf.name;

        
        Class fieldType = mf.field.getType();

    	try {        
	    	if (mf.isMap()) {
	            Class referenceObjClass = ReflectionUtils.getParameterizedClass(mf.field, 1);
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
	            mf.field.set(entity, map);
	            
	        } else if (mf.isMultipleValues()) {
	            boolean bSet = ReflectionUtils.implementsInterface(fieldType, Set.class);
	
	            // multiple references in a List
	            Class referenceObjClass = mf.subType;
	            Collection references = bSet ? mongoReference.setClass().newInstance() : mongoReference.listClass().newInstance();
	            
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
	            mf.field.set(entity, references);
	
	        } else {
	        	
	            // single reference
	            Class referenceObjClass = fieldType;
	            if ( dbObject.containsField(name) ) {
	                DBRef dbRef = (DBRef) dbObject.get(name);
	                BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();
	
	                Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
	                refObj = mapDBObjectToEntity(refDbObject, refObj);
	                mf.field.set(entity, refObj);
	            }
	        }
        } catch (Exception e) {throw new RuntimeException(e);}
    }
    
    private static Locale parseLocale(String localeString) {
        if (localeString != null && localeString.length() > 0) {
            StringTokenizer st = new StringTokenizer(localeString, "_");
            String language = st.hasMoreElements() ? st.nextToken() : Locale.getDefault().getLanguage();
            String country = st.hasMoreElements() ? st.nextToken() : "";
            String variant = st.hasMoreElements() ? st.nextToken() : "";
            return new Locale(language, country, variant);
        }
        return null;
    }
    
    /** turns the object intto an ObjectId if it is/should-be one */
	public static Object asObjectIdMaybe(Object id) {
		if ((id instanceof String) && ObjectId.isValid((String)id)) return new ObjectId((String)id);
		return id;
	}
    /** Converts known types from mongodb -> java. Really it just converts enums and locales from strings */
    public static Object objectFromValue( Class c, BasicDBObject dbObject, String name ) {
        if (c == String.class) {
            return dbObject.getString(name);
        } else if (c == Integer.class || c == int.class) {
            return dbObject.getInt(name);
        } else if (c == Long.class || c == long.class) {
            return dbObject.getLong(name);
        } else if (c == Locale.class) {
            return parseLocale(dbObject.getString(name));
        } else if (c.isEnum()) {
            return Enum.valueOf(c, dbObject.getString(name));
        } else if (c == Key.class) {
            return new Key((DBRef)dbObject.get(name));
        }
        return dbObject.get(name);
    }

    /** Converts known types from java -> mongodb. Really it just converts enums and locales to strings */
    public Object objectToValue(Class clazz, Object obj) {

    	if(clazz == null) clazz = obj.getClass();
        if ( clazz.isEnum() ) {
            return ((Enum) obj).name();
        } else if ( clazz == Locale.class ) {
          	return ((Locale) obj).toString();
        } else if ( clazz == Key.class ) {
          	return ((Key) obj).toRef(this);
        } else {
            return obj;
        }
    	
    }
    
    /** Converts known types from java -> mongodb. Really it just converts enums and locales to strings */
    public Object objectToValue(Object obj) {
    	return objectToValue(obj.getClass(), obj);
    }
}
