package org.mongodb.morphia.query;


import com.mongodb.DBObject;


/**
 * Internal class for building up query documents.
 */
public interface Criteria {
  void addTo(DBObject obj);

  void attach(CriteriaContainerImpl container);

  String getFieldName();
}
