/**
 * 
 */
package com.google.code.morphia;

import com.google.code.morphia.mapping.Mapper;
import com.mongodb.DBObject;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class AbstractEntityInterceptor implements EntityInterceptor {
	
	public void PostLoad(Object ent, DBObject dbObj, Mapper mapr) {
	}
	
	public void PostPersist(Object ent, DBObject dbObj, Mapper mapr) {
	}
	
	public void PreLoad(Object ent, DBObject dbObj, Mapper mapr) {
	}
	
	public void PrePersist(Object ent, DBObject dbObj, Mapper mapr) {
	}
	
	public void PreSave(Object ent, DBObject dbObj, Mapper mapr) {
	}
}
