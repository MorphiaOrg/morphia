package dev.morphia.query;


import dev.morphia.mapping.Mapper;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.morphia.query.CriteriaJoin.AND;

/**
 * Defines a container of Criteria and a join method.
 *
 * @see CriteriaJoin
 */
public class CriteriaContainerImpl extends AbstractCriteria implements CriteriaContainer {
    private CriteriaJoin joinMethod;
    private List<Criteria> children = new ArrayList<>();

    private final Mapper mapper;
    private QueryImpl<?> query;

    protected CriteriaContainerImpl(final Mapper mapper, final QueryImpl<?> query, final CriteriaJoin joinMethod) {
        this.joinMethod = joinMethod;
        this.mapper = mapper;
        this.query = query;
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
        return new FieldEndImpl<>(mapper, query, name, this);
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
    public Document toDocument() {
        if (joinMethod == AND) {
            return and();
        } else {
            return or();
        }
    }


    private Document and() {
        Document dbObject = new Document();
        final List<Document> and = new ArrayList<>();
        Set<String> names = new HashSet<>();
        boolean duplicates = false;

        for (final Criteria child : children) {
            final Document childObject = child.toDocument();
            for (final String s : childObject.keySet()) {
                duplicates |= !names.add(s);
            }
            and.add(childObject);
        }

        if (!duplicates) {
            for (final Object o : and) {
                dbObject.putAll((Map) o);
            }
        } else {
            dbObject.put("$and", and);
        }

        return dbObject;
    }

    private Document or() {
        Document dbObject = new Document();
        final List<Document> or = new ArrayList<>();

        for (final Criteria child : children) {
            or.add(child.toDocument());
        }

        dbObject.put("$or", or);

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
        final CriteriaContainerImpl parent = new CriteriaContainerImpl(mapper, query, cj);

        for (final Criteria c : criteria) {
            parent.add(c);
        }

        add(parent);

        return parent;
    }

    @Override
    public String toString() {
        return children.toString();
    }
}
