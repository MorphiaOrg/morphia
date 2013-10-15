/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mongodb.morphia.mapping;

import com.google.code.morphia.mapping.cache.EntityCache;
import com.mongodb.DBObject;

/**
 *
 * @author Nick Artman
 */

public interface MapperReferenceExceptionHandler {
	
	public void handleMappedFieldException(DBObject dbObject, MappedField mf, Object entity, EntityCache cache, Exception e);
	
}
