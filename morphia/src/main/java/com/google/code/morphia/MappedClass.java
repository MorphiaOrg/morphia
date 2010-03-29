/**
 * 
 */
package com.google.code.morphia;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PostPersist;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.PrePersist;
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
    private static final Logger logger = Logger.getLogger(MappedClass.class.getName());
	
    /** special fields representing the Key of the object */
    public Field idField;
	
    /** special annotations representing the type the object */
	public Entity entityAn;
	public Embedded embeddedAn;
	
	/** Annotations we are interested in looking for. */
	protected Class[] classAnnotations = new Class[] {Embedded.class, Entity.class};
	/** Annotations we were interested in, and found. */
	protected Map<Class<Annotation>, Annotation> releventAnnotations = new HashMap<Class<Annotation>, Annotation>();
	
	/** Annotations we are interested in looking for (related to an entity's lifecycle.  */
	public Class[] lifecycleAnnotations = new Class[] {PrePersist.class, PostPersist.class, PreLoad.class, PostLoad.class};
	/** Methods which are lifecycle events */
	public Map<Class<Annotation>, List<Method>> lifecycleMethods = new HashMap<Class<Annotation>, List<Method>>();
		
    /** the collectionName based on the type and @Document value(); this can be overriden by the @CollectionName field on the instance*/
	public String defCollName;

	/** a list of the fields to map */
	public List<MappedField> persistenceFields = new ArrayList<MappedField>();
	
	/** the type we are mapping to/from */
	public Class clazz;

	/** constructor */
	public MappedClass(Class clazz) {
        this.clazz = clazz;

		for (Class<Annotation> c : classAnnotations) {
			addAnnotation(c);
		}

		for(Method m : ReflectionUtils.getDeclaredAndInheritedMethods(clazz)) {
			Class<? extends Annotation> lifecycleType;
			
			if (m.isAnnotationPresent(PrePersist.class))
				lifecycleType = PrePersist.class;
			else if (m.isAnnotationPresent(PostPersist.class))
				lifecycleType = PostPersist.class;
			else if (m.isAnnotationPresent(PreLoad.class))
				lifecycleType = PreLoad.class;
			else if (m.isAnnotationPresent(PostLoad.class))
				lifecycleType = PostLoad.class;
			else
				continue;
			
			addLifecycleEventMethod((Class<Annotation>)lifecycleType, m);
		}

        embeddedAn = (Embedded)this.releventAnnotations.get(Embedded.class);
        entityAn = (Entity)this.releventAnnotations.get(Entity.class);

        
        defCollName = (entityAn == null || entityAn.value().equals(Mapper.IGNORED_FIELDNAME)) ? clazz.getSimpleName() : entityAn.value();
        for (Field field : ReflectionUtils.getDeclaredAndInheritedFields(clazz, false)) {
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
            	logger.warning("Ignoring (will not persist) field: " + clazz.getName() + "." + field.getName() + " [" + field.getType().getSimpleName() + "]");
            }
        }
	}
	
	private void addLifecycleEventMethod(Class<Annotation> clazz, Method m) {
		if (lifecycleMethods.containsKey(clazz))
			lifecycleMethods.get(clazz).add(m);
		else {
			ArrayList<Method> methods = new ArrayList<Method>();
			methods.add(m);
			lifecycleMethods.put(clazz, methods);
		}
	}
	
	public List<Method> getLifecycleMethods(Class<Annotation> clazz) {
		return lifecycleMethods.get(clazz);
	}
	
	/**
	 * Adds the annotation, if it exists on the field.
	 * @param clazz
	 */
	private void addAnnotation(Class<Annotation> c) {
		Annotation ann = ReflectionUtils.getAnnotation(clazz, c);
		if (ann != null)
			this.releventAnnotations.put(c, ann);
	}

	@Override
	public String toString() {
		return "MappedClass - kind:" + this.defCollName + " for " + this.clazz.getSimpleName() + " props:" + this.persistenceFields;
	}

	public <T> List<T> getFieldsAnnotatedWith(Class<T> clazz){
		List<T> results = new ArrayList<T>();
		for(MappedField mf : this.persistenceFields){
			if(mf.mappingAnnotations.containsKey(clazz))
				results.add((T)mf.mappingAnnotations.get(clazz));
		}
		return results;
	}
	
	public boolean containsFieldName(String name) {
		for(MappedField mf : this.persistenceFields)
			if (name.equals(mf.name)) return true;
		
		return false;
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
		// No @Document with @Embedded
        if (entityAn != null && embeddedAn != null ) {
            throw new MappingException(
                    "In [" + clazz.getName()
                           + "]: Cannot have both @Document and @Embedded annotation at class level.");
        }

        for (MappedField mf : persistenceFields) {
            Field field = mf.field;
            Class fieldType = field.getType();
            
        	field.setAccessible(true);
            if (logger.isLoggable(Level.FINE)) {
                logger.finer("In [" + clazz.getName() + "]: Processing field: " + field.getName());
            }
            
            //a field can be a Value, Reference, or Embedded
            if ( mf.hasAnnotation(Property.class) ) {
                // make sure that the property type is supported
                if ( 		!ReflectionUtils.implementsAnyInterface(fieldType, Iterable.class, Collection.class, List.class, Map.class, Set.class)
                        && 	!ReflectionUtils.isPropertyType(field.getType())) {
                	
                    throw new MappingException("In [" + clazz.getName() + "]: Field [" + field.getName()
                            + "] which is annotated as @Value is of type that cannot be mapped (type is "
                            + field.getType().getName() + ").");
                }
            } else if (mf.hasAnnotation(Reference.class)) {
                if ( 		!ReflectionUtils.implementsAnyInterface(fieldType, List.class, Map.class, Set.class)
                        && 	!field.getType().isInterface() 
                        && 	ReflectionUtils.getClassEntityAnnotation(field.getType()) == null) {

                    throw new MappingException(
                            "In ["
                                    + clazz.getName()
                                    + "]: Field ["
                                    + field.getName()
                                    + "] which is annotated as @Reference is of type [" + field.getType().getName() + "] which cannot be referenced.");
                }
            }            
        }
        
        //Only embedded class can have no id field
        if (idField == null && embeddedAn == null) {
            throw new MappingException("In [" + clazz.getName() + "]: No field is annotated with @Id; but it is required");
        }
        
        //Embedded classes should not have an id
        if (embeddedAn != null && idField != null) {
            throw new MappingException("In [" + clazz.getName() + "]: @Embedded classes cannot specify a @Id field");
        }

        //Embedded classes can not have a fieldName value() specified
        if (embeddedAn != null && !embeddedAn.value().equals(Mapper.IGNORED_FIELDNAME)) {
            throw new MappingException("In [" + clazz.getName() + "]: @Embedded classes cannot specify a fieldName value(); this is on applicable on fields");
        }
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Class) return equals((Class)obj);
		else if (obj instanceof MappedClass) return equals((MappedClass)obj);
		else return false;
	}

	public boolean equals(MappedClass clazz) {
		return this.clazz.equals(clazz);
	}

	public boolean equals(Class clazz) {
		return this.clazz.equals(clazz);
	}
	
	/**
	 * Represents the mapping of this field to/from mongodb (name, annotations)
	 * @author Scott Hernandez
	 */
	public static class MappedField {
		protected Field field;
		protected Map<Class<Annotation>,Annotation> mappingAnnotations = new HashMap<Class<Annotation>, Annotation>();
		public String name;
		protected Class[] interestingAnnotations = new Class[] {Indexed.class, Property.class, Reference.class, Embedded.class, Id.class};
		protected Class subType = null;
		protected boolean bSingleValue = true;
		protected boolean bMongoType = false;
		protected boolean bMap = false;
		
		public MappedField(Field f) {
			f.setAccessible(true);
			this.field = f;
			for (Class<Annotation> clazz : interestingAnnotations) {
				addAnnotation(clazz);
			}
			this.name = getMappedFieldName(f);
			Class type = f.getType();
			if (type.isArray() || ReflectionUtils.implementsAnyInterface(field.getType(), Iterable.class, Collection.class, List.class, Set.class, Map.class)) {
				bSingleValue = false;
				// subtype of Long[], List<Long> is Long 
				subType = (type.isArray()) ? type.getComponentType() : ReflectionUtils.getParameterizedClass(f);
				bMap = ReflectionUtils.implementsInterface(type, Map.class);
			}
			//check the main type
			bMongoType = ReflectionUtils.isPropertyType(type);
			// if the main type isn't supported by the Mongo, see if the subtype is
			// works for Long[], List<Long>, etc.
			if (!bMongoType && subType != null) bMongoType = ReflectionUtils.isPropertyType(subType);
		}

		public <T extends Annotation> T getAnnotation(Class<T> clazz) {
			return (T)mappingAnnotations.get(clazz);
		}

		public boolean hasAnnotation(Class ann) {
			return mappingAnnotations.containsKey(ann);
		}

		/**
		 * Adds the annotation, if it exists on the field.
		 * @param clazz
		 */
		public void addAnnotation(Class<Annotation> clazz) {
			if (field.isAnnotationPresent(clazz))
				this.mappingAnnotations.put(clazz, field.getAnnotation(clazz));
		}

		public void validate() {
			if (mappingAnnotations.get(Property.class) != null && mappingAnnotations.get(Embedded.class) != null)
				throw new RuntimeException("@Property and @Embedded cannot be on the same Field: " + field.getName());
			
			if (mappingAnnotations.get(Property.class) != null && mappingAnnotations.get(Reference.class) != null)
				throw new RuntimeException("@Property and @Reference cannot be on the same Field: " + field.getName());

			if (mappingAnnotations.get(Reference.class) != null && mappingAnnotations.get(Embedded.class) != null)
				throw new RuntimeException("@Refernce and @Embedded cannot be on the same Field: " + field.getName());
		}
		/**
		 * Returns the name of the field's key-name for mongodb 
		 */
		public String getMappedFieldName(Field field) {
			if (hasAnnotation(Property.class)){
				Property mv = (Property)mappingAnnotations.get(Property.class);
				if(!mv.value().equals(Mapper.IGNORED_FIELDNAME)) return mv.value();
			} else if (hasAnnotation(Reference.class)){
				Reference mr = (Reference) mappingAnnotations.get(Reference.class);
				if(!mr.value().equals(Mapper.IGNORED_FIELDNAME)) return mr.value();
			} else if (hasAnnotation(Embedded.class)){
				Embedded me = (Embedded)mappingAnnotations.get(Embedded.class);
				if(!me.value().equals(Mapper.IGNORED_FIELDNAME)) return me.value();
			}			
			return field.getName();
		}

		@Override
		public String toString() {
			return name + "; " + this.mappingAnnotations.toString();
		}

		public Class getSubType() {
			return subType;
		}
		
		public boolean isSingleValue() {
			return bSingleValue;
		}

		public boolean isMultipleValues() {
			return !bSingleValue;
		}
		public boolean isMongoTypeCompatible() {
			return bMongoType;
		}

		public boolean isMap() {
			return bMap;
		}

	}

	
	public DBObject callLifecycleMethods(Class<? extends Annotation> event, Object entity, DBObject dbObj) {
		List<Method> methods = getLifecycleMethods((Class<Annotation>)event);
		DBObject retDbObj = dbObj;
		try
		{
			Object tempObj = null;
			if (methods != null)
				for (Method method: methods) {
					method.setAccessible(true);
					if (method.getParameterTypes().length == 0)
						tempObj = method.invoke(entity);
					else
						tempObj = method.invoke(entity, retDbObj);
			
					if (tempObj != null) 
						retDbObj = (DBObject) tempObj;
				}
		}
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
		catch (InvocationTargetException e) { throw new RuntimeException(e); }

		return retDbObj;
	}
}