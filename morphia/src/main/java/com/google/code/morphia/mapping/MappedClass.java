/**
 * 
 */
package com.google.code.morphia.mapping;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.morphia.EntityInterceptor;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.EntityListeners;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Polymorphic;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PostPersist;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.PreSave;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Transient;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.DBObject;

/**
 * Represents a mapped class between the MongoDB DBObject and the java POJO.
 * 
 * This class will validate classes to make sure they meet the requirement for persistence.
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class MappedClass {
    private static final Logger log = Logger.getLogger(MappedClass.class.getName());
	
	private static class ClassMethodPair {
		Class<?> clazz;
		Method method;

		public ClassMethodPair(Class<?> c, Method m) { clazz = c; method =m ; } 
	}
	
    /** special fields representing the Key of the object */
    private Field idField;
	
    /** special annotations representing the type the object */
	private Entity entityAn;
	private Embedded embeddedAn;
//    private Polymorphic polymorphicAn;
	
	/** Annotations we are interested in looking for. */
	public static List<Class<? extends Annotation>> interestingAnnotations = new ArrayList<Class<? extends Annotation>>(Arrays.asList(Embedded.class, Entity.class, Polymorphic.class, EntityListeners.class));
	/** Annotations we were interested in, and found. */
	private Map<Class<? extends Annotation>, Annotation> releventAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
	
	/** Methods which are lifecycle events */
	private Map<Class<? extends Annotation>, List<ClassMethodPair>> lifecycleMethods = new HashMap<Class<? extends Annotation>, List<ClassMethodPair>>();
		
    /** the collectionName based on the type and @Entity value(); this can be overriden by the @CollectionName field on the instance*/
	private String collName;

	/** a list of the fields to map */
	private List<MappedField> persistenceFields = new ArrayList<MappedField>();
	
	/** the type we are mapping to/from */
	private Class clazz;
	private Constructor ctor;
	Mapper mapr;
	
	/** constructor */
	public MappedClass(Class clazz, Mapper mapr) {
		this.mapr = mapr;
        this.clazz = clazz;
        
		for (Class<? extends Annotation> c : interestingAnnotations) {
			addAnnotation(c);
		}
		
		Class type = clazz;
		
		//allows private/protected constructors
		try {
	        ctor = type.getDeclaredConstructor();
	        ctor.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new MappingException("No usable constructor for " + type.getName(), e);
		}
		List<Class<?>> lifecycleClasses = new ArrayList<Class<?>>();
		lifecycleClasses.add(clazz);
		
		EntityListeners entityLisAnn = (EntityListeners) releventAnnotations.get(EntityListeners.class);
		if (entityLisAnn != null && entityLisAnn.value() != null && entityLisAnn.value().length != 0)
			for (Class<?> c : entityLisAnn.value())
				lifecycleClasses.add(c);
		
		Class<? extends Annotation>[] lifecycleAnnotations = new Class[] {PrePersist.class, PreSave.class, PostPersist.class, PreLoad.class, PostLoad.class};
		for (Class<?> cls : lifecycleClasses) {
			for (Method m : ReflectionUtils.getDeclaredAndInheritedMethods(cls)) {
				Class<? extends Annotation> lifecycleType = null;
				
				for(Class<? extends Annotation> c : lifecycleAnnotations) {
					if (m.isAnnotationPresent(c)) {
						lifecycleType = c;
						break;
					}
				}
				
				if (lifecycleType != null)
					addLifecycleEventMethod((Class<Annotation>)lifecycleType, m, cls.equals(clazz) ? null : cls);
			}
		}
		
        embeddedAn = (Embedded)releventAnnotations.get(Embedded.class);
        entityAn = (Entity)releventAnnotations.get(Entity.class);
//        polymorphicAn = (Polymorphic) releventAnnotations.get(Polymorphic.class);
        collName = (entityAn == null || entityAn.value().equals(Mapper.IGNORED_FIELDNAME)) ? clazz.getSimpleName() : entityAn.value();

        for (Field field : ReflectionUtils.getDeclaredAndInheritedFields(clazz, true)) {
        	field.setAccessible(true);
            if (field.isAnnotationPresent(Id.class)) {
            	idField = field;
            	persistenceFields.add(new MappedField(field));   	
            } else if (field.isAnnotationPresent(Transient.class)) {
            	continue;
            } else if (	field.isAnnotationPresent(Property.class) || 
        				field.isAnnotationPresent(Reference.class) || 
        				field.isAnnotationPresent(Embedded.class) || 
        				isSupportedType(field.getType()) ||
        				ReflectionUtils.implementsInterface(field.getType(), Serializable.class)) {
            	persistenceFields.add(new MappedField(field));
            } else {
            	log.warning("Ignoring (will not persist) field: " + clazz.getName() + "." + field.getName() + " [type:" + field.getType().getName() + "]");
            }
        }
	}
	
	private void addLifecycleEventMethod(Class<Annotation> lceClazz, Method m, Class<?> clazz) {
		ClassMethodPair cm = new ClassMethodPair(clazz, m);
		if (lifecycleMethods.containsKey(lceClazz))
			lifecycleMethods.get(lceClazz).add(cm);
		else {
			ArrayList<ClassMethodPair> methods = new ArrayList<ClassMethodPair>();
			methods.add(cm);
			lifecycleMethods.put(lceClazz, methods);
		}
	}
	
	public List<ClassMethodPair> getLifecycleMethods(Class<Annotation> clazz) {
		return lifecycleMethods.get(clazz);
	}
	
	/**
	 * Adds the annotation, if it exists on the field.
	 * @param clazz
	 */
	private void addAnnotation(Class<? extends Annotation> c) {
		Annotation ann = ReflectionUtils.getAnnotation(getClazz(), c);
		if (ann != null)
			releventAnnotations.put(c, ann);
	}

	@Override
	public String toString() {
		return "MappedClass - kind:" + this.getCollectionName() + " for " + this.getClazz().getName() + " fields:" + persistenceFields;
	}

	public List<MappedField> getFieldsAnnotatedWith(Class<? extends Annotation> clazz){
		List<MappedField> results = new ArrayList<MappedField>();
		for(MappedField mf : persistenceFields){
			if(mf.mappingAnnotations.containsKey(clazz))
				results.add(mf);
		}
		return results;
	}

	/** Returns the MappedField by the name that will stored in mongodb */
	public MappedField getMappedField(String name) {
		for(MappedField mf : persistenceFields)
			if (name.equals(mf.getName())) return mf;
	
		return null;
	}
	
	/** Check by the name that will stored in mongodb */
	public boolean containsFieldName(String name) {
		return getMappedField(name)!=null;
	}
	
	public MappedField getMappedFieldByClassField(String name) {
		for(MappedField mf : persistenceFields)
			if (name.equals(mf.getClassFieldName())) return mf;
	
		return null;
	}
	/** Checks to see if it a Map/Set/List or a property supported by the MangoDB java driver*/
	public boolean isSupportedType(Class clazz) {
		if (ReflectionUtils.isPropertyType(clazz)) return true;
		if (clazz.isArray() || ReflectionUtils.implementsAnyInterface(clazz, 	Iterable.class, 
															Collection.class, 
															List.class, 
															Set.class,
															Map.class)){
			Class subType = null;
			if (clazz.isArray()) subType = clazz.getComponentType();
			else subType = ReflectionUtils.getParameterizedClass(clazz);
			
			//get component type, String.class from List<String>
			if (subType != null && subType != Object.class && !ReflectionUtils.isPropertyType(subType))
				return false;
			
			//either no componentType or it is an allowed type
			return true;
		}
		return false;
	}
	
	public void validate() {
		// No @Entity with @Embedded
        if (getEntityAnnotation() != null && getEmbeddedAnnotation() != null ) {
            throw new MappingException(
                    "In [" + getClazz().getName()
                           + "]: Cannot have both @Entity and @Embedded annotation at class level.");
        }

        for (MappedField mf : persistenceFields) {
            if (log.isLoggable(Level.FINE)) {
                log.finer("Processing field: " + mf.getFullName());
            }
            
            //a field can be a Value, Reference, or Embedded
            if ( mf.hasAnnotation(Property.class) ) {
                // make sure that the property type is supported
                if (mf.isSingleValue() && !mf.isTypeMongoCompatible()) {
                    throw new MappingException(mf.getFullName()
                            + " is annotated as @Property but is a type that cannot be mapped (type is "
                            + mf.getType().getName() + ").");
                }
            } else if (mf.hasAnnotation(Reference.class)) {
            	
            	Class realType = (mf.isSingleValue()) ? mf.getType() : mf.getSubType();
            	if ((!realType.isInterface() && mapr.getMappedClass(realType).getIdField()==null))
                    throw new MappingException(mf.getFullName()
                            + " is annotated as a @Reference but the " + mf.getType().getName() + " class is missing the @Id annotation");
            }            
        }
        
        //Only embedded class can have no id field
        if (getIdField() == null && getEmbeddedAnnotation() == null) {
            throw new MappingException("In [" + getClazz().getName() + "]: No field is annotated with @Id; but it is required");
        }
        
        //Embedded classes should not have an id
        if (getEmbeddedAnnotation() != null && getIdField() != null) {
            throw new MappingException("In [" + getClazz().getName() + "]: @Embedded classes cannot specify a @Id field");
        }

        //Embedded classes can not have a fieldName value() specified
        if (getEmbeddedAnnotation() != null && !getEmbeddedAnnotation().value().equals(Mapper.IGNORED_FIELDNAME)) {
            throw new MappingException("In [" + getClazz().getName() + "]: @Embedded classes cannot specify a fieldName value(); this is on applicable on fields");
        }
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Class) return equals((Class)obj);
		else if (obj instanceof MappedClass) return equals((MappedClass)obj);
		else return false;
	}

	public boolean equals(MappedClass clazz) {
		return this.getClazz().equals(clazz);
	}

	public boolean equals(Class clazz) {
		return this.getClazz().equals(clazz);
	}
	
	public DBObject callLifecycleMethods(Class<? extends Annotation> event, Object entity, DBObject dbObj, Mapper mapr) {
		List<ClassMethodPair> methodPairs = getLifecycleMethods((Class<Annotation>)event);
		Collection<EntityInterceptor> interceptors = mapr.getInterceptors();
		
		DBObject retDbObj = dbObj;
		try
		{
			//call interceptors first, then lifecycle events on the @Entity and @EntityListeners
			for (EntityInterceptor ei : interceptors) {
				log.fine("Calling interceptor method " + event.getSimpleName() + " on " + ei );

				if 		(event.equals(PreLoad.class)) 		ei.PreLoad(entity, dbObj, mapr);
				else if (event.equals(PostLoad.class)) 		ei.PostLoad(entity, dbObj, mapr);
				else if	(event.equals(PrePersist.class)) 	ei.PrePersist(entity, dbObj, mapr);
				else if	(event.equals(PreSave.class)) 		ei.PreSave(entity, dbObj, mapr);
				else if (event.equals(PostPersist.class))	ei.PostPersist(entity, dbObj, mapr);					
			}
			
			Object tempObj = null;
			if (methodPairs != null) {
				HashMap<Class<?>, Object> toCall = new HashMap<Class<?>, Object>((int) (methodPairs.size()*1.3));
				for (ClassMethodPair cm : methodPairs)
					toCall.put(cm.clazz, null);
				for (Class<?> c : toCall.keySet())
					if (c != null)
						toCall.put(c, mapr.createInstance(c));

				for (ClassMethodPair cm: methodPairs) {
					Method method = cm.method;
					Class<?> type = cm.clazz;
					
					Object inst = toCall.get(type);
					method.setAccessible(true);
					log.fine("Calling lifecycle method(@" + event.getSimpleName() + " " + method + ") on " + inst + "");
					if (inst == null) 
						if (method.getParameterTypes().length == 0)
							tempObj = method.invoke(entity);
						else
							tempObj = method.invoke(entity, retDbObj);
					else
						if (method.getParameterTypes().length == 0)
							tempObj = method.invoke(inst);
						else if (method.getParameterTypes().length == 1)
							tempObj = method.invoke(inst, entity);
						else
							tempObj = method.invoke(inst, entity, retDbObj);
					
					if (tempObj != null) 
						retDbObj = (DBObject) tempObj;
				}
			}
		}
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
		catch (InvocationTargetException e) { throw new RuntimeException(e); }

		return retDbObj;
	}

	/**
	 * @return the idField
	 */
	public Field getIdField() {
		return idField;
	}

	/**
	 * @return the entityAn
	 */
	public Entity getEntityAnnotation() {
		return entityAn;
	}

	/**
	 * @return the embeddedAn
	 */
	public Embedded getEmbeddedAnnotation() {
		return embeddedAn;
	}

//    public Polymorphic getPolymorphicAnnotation() {
//        return polymorphicAn;
//    }

	/**
	 * @return the releventAnnotations
	 */
	public Map<Class<? extends Annotation>, Annotation> getReleventAnnotations() {
		return releventAnnotations;
	}

	/**
	 * @return the persistenceFields
	 */
	public List<MappedField> getPersistenceFields() {
		return persistenceFields;
	}

	/**
	 * @return the defCollName
	 */
	public String getCollectionName() {
		return collName;
	}

	/**
	 * @return the clazz
	 */
	public Class getClazz() {
		return clazz;
	}
	
	public Constructor getCTor() {
		return ctor;
	}
}