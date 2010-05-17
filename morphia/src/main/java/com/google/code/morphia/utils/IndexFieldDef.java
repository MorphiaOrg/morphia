package com.google.code.morphia.utils;

public class IndexFieldDef {
	String field;
	IndexDirection dir;
	public IndexFieldDef(String field, IndexDirection dir) {
		this.field = field; this.dir = dir;
	}
	
	public String getField() {return field;}
	public IndexDirection getDirection() {return dir;}

	@Override
	public String toString() {
		return field + ":" + dir;
	}
	
	
}
