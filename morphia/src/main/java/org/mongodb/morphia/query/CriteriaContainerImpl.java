package org.mongodb.morphia.query;


import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return collect(CriteriaJoin.AND, criteria);
    }

    @Override
    public FieldEnd<? extends CriteriaContainer> criteria(final String name) {
        return criteria(name, query.isValidatingNames());
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
    public void addTo(final DBObject obj) {
        if (joinMethod == CriteriaJoin.AND) {
            final Set<String> fields = new HashSet<String>();
            int nonNullFieldNames = 0;
            for (final Criteria child : children) {
                if (null != child.getFieldName()) {
                    fields.add(child.getFieldName());
                    nonNullFieldNames++;
                }
            }
            if (fields.size() < nonNullFieldNames) {
                //use $and
                final BasicDBList and = new BasicDBList();

                for (final Criteria child : children) {
                    final BasicDBObject container = new BasicDBObject();
                    child.addTo(container);
                    and.add(container);
                }

                obj.put("$and", and);
            } else {
                //no dup field names, don't use $and
                for (final Criteria child : children) {
                    child.addTo(obj);
                }
            }
        } else if (joinMethod == CriteriaJoin.OR) {
            final BasicDBList or = new BasicDBList();

            for (final Criteria child : children) {
                final BasicDBObject container = new BasicDBObject();
                child.addTo(container);
                or.add(container);
            }

            obj.put("$or", or);
        }
    }

    @Override
    public String getFieldName() {
        return joinMethod.toString();
    }

    /**
     * @return the Criteria in this CriteriaContainer
     */
    public List<Criteria> getChildren() {
        return children;
    }

    /**
     * Sets the Criteria in this CriteriaContainer
     *
     * @param children the Criteria
     */

    public void setChildren(final List<Criteria> children) {
        this.children = children;
    }

    /**
     * @return the join method of this CriteriaContainer
     * @see CriteriaJoin
     */
    public CriteriaJoin getJoinMethod() {
        return joinMethod;
    }

    /**
     * Sets the join method of this CriteriaContainer
     *
     * @param joinMethod the CriteriaJoin to use
     * @see CriteriaJoin
     */

    public void setJoinMethod(final CriteriaJoin joinMethod) {
        this.joinMethod = joinMethod;
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

    private FieldEnd<? extends CriteriaContainer> criteria(final String field, final boolean validateName) {
        return new FieldEndImpl<CriteriaContainerImpl>(query, field, this, validateName);
    }
}
