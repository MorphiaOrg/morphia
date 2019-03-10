package xyz.morphia.query;


import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static xyz.morphia.query.CriteriaJoin.AND;

/**
 * Defines a container of Criteria and a join method.
 *
 * @see CriteriaJoin
 */
public class CriteriaContainerImpl extends AbstractCriteria implements CriteriaContainer {
    private CriteriaJoin joinMethod;
    private List<Criteria> children;

    private QueryImpl<?> query;

    protected CriteriaContainerImpl(final QueryImpl<?> query, final CriteriaJoin joinMethod) {
        this(joinMethod);
        this.query = query;
    }

    protected CriteriaContainerImpl(final CriteriaJoin joinMethod) {
        this.joinMethod = joinMethod;
        children = new ArrayList<Criteria>();
    }

    @Override
    public void add(final Criteria... criteria) {
        for (final Criteria c : criteria) {
            c.attach(this);
            children.add(c);
        }
    }

    @Override
    public CriteriaContainer and(final Criteria... criteria) {
        return collect(AND, criteria);
    }

    @Override
    public FieldEnd<? extends CriteriaContainer> criteria(final String name) {
        return new FieldEndImpl<CriteriaContainerImpl>(query, name, this);
    }

    @Override
    public CriteriaContainer or(final Criteria... criteria) {
        return collect(CriteriaJoin.OR, criteria);
    }

    @Override
    public void remove(final Criteria criteria) {
        children.remove(criteria);
    }

    @Override
    public DBObject toDBObject() {
        DBObject dbObject = new BasicDBObject();
        if (joinMethod == AND) {
            final Set<String> fields = new HashSet<String>();
            int nonNullFieldNames = 0;
            for (final Criteria child : children) {
                if (null != child.getFieldName()) {
                    fields.add(child.getFieldName());
                    nonNullFieldNames++;
                }
            }
            if (fields.size() < nonNullFieldNames) {
                final BasicDBList and = new BasicDBList();

                for (final Criteria child : children) {
                    and.add(child.toDBObject());
                }

                dbObject.put("$and", and);
            } else {
                //no dup field names, don't use $and
                for (final Criteria child : children) {
                    dbObject.putAll(child.toDBObject());
                }
            }
        } else if (joinMethod == CriteriaJoin.OR) {
            final BasicDBList or = new BasicDBList();

            for (final Criteria child : children) {
                or.add(child.toDBObject());
            }

            dbObject.put("$or", or);
        }
        return dbObject;
    }

    @Override
    public String getFieldName() {
        return joinMethod.toString();
    }

    /**
     * @return the Query for this CriteriaContainer
     */
    public QueryImpl<?> getQuery() {
        return query;
    }

    /**
     * Sets the Query for this CriteriaContainer
     *
     * @param query the query
     */
    public void setQuery(final QueryImpl<?> query) {
        this.query = query;
    }

    private CriteriaContainer collect(final CriteriaJoin cj, final Criteria... criteria) {
        final CriteriaContainerImpl parent = new CriteriaContainerImpl(query, cj);

        for (final Criteria c : criteria) {
            parent.add(c);
        }

        add(parent);

        return parent;
    }

    @Override
    public String toString() {
        return toDBObject().toString();
    }
}
