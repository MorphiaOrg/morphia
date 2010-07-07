package com.google.code.morphia.utils;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

/**
 * Used to store counters for other entities.
 * @author skot
 *
 */

@Entity(value="ids", noClassnameStored=true)
public class StoredId {
	final @Id String className;
	Long value = 1L;
	
	public StoredId(String name) {
		className = name;
	}
	
	protected StoredId(){
		className = "";
	}
}
