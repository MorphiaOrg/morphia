package com.google.code.morphia.query;

import com.mongodb.DBObject;

public interface Criteria {
	public void addTo(DBObject obj);
	public void attach(CriteriaContainerImpl container);
}
