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

package com.google.code.morphia.mapping;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBBinary;
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
	
	public static final String ID_KEY = "_id";
	public static final String IGNORED_FIELDNAME = ".";
    public static final String CLASS_NAME_FIELDNAME = "className";

    /** Set of classes that have been validated for mapping by this mapper */
    private final ConcurrentHashMap<String,MappedClass> mappedClasses = new ConcurrentHashMap<String, MappedClass>();
    
    private final ThreadLocal<Map<String, Object>> entityCache = new ThreadLocal<Map<String, Object>>();

    public Mapper() {
    }

    public boolean isMapped(Class c) {
        return mappedClasses.containsKey(c.getName());
    }

    public void addMappedClass(Class c) {
    	MappedClass mc = new MappedClass(c, this);
    	mc.validate();
        mappedClasses.put(c.getName(), mc);
    }

    public MappedClass addMappedClass(MappedClass mc) {
    	mc.validate();
        mappedClasses.put(mc.getClazz().getName(), mc);
        return mc;
    }

    public Map<String, MappedClass> getMappedClasses() {
        return mappedClasses;
    }

    /** Gets the mapped class for the object (type). If it isn't mapped, create a new class and cache it (without validating).*/
    public MappedClass getMappedClass(Object obj) {
		if (obj == null) return null;
		Class type = (obj instanceof Class) ? (Class)obj : obj.getClass();
		MappedClass mc = mappedClasses.get(type.getName());
		if (mc == null) {
			//no validation
			mc = new MappedClass(type, this);
			this.mappedClasses.put(mc.getClazz().getName(), mc);
		}
		return mc;
	}

    public void clearHistory() {
        entityCache.remove();
    }

    public String getCollectionName(Object object) {
    	MappedClass mc = getMappedClass(object);
        return mc.getCollectionName();
    }

    private String getId(Object entity) {
        try {
            return (String)getMappedClass(entity).getIdField().get(entity);
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
	public void updateKeyInfo(Object entity, Object dbId, String dbNs) {
		MappedClass mc = getMappedClass(entity);
		//update id field, if there.
		if (mc.getIdField() != null && dbId != null) {
			try {				
				Object dbIdValue = objectFromValue(mc.getIdField().getType(), dbId);
				Object value = mc.getIdField().get(entity);
				if ( value != null ) {
					//The entity already had an id set. Check to make sure it hasn't changed. That would be unexpected, and could indicate a bad state.
			    	if (!dbIdValue.equals(value))
			    		throw new RuntimeException("id mismatch: " + value + " != " + dbIdValue + " for " + entity.getClass().getName());
				} else {
		    		mc.getIdField().set(entity, dbIdValue);
				}
			} catch (Exception e) {
				if (e.getClass().equals(RuntimeException.class)) throw (RuntimeException)e;

				throw new RuntimeException(e);
			}
		}
	}

    Class getClassForName(String className, Class defaultClass) {
    	if (mappedClasses.containsKey(className)) return mappedClasses.get(className).getClazz();
        try {
            Class c = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            return c;
        } catch ( ClassNotFoundException ex ) {
            return defaultClass;
        }
    }

    protected Object createEntityInstanceForDbObject( Class entityClass, BasicDBObject dbObject ) {
        // see if there is a className value
        String className = (String) dbObject.get(CLASS_NAME_FIELDNAME);
        Class c = entityClass;
        if ( className != null ) {
        	//try to Class.forName(className) as defined in the dbObject first, otherwise return the entityClass
            c = getClassForName(className, entityClass);
        }
        return createInstance(c);
    }

    /** Gets a no-arg constructor and calls it via reflection.*/
    protected Object createInstance(Class type) {
        try {
        	//allows private/protected constructors
	        Constructor constructor = type.getDeclaredConstructor();
	        constructor.setAccessible(true);
	        return constructor.newInstance();
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    /** creates an instance of testType (if it isn't Object.class or null) or fallbackType */
    protected Object notObjInst(Class fallbackType, Constructor tryMe) {
    	if (tryMe != null) {
    		tryMe.setAccessible(true);
    		try {
				return tryMe.newInstance();}
    		catch (Exception e) {
				throw new RuntimeException(e);}
    	}
    	return createInstance(fallbackType);
    }
    
    /** coverts a DBObject back to a type-safe java object*/
    public Object fromDBObject(Class entityClass, BasicDBObject dbObject) {
    	if (dbObject == null) {
    		Throwable t = new Throwable();
    		logger.log(Level.SEVERE, "Somebody passes in a null dbObject; bad client!", t);
    		return null;
    	}
    	
    	entityCache.set(new HashMap<String, Object>());
        
        Object entity = createEntityInstanceForDbObject(entityClass, dbObject);
        
        mapDBObjectToEntity(dbObject, entity);

        entityCache.remove();
        return entity;
    }

    /** converts from a java object to a mongo object (possibly a DBObject for complex mappings) */
    public Object toMongoObject(Object javaObj) {
    	Class origClass = javaObj.getClass();
    	Object newObj = objectToValue(origClass, javaObj);
    	Class type = newObj.getClass();
    	boolean bSameType =origClass.equals(type);
    	boolean bSingleValue = true;
    	Class subType = null;
    	
		if (type.isArray() || ReflectionUtils.implementsAnyInterface(type, Iterable.class, Collection.class, List.class, Set.class, Map.class)) {
			bSingleValue = false;
			// subtype of Long[], List<Long> is Long 
			subType = (type.isArray()) ? type.getComponentType() : ReflectionUtils.getParameterizedClass(type);
		}

    	if (bSameType && bSingleValue && !ReflectionUtils.isPropertyType(type)) {
    		DBObject dbObj = toDBObject(javaObj);
    		dbObj.removeField(CLASS_NAME_FIELDNAME);
    		return dbObj;
    	}
    	else if (bSameType && !bSingleValue && !ReflectionUtils.isPropertyType(subType)) {
    		ArrayList<Object> vals = new ArrayList<Object>();
    		if (type.isArray())
	    		for(Object obj : (Object[])newObj)
	    			vals.add(toMongoObject(obj));
    		else
	    		for(Object obj : (Iterable)newObj)
	    			vals.add(toMongoObject(obj));
    		return vals;
    	} else 
    		return newObj;
    }
    
    /** coverts an entity to a DBObject */
    public DBObject toDBObject( Object entity ) {
    	BasicDBObject dbObject = new BasicDBObject();
    	try {
	
	        MappedClass mc = getMappedClass(entity);

            if ( mc.getPolymorphicAnnotation() != null ) {
                dbObject.put(CLASS_NAME_FIELDNAME, entity.getClass().getCanonicalName());
            }
    		
	        dbObject = (BasicDBObject) mc.callLifecycleMethods(PrePersist.class, entity, dbObject);
	        for (MappedField mf : mc.getPersistenceFields()) {
	
	            if ( mf.hasAnnotation(Id.class) ) {
	                Object value = mf.getFieldValue(entity);
	                if ( value != null ) {
	                    dbObject.put(ID_KEY, objectToValue(asObjectIdMaybe(value)));
	                }
	            } else if ( mf.hasAnnotation(Reference.class) ) {
	                mapReferencesToDBObject(entity, mf, dbObject);
	            } else  if (mf.hasAnnotation(Embedded.class)){
	                mapEmbeddedToDBObject(entity, mf, dbObject);
	            } else if (mf.hasAnnotation(Serialized.class) || mf.isTypeMongoCompatible()) {
	            	mapValuesToDBObject(entity, mf, dbObject);
	            } else {
	            	logger.warning("Ignoring field: " + mf.getFullName() + " [type:" + mf.getType().getSimpleName() + "]");
	            }
	        }
        } catch (Exception e) {throw new RuntimeException(e);}
        return dbObject;

    }

    void mapReferencesToDBObject( Object entity, MappedField mf, BasicDBObject dbObject) {
    	try {
	        String name = mf.getName();
	
	        Object fieldValue = mf.getFieldValue(entity);
	        
	        if (mf.isMap()) {
	            Map<Object,Object> map = (Map<Object,Object>) fieldValue;
	            if ( map != null && map.size() > 0) {
	                Map values = (Map)notObjInst(HashMap.class, mf.getCTor());

	                for ( Map.Entry<Object,Object> entry : map.entrySet() ) {
	                    values.put(entry.getKey(), new DBRef(null, getCollectionName(entry.getValue()), asObjectIdMaybe(getId(entry.getValue()))));
	                }
	                if (values.size() > 0) dbObject.put(name, values);
	            }
	    	} else if (mf.isMultipleValues()) {
	    		if (fieldValue != null) {
	                List values = new ArrayList();

		            if (mf.getType().isArray()) {
			            for (Object o : (Object[])fieldValue) {
		                    values.add(new DBRef(null, getCollectionName(o), asObjectIdMaybe(getId(o))));
		                }
		            } else {
			            for (Object o : (Iterable)fieldValue) {
		                    values.add(new DBRef(null, getCollectionName(o), asObjectIdMaybe(getId(o))));
		                }		            	
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
        String name = mf.getName();

        Object fieldValue = null;
		try {
			fieldValue = mf.getFieldValue(entity);
        } catch (Exception e) {throw new RuntimeException(e);}


	    if (mf.isMap()) {
	        Map<String, Object> map = (Map<String, Object>) fieldValue;
	        if ( map != null ) {
	            BasicDBObject values = new BasicDBObject();
	            for ( Map.Entry<String,Object> entry : map.entrySet() ) {
	            	Object meVal = entry.getValue();
	            	DBObject dbObj = toDBObject(meVal);
	            	if (mf.getSubType().equals(meVal.getClass())) 
	            		dbObj.removeField(Mapper.CLASS_NAME_FIELDNAME);
	                values.put(entry.getKey(), dbObj);
	            }
	            if (values.size() > 0) dbObject.put(name, values);
	        }
	
	    } else if (mf.isMultipleValues()) {
            Iterable coll = (Iterable)fieldValue;
            if ( coll != null ) {
                List values = new ArrayList();
                for ( Object o : coll ) {                	
	            	DBObject dbObj = toDBObject(o);
	            	if (mf.getSubType().equals(o.getClass())) 
	            		dbObj.removeField(Mapper.CLASS_NAME_FIELDNAME);
                    values.add(dbObj);
                }
                if (values.size()>0) dbObject.put(name, values);
            }
        } else {
        	DBObject dbObj = fieldValue == null ? null : toDBObject(fieldValue);
            if ( dbObj != null && dbObj.keySet().size() > 0) {
            	if (mf.getType().equals(fieldValue.getClass())) 
            		dbObj.removeField(Mapper.CLASS_NAME_FIELDNAME);
            	dbObject.put(name, dbObj);
            }
        }
    }

    void mapValuesToDBObject( Object entity, MappedField mf, BasicDBObject dbObject ) {
        try {
	    	String name = mf.getName();
	        Class fieldType = mf.getType();
	        Object fieldValue = mf.getFieldValue(entity);
	        boolean isSerialized = mf.hasAnnotation(Serialized.class);
	        
	        if (isSerialized) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(fieldValue);
				dbObject.put(name, baos.toByteArray());
	        }
	        //sets and list are stored in mongodb as ArrayLists
	        else if (mf.isMap()) {
	            Map<Object,Object> map = (Map<Object,Object>) mf.getFieldValue(entity);
	            if (map != null && map.size() > 0) {
	                Map mapForDb = new HashMap();
	                for ( Map.Entry<Object,Object> entry : map.entrySet() ) {
	                	mapForDb.put(entry.getKey(), objectToValue(entry.getValue()));
	                }
	                dbObject.put(name, mapForDb);
	            }
	        } else if (mf.isMultipleValues()) {
	        	Class paramClass = mf.getSubType();
	            if (fieldValue != null) {
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
	        	Object val = objectToValue(fieldValue);
	            if ( val != null ) {
	            	dbObject.put(name, val);
	            }
	        }
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    Object mapDBObjectToEntity( BasicDBObject dbObject, Object entity ) {
        // check the history key (a key is the namespace + id)
        String cacheKey = (!dbObject.containsField(ID_KEY)) ? null : "[" + dbObject.getString(ID_KEY) + "]";
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

        dbObject = (BasicDBObject) mc.callLifecycleMethods(PreLoad.class, entity, dbObject);
        try {
	        for (MappedField mf : mc.getPersistenceFields()) {
	            if ( mf.hasAnnotation(Id.class) ) {
	                if ( dbObject.get(ID_KEY) != null ) {
	                    mf.setFieldValue(entity, objectFromValue(mf.getType(), dbObject, ID_KEY));
	                }
	
	            } else if ( mf.hasAnnotation(Reference.class) ) {
	                mapReferencesFromDBObject(dbObject, mf, entity);
	
	            } else if ( mf.hasAnnotation(Embedded.class) ) {
	                mapEmbeddedFromDBObject(dbObject, mf, entity);
	                
	            } else if ( mf.hasAnnotation(Property.class) || mf.hasAnnotation(Serialized.class) || mf.isTypeMongoCompatible()) {
	            	mapValuesFromDBObject(dbObject, mf, entity);
	            } else {
	            	logger.warning("Ignoring field: " + mf.getFullName() + " [type:" + mf.getType().getName() + "]");
	            }
	        }
        } catch (Exception e) {throw new RuntimeException(e);}

        mc.callLifecycleMethods(PostLoad.class, entity, dbObject);
        return entity;
    }

    void mapValuesFromDBObject( BasicDBObject dbObject, MappedField mf, Object entity ) {
        String name = mf.getName();
        try {
	        Class fieldType = mf.getType();
	        boolean isSerialized = mf.hasAnnotation(Serialized.class);
	        
	        if (isSerialized) {
	        	Object data = dbObject.get(name);
	        	if (!(data instanceof DBBinary || data instanceof byte[]))
	        		throw new MappingException("The stored data is not a DBBinary or byte[] instance for " + mf.getFullName()+ " ; it is a " + data.getClass().getName());

				try
				{
					ByteArrayInputStream bais;
					if (data instanceof DBBinary)
						bais = new ByteArrayInputStream(((DBBinary)data).getData());
					else 
						bais = new ByteArrayInputStream((byte[])data);
					
					ObjectInputStream ois = new ObjectInputStream(bais);

					mf.setFieldValue(entity, ois.readObject());
				}
				catch (IOException ex) { throw new RuntimeException(ex); }
				catch (ClassNotFoundException ex) { throw new IllegalStateException("Unable to deserialize " + data + " on field " + mf.getFullName() , ex); }
	        } else if (mf.isMap()) {
		        if ( dbObject.containsField(name) ) {
		            Map<Object,Object> map = (Map<Object,Object>) dbObject.get(name);
	                Map values = (Map)notObjInst(HashMap.class, mf.getCTor());
		            for ( Map.Entry<Object,Object> entry : map.entrySet() ) {
		            	values.put(entry.getKey(), objectFromValue(fieldType, entry.getValue()));
		            }
		            mf.setFieldValue(entity, values);
		        }
	    	}else if (mf.isMultipleValues()) {
	            if ( dbObject.containsField(name) ) {
	                Class subtype = mf.getSubType();
	                
	                //for byte[] don't treat it as a multiple values.
	                if (subtype == byte.class && fieldType.isArray()) {
	                	mf.setFieldValue(entity, dbObject.get(name));
	                	return;
	                }
	                //List and Sets are stored as List in mongodb
	                List list = (List) dbObject.get(name);
	                
	                if ( subtype != null ) {
	                    //map back to the java datatype (List/Set/Array[])
	                    Collection values;
	                    
	                    if (!mf.isSet())
	    	                values = (List)notObjInst(ArrayList.class, mf.getCTor());
	                    else
	    	                values = (Set)notObjInst(HashSet.class, mf.getCTor());
	                    
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
	                    		mf.setFieldValue(entity, array);
	                    	}
	                    }
	                    else
	                    	mf.setFieldValue(entity, values);
	                } else {
	                	mf.setFieldValue(entity, list);
	                }
	            }
	        } else {
	            if ( dbObject.containsField(name) ) {
	            	mf.setFieldValue(entity, objectFromValue(fieldType, dbObject, name));
	            }
	        }
    	} catch (Exception e) {throw new RuntimeException(e);}
    }

	void mapEmbeddedFromDBObject( BasicDBObject dbObject, MappedField mf, Object entity ) {
        String name = mf.getName();

        Class fieldType = mf.getType();
        try {
	        if (mf.isMap()) {
	            Class docObjClass = mf.getSubType();
	            Map map = (Map)notObjInst(HashMap.class, mf.getCTor());

	            if ( dbObject.containsField(name) ) {
	                BasicDBObject value = (BasicDBObject) dbObject.get(name);
	                for ( Map.Entry entry : value.entrySet() ) {
	                    Object docObj = createEntityInstanceForDbObject(docObjClass, (BasicDBObject)entry.getValue());
	                    docObj = mapDBObjectToEntity((BasicDBObject)entry.getValue(), docObj);
	                    map.put(entry.getKey(), docObj);
	                }
	            }
	            mf.setFieldValue(entity, map);
	        } else if (mf.isMultipleValues()) {
	        	// multiple documents in a List
	            Class docObjClass = mf.getSubType();
	            Collection docs = (Collection)notObjInst((!mf.isSet()) ? ArrayList.class : HashSet.class, mf.getCTor());
	
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
	            mf.setFieldValue(entity, docs);
	        }  else {
	            // single document
	            Class docObjClass = fieldType;
	            if ( dbObject.containsField(name) ) {
	                BasicDBObject docDbObject = (BasicDBObject) dbObject.get(name);
	                Object refObj = createEntityInstanceForDbObject(docObjClass, docDbObject);
	                refObj = mapDBObjectToEntity(docDbObject, refObj);
	                mf.setFieldValue(entity, refObj);
	            }
	        }
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    void mapReferencesFromDBObject( BasicDBObject dbObject, MappedField mf, Object entity ) {
        String name = mf.getName();

        Class fieldType = mf.getType();

    	try {
	    	if (mf.isMap()) {
	            Class referenceObjClass = mf.getSubType();
	            Map map = (Map)notObjInst(HashMap.class, mf.getCTor());

	            if ( dbObject.containsField(name) ) {
	                BasicDBObject value = (BasicDBObject) dbObject.get(name);
	                for ( Map.Entry entry : value.entrySet() ) {
	                    DBRef dbRef = (DBRef) entry.getValue();
	                    BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

                        // handle broken references with "no action"
                        if (refDbObject != null) {
                            Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                            refObj = mapDBObjectToEntity(refDbObject, refObj);
                            map.put(entry.getKey(), refObj);
                        }
	                }
	            }
	            mf.setFieldValue(entity, map);
	            
	        } else if (mf.isMultipleValues()) {
	            // multiple references in a List
	            Class referenceObjClass = mf.getSubType();
	            Collection references = (Collection) notObjInst((!mf.isSet()) ? ArrayList.class : HashSet.class, mf.getCTor());
	        	
	            if ( dbObject.containsField(name) ) {
	                Object value = dbObject.get(name);
	                if ( value instanceof List ) {
	                    List refList = (List) value;
	                    for ( Object dbRefObj : refList ) {
	                        DBRef dbRef = (DBRef) dbRefObj;
	                        BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

                            // handle broken references with "no action"
                            if (refDbObject != null) {
                                Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                                refObj = mapDBObjectToEntity(refDbObject, refObj);
                                references.add(refObj);
                            }
	                    }
	                } else {
	                    DBRef dbRef = (DBRef) dbObject.get(name);
	                    BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

                        // handle broken references with "no action"
                        if (refDbObject != null) {
                            Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                            refObj = mapDBObjectToEntity(refDbObject, refObj);
                            references.add(refObj);
                        }
	                }
	            }
	            
	            mf.setFieldValue(entity, references);
	        } else {
	            // single reference
	            Class referenceObjClass = fieldType;
	            if ( dbObject.containsField(name) ) {
	                DBRef dbRef = (DBRef) dbObject.get(name);
	                BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

                    // handle broken references with "no action"
                    if (refDbObject != null) {
                        Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                        refObj = mapDBObjectToEntity(refDbObject, refObj);
                        mf.setFieldValue(entity, refObj);
                    }
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
    
    /** turns the object into an ObjectId if it is/should-be one */
	public static Object asObjectIdMaybe(Object id) {
		try {
			if (id instanceof String && ObjectId.isValid((String)id))
				return new ObjectId((String)id);
		} catch (Exception e) {
			//sometimes isValid throws exceptions... bad!
		}
		return id;
	}
	
    /** Converts known types from mongodb -> java. Really it just converts enums and locales from strings */
    public static Object objectFromValue( Class javaType, BasicDBObject dbObject, String name ) {
    	return objectFromValue(javaType, dbObject.get(name));
    }

    protected static Object objectFromValue( Class javaType, Object val) {
        if (javaType == String.class) {
            return val.toString();
        } else if (javaType == Character.class || javaType == char.class) {
            return val.toString().charAt(0);
        } else if (javaType == Integer.class || javaType == int.class) {
            return ((Number)val).intValue();
        } else if (javaType == Long.class || javaType == long.class) {
            return ((Number)val).longValue();
        } else if (javaType == Byte.class || javaType == byte.class) {
           	Object dbValue = val;
        	if (dbValue instanceof Byte) return dbValue;
        	else if (dbValue instanceof Double) return ((Double)dbValue).byteValue();
        	else if (dbValue instanceof Integer) return ((Integer)dbValue).byteValue();
        	String sVal = val.toString();
            return Byte.parseByte(sVal);
        } else if (javaType == Short.class || javaType == short.class) {
           	Object dbValue = val;
        	if (dbValue instanceof Short) return dbValue;
        	else if (dbValue instanceof Double) return ((Double)dbValue).shortValue();
        	else if (dbValue instanceof Integer) return ((Integer)dbValue).shortValue();
        	String sVal = val.toString();
            return Short.parseShort(sVal);
        } else if (javaType == Float.class || javaType == float.class) {
        	Object dbValue = val;
        	if (dbValue instanceof Double) return ((Double)dbValue).floatValue();
        	String sVal = val.toString();
            return Float.parseFloat(sVal);
        } else if (javaType == Locale.class) {
            return parseLocale(val.toString());
        } else if (javaType.isEnum()) {
            return Enum.valueOf(javaType, val.toString());
        } else if (javaType == Key.class) {
            return new Key((DBRef)val);
        }
        return val;
    }

    /** Converts known types from java -> mongodb. Really it just converts enums and locales to strings */
    public Object objectToValue(Class javaType, Object obj) {
    	if (obj == null) return null;
    	if (javaType == null) javaType = obj.getClass();

    	if ( javaType.isEnum() ) {
            return ((Enum) obj).name();
        } else if ( javaType == Locale.class ) {
          	return ((Locale) obj).toString();
        } else if ( javaType == char.class ||  javaType == Character.class ) {
        	return ((Character)obj).toString();
        } else if ( javaType == Key.class ) {
          	return ((Key) obj).toRef(this);
        } else {
            return obj;
        }
    	
    }
    
    /** Converts known types from java -> mongodb. Really it just converts enums and locales to strings */
    public Object objectToValue(Object obj) {
    	if (obj == null) return null;
    	return objectToValue(obj.getClass(), obj);
    }
}
