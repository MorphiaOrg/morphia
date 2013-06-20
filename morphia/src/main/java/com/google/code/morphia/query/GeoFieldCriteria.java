package com.google.code.morphia.query;


import java.util.Map;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;


public class GeoFieldCriteria extends FieldCriteria {

  final Map<String, Object> opts;

  protected GeoFieldCriteria(final QueryImpl<?> query, final String field, final FilterOperator op, final Object value, final boolean validateNames,
    final boolean validateTypes, final Map<String, Object> opts) {
    super(query, field, op, value, validateNames, validateTypes);
    this.opts = opts;
  }

  @Override
  public void addTo(final DBObject obj) {
    final BasicDBObjectBuilder query;
    switch (operator) {
      case NEAR:
        query = BasicDBObjectBuilder.start(FilterOperator.NEAR.val(), value);
        break;
      case NEAR_SPHERE:
        query = BasicDBObjectBuilder.start(FilterOperator.NEAR_SPHERE.val(), value);
        break;
      case WITHIN_BOX:
        query = BasicDBObjectBuilder.start().push(FilterOperator.WITHIN.val()).add(operator.val(), value);
        break;
      case WITHIN_CIRCLE:
        query = BasicDBObjectBuilder.start().push(FilterOperator.WITHIN.val()).add(operator.val(), value);
        break;
      case WITHIN_CIRCLE_SPHERE:
        query = BasicDBObjectBuilder.start().push(FilterOperator.WITHIN.val()).add(operator.val(), value);
        break;
      default:
        throw new UnsupportedOperationException(operator + " not supported for geo-query");
    }

    //add options...
    if (opts != null) {
      for (final Map.Entry<String, Object> e : opts.entrySet()) {
        query.append(e.getKey(), e.getValue());
      }
    }

    obj.put(field, query.get());
  }
}
