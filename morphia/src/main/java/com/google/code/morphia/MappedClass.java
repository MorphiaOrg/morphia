/**
 * 
 */
package com.google.code.morphia;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.morphia.annotations.CollectionName;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Transient;
import com.google.code.morphia.utils.IndexDirection;
import com.google.code.morphia.utils.ReflectionUtils;

/**
 * Represents a mapped class between the MongoDB DBObject and the java POJO.
 * 
 * This class will validate classes to make sure they meet the requirement for persistence.
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class MappedClass {
    public class SuggestedIndex {
		String name;
		IndexDirection dir;
		public SuggestedIndex(String n, IndexDirection d) {name = n; dir = d;}
		public String getName() {return name;}
		public IndexDirection getDirection() {return dir;}
	}

	private static final Logger logger = Logger.getLogger(MappedClass.class.getName());
	
    /** special fields representing the Key of the object */
    public Field idField, collectionNameField;
	
    /** special annotations representing the type the object */
	public Entity entityAn;
	public Embedded embeddedAn;
	
	public Class[] interestingAnnotations = new Class[] {Embedded.class, Entity.class};
	protected Map<Class<Annotation>,Annotation> relAnnotations = new HashMap<Class<Annotation>, Annotation>();

	public List<SuggestedIndex> suggestedIndexes = new ArrayList<SuggestedIndex>();
	
    /** the collectionName based on the type and @Document value(); this can be overriden by the @CollectionName field on the instance*/
	public String defCollName;

	/** a list of the fields to map */
	public List<MappedField> persistenceFields = new ArrayList<MappedField>();
	
	/** the type we are mapping to/from */
	public Class clazz;

	public MappedClass(Class clazz) {
        this.clazz = clazz;

		for (Class<Annotation> c : interestingAnnotations) {
			addAnnotation(c);
		}


        embeddedAn = ReflectionUtils.getClassEmbeddedAnnotation(clazz);
        entityAn = ReflectionUtils.getClassEntityAnnotation(clazz);

        defCollName = (entityAn == null || entityAn.value().equals(Mapper.IGNORED_FIELDNAME)) ? clazz.getSimpleName() : entityAn.value();
        for (Field field : ReflectionUtils.getDeclaredAndInheritedFields(clazz, false)) {
            if (field.isAnnotationPresent(Id.class)) {
                idField = field;
            	persistenceFields.add(new MappedField(field));   	
            } else if (field.isAnnotationPresent(Transient.class)) {
            	continue;
            } else if (field.isAnnotationPresent(CollectionName.class)) {
            	collectionNameField = field;
            	persistenceFields.add(new MappedField(field));
            } else if (	field.isAnnotationPresent(Property.class) || 
        				field.isAnnotationPresent(Reference.class) || 
        				field.isAnnotationPresent(Embedded.class) || 
        				isSupportedType(field.getType())) {
            
            	persistenceFields.add(new MappedField(field));   	
            } else {
            	logger.warning("Ignoring (will not persist) field: " + clazz.getName() + "." + field.getName() + " [" + field.getType().getSimpleName() + "]");
            }
        }
        
	}
	/**
	 * Adds the annotation, if it exists on the field.
	 * @param clazz
	 */
	private void addAnnotation(Class<Annotation> c) {
		if (clazz.isAnnotationPresent(c))
			this.relAnnotations.put(clazz, clazz.getAnnotation(c));
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
		return  ReflectionUtils.isValidMapValueType(clazz) || 
				ReflectionUtils.implementsAnyInterface(clazz, List.class, Map.class, Set.class);
	}
	
	public void validate() {
		// No @Document with @Embedded
        if (entityAn != null && embeddedAn != null ) {
            throw new MongoMappingException(
                    "In [" + clazz.getName()
                           + "]: Cannot have both @Document and @Embedded annotation at class level.");
        }

        for (MappedField mf : persistenceFields) {
            Field field = mf.field;
            String mappedName = mf.name;
            Class fieldType = field.getType();
            
        	field.setAccessible(true);
            if (logger.isLoggable(Level.FINE)) {
                logger.finer("In [" + clazz.getName() + "]: Processing field: " + field.getName());
            }

            //a field can be a Value, Reference, or Embedded
            if ( mf.hasAnnotation(Property.class) ) {
                // make sure that the property type is supported
                if ( 		!ReflectionUtils.implementsAnyInterface(fieldType, List.class, Map.class, Set.class)
                        && 	!ReflectionUtils.isPropertyType(field.getType())) {
                	
                    throw new MongoMappingException("In [" + clazz.getName() + "]: Field [" + field.getName()
                            + "] which is annotated as @Value is of type that cannot be mapped (type is "
                            + field.getType().getName() + ").");
                }
            } else if (mf.hasAnnotation(Reference.class)) {
                if ( 		!ReflectionUtils.implementsAnyInterface(fieldType, List.class, Map.class, Set.class)
                        && 	!field.getType().isInterface() 
                        && 	ReflectionUtils.getClassEntityAnnotation(field.getType()) == null) {

                    throw new MongoMappingException(
                            "In ["
                                    + clazz.getName()
                                    + "]: Field ["
                                    + field.getName()
                                    + "] which is annotated as @Reference is of type [" + field.getType().getName() + "] which cannot be referenced.");
                }
            }
            if (mf.hasAnnotation(Indexed.class)) {
    			Indexed index = field.getAnnotation(Indexed.class);
            	suggestedIndexes.add(new SuggestedIndex(mappedName, index.value()));
            }
            
        }
        

        // make sure @CollectionName field is a String
        if (collectionNameField != null && collectionNameField.getType() != String.class) {
            throw new MongoMappingException("In [" + clazz.getName() + "]: Field [" + collectionNameField.getName()
                    + "] which is annotated as @CollectionName must be of type java.lang.String, but is of type: "
                    + collectionNameField.getType().getName());
        }
        
        //Only embedded class can have no id field
        if (idField == null && embeddedAn == null) {
            throw new MongoMappingException("In [" + clazz.getName() + "]: No field is annotated with @Id; but it is required");
        }

        
        //Embedded classes should not have an id
        if (embeddedAn != null && idField != null) {
            throw new MongoMappingException("In [" + clazz.getName() + "]: @Embedded classes cannot specify a @Id field");
        }

        //Embedded classes should not have a CollectionName
        if (embeddedAn != null && collectionNameField != null) {
            throw new MongoMappingException("In [" + clazz.getName() + "]: @Embedded classes cannot specify a @CollectionName field");
        }

        //Embedded classes can not have a fieldName value() specified
        if (embeddedAn != null && !embeddedAn.value().equals(Mapper.IGNORED_FIELDNAME)) {
            throw new MongoMappingException("In [" + clazz.getName() + "]: @Embedded classes cannot specify a fieldName value(); this is on applicable on fields");
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
		protected Class[] interestingAnnotations = new Class[] {Property.class, Reference.class, Embedded.class, Id.class, CollectionName.class};
		
		public MappedField(Field f) {
			this.field = f;
			for (Class<Annotation> clazz : interestingAnnotations) {
				addAnnotation(clazz);
			}
			this.name = getMappedFieldName(f);
		}

		public Annotation getAnnotation(Class clazz) {
			return mappingAnnotations.get(clazz);
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

	}
}