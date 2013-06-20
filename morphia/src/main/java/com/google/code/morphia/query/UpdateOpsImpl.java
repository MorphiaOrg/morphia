package com.google.code.morphia.query;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.Mapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


/**
 * @author Scott Hernandez
 */
public class UpdateOpsImpl<T> implements UpdateOperations<T> {
  Map<String, Map<String, Object>> ops = new HashMap<String, Map<String, Object>>();
  final Mapper mapper;
  final Class<T> clazz;
  boolean validateNames = true;
  boolean validateTypes = true;
  boolean isolated;

  public UpdateOpsImpl(final Class<T> type, final Mapper mapper) {
    this.mapper = mapper;
    clazz = type;
  }

  public UpdateOperations<T> enableValidation() {
    validateNames = validateTypes = true;
    return this;
  }

  public UpdateOperations<T> disableValidation() {
    validateNames = validateTypes = false;
    return this;
  }

  public UpdateOperations<T> isolated() {
    isolated = true;
    return this;
  }

  public boolean isIsolated() {
    return isolated;
  }

  @SuppressWarnings("unchecked")
  public void setOps(final DBObject ops) {
    this.ops = (Map<String, Map<String, Object>>) ops;
  }

  public DBObject getOps() {
    return new BasicDBObject(ops);
  }

  public UpdateOperations<T> add(final String fieldExpr, final Object value) {
    return add(fieldExpr, value, false);
  }


  public UpdateOperations<T> add(final String fieldExpr, final Object value, final boolean addDups) {
    if (value == null) {
      throw new QueryException("Value cannot be null.");
    }

    //		Object dbObj = mapper.toMongoObject(value, true);
    add((addDups) ? UpdateOperator.PUSH : UpdateOperator.ADD_TO_SET, fieldExpr, value, true);
    return this;
  }

  public UpdateOperations<T> addAll(final String fieldExpr, final List<?> values, final boolean addDups) {
    if (values == null || values.isEmpty()) {
      throw new QueryException("Values cannot be null or empty.");
    }

    //		List<?> convertedValues = (List<?>)mapper.toMongoObject(values, true);
    if (addDups) {
      add(UpdateOperator.PUSH_ALL, fieldExpr, values, true);
    } else {
      add(UpdateOperator.ADD_TO_SET_EACH, fieldExpr, values, true);
    }
    return this;
  }

  public UpdateOperations<T> dec(final String fieldExpr) {
    return inc(fieldExpr, -1);
  }


  public UpdateOperations<T> inc(final String fieldExpr) {
    return inc(fieldExpr, 1);
  }


  public UpdateOperations<T> inc(final String fieldExpr, final Number value) {
    if (value == null) {
      throw new QueryException("Value cannot be null.");
    }
    add(UpdateOperator.INC, fieldExpr, value, false);
    return this;
  }


  protected UpdateOperations<T> remove(final String fieldExpr, final boolean firstNotLast) {
    add(UpdateOperator.POP, fieldExpr, (firstNotLast) ? -1 : 1, false);
    return this;
  }


  public UpdateOperations<T> removeAll(final String fieldExpr, final Object value) {
    if (value == null) {
      throw new QueryException("Value cannot be null.");
    }
    //		Object dbObj = mapper.toMongoObject(value);
    add(UpdateOperator.PULL, fieldExpr, value, true);
    return this;
  }


  public UpdateOperations<T> removeAll(final String fieldExpr, final List<?> values) {
    if (values == null || values.isEmpty()) {
      throw new QueryException("Value cannot be null or empty.");
    }

    //		List<Object> values = toDBObjList(values);
    add(UpdateOperator.PULL_ALL, fieldExpr, values, true);
    return this;
  }


  public UpdateOperations<T> removeFirst(final String fieldExpr) {
    return remove(fieldExpr, true);
  }


  public UpdateOperations<T> removeLast(final String fieldExpr) {
    return remove(fieldExpr, false);
  }

  public UpdateOperations<T> set(final String fieldExpr, final Object value) {
    if (value == null) {
      throw new QueryException("Value cannot be null.");
    }

    //		Object dbObj = mapper.toMongoObject(value, true);
    add(UpdateOperator.SET, fieldExpr, value, true);
    return this;
  }

  public UpdateOperations<T> unset(final String fieldExpr) {
    add(UpdateOperator.UNSET, fieldExpr, 1, false);
    return this;
  }

  protected List<Object> toDBObjList(final MappedField mf, final List<?> values) {
    final List<Object> list = new ArrayList<Object>(values.size());
    for (final Object obj : values) {
      list.add(mapper.toMongoObject(mf, null, obj));
    }

    return list;
  }

  //TODO Clean this up a little.
  protected void add(final UpdateOperator op, String f, final Object value, final boolean convert) {
    if (value == null) {
      throw new QueryException("Val cannot be null");
    }

    Object val = null;
    MappedField mf = null;
    if (validateNames || validateTypes) {
      final StringBuffer sb = new StringBuffer(f);
      mf = Mapper.validate(clazz, mapper, sb, FilterOperator.EQUAL, val, validateNames, validateTypes);
      f = sb.toString();
    }

    if (convert) {
      if (UpdateOperator.PULL_ALL.equals(op) && value instanceof List) {
        val = toDBObjList(mf, (List<?>) value);
      } else {
        val = mapper.toMongoObject(mf, null, value);
      }
    }


    if (UpdateOperator.ADD_TO_SET_EACH.equals(op)) {
      val = new BasicDBObject(UpdateOperator.EACH.val(), val);
    }

    if (val == null) {
      val = value;
    }

    final String opString = op.val();

    if (!ops.containsKey(opString)) {
      ops.put(opString, new HashMap<String, Object>());
    }
    ops.get(opString).put(f, val);
  }
}
