package org.mongodb.morphia.aggregation;

public interface IAccumulator {
	
	Object getValue();
	String getOperation();

}
