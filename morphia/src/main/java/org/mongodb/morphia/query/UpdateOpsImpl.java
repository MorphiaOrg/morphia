package org.mongodb.morphia.query;


import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mongodb.morphia.query.QueryValidator.validateQuery;


/**
 * @author Scott Hernandez
 */
public class UpdateOpsImpl<T> implements UpdateOperations<T> {
    private Map<String, Map<String, Object>> ops = new HashMap<String, Map<String, Object>>();
    private final Mapper mapper;
    private final Class<T> clazz;
    private boolean validateNames = true;
    private boolean validateTypes = true;
    private boolean isolated;

    public UpdateOpsImpl(final Class<T> type, final Mapper mapper) {
        this.mapper = mapper;
        clazz = type;
    }

    public UpdateOperations<T> enableValidation() {
        validateNames = true;
        validateTypes = true;
        return this;
    }

    public UpdateOperations<T> disableValidation() {
        validateNames = false;
        validateTypes = false;
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

        add((addDups) ? UpdateOperator.PUSH : UpdateOperator.ADD_TO_SET, fieldExpr, value, true);
        return this;
    }

    public UpdateOperations<T> addAll(final String fieldExpr, final List<?> values, final boolean addDups) {
        if (values == null || values.isEmpty()) {
            throw new QueryException("Values cannot be null or empty.");
        }

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

    public UpdateOperations<T> max(final String fieldExpr, final Number value) {
        add(UpdateOperator.MAX, fieldExpr, value, false);
        return this;
    }

    public UpdateOperations<T> min(final String fieldExpr, final Number value) {
        add(UpdateOperator.MIN, fieldExpr, value, false);
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
        add(UpdateOperator.PULL, fieldExpr, value, true);
        return this;
    }


    public UpdateOperations<T> removeAll(final String fieldExpr, final List<?> values) {
        if (values == null || values.isEmpty()) {
            throw new QueryException("Value cannot be null or empty.");
        }

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

        add(UpdateOperator.SET, fieldExpr, value, true);
        return this;
    }

    public UpdateOperations<T> setOnInsert(final String fieldExpr, final Object value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }

        add(UpdateOperator.SET_ON_INSERT, fieldExpr, value, true);
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
    protected void add(final UpdateOperator op, final String f, final Object value, final boolean convert) {
        if (value == null) {
            throw new QueryException("Val cannot be null");
        }

        Object val = null;
        MappedField mf = null;
        final StringBuilder sb = new StringBuilder(f);
        if (validateNames || validateTypes) {
            mf = validateQuery(clazz, mapper, sb, FilterOperator.EQUAL, val, validateNames, validateTypes);
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
        ops.get(opString).put(sb.toString(), val);
    }
}
