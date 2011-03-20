/**
 * 
 */
package com.google.code.morphia.ext.entityscanner;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

@Entity
class E {
	@Id
	ObjectId id;
}