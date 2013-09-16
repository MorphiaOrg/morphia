package org.mongodb.morphia.query;


import org.bson.types.CodeWScope;
import com.mongodb.DBObject;


public class WhereCriteria extends AbstractCriteria {

  private final Object js;

  public WhereCriteria(final String js) {
    this.js = js;
  }

  public WhereCriteria(final CodeWScope js) {
    this.js = js;
  }

  public void addTo(final DBObject obj) {
    obj.put(FilterOperator.WHERE.val(), js);
  }

  public String getFieldName() {
    return FilterOperator.WHERE.val();
  }

}
