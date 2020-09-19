package dev.morphia.query;


import dev.morphia.mapping.MappedClass;
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
 * @morphia.internal
 * @see CriteriaJoin
 */
@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
public class CriteriaContainerImpl extends AbstractCriteria implements CriteriaContainer {
    private final Mapper mapper;
    private final MappedClass mappedClass;
    private final CriteriaJoin joinMethod;
    private final List<Criteria> children = new ArrayList<>();
    private LegacyQuery<?> query;

    protected CriteriaContainerImpl(Mapper mapper, LegacyQuery<?> query, CriteriaJoin joinMethod) {
        this.joinMethod = joinMethod;
        this.mapper = mapper;
        this.query = query;
        mappedClass = mapper.getMappedClass(query.getEntityClass());
    }

    /**
     * @return the join method used
     * @see CriteriaJoin
     */
    public CriteriaJoin getJoinMethod() {
        return joinMethod;
    }

    /**
     * @return the children of this container
     */
    public List<Criteria> getChildren() {
        return children;
    }

    @Override
    public void add(Criteria... criteria) {
        for (Criteria c : criteria) {
            c.attach(this);
            children.add(c);
        }
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
        Document document = new Document();
        final List<Document> and = new ArrayList<>();
        Set<String> names = new HashSet<>();
        boolean duplicates = false;

        for (Criteria child : children) {
            final Document childObject = child.toDocument();
            for (String s : childObject.keySet()) {
                duplicates |= !names.add(s);
            }
            and.add(childObject);
        }

        if (!duplicates) {
            for (Object o : and) {
                document.putAll((Map) o);
            }
        } else {
            document.put("$and", and);
        }

        return document;
    }

    private Document or() {
        Document document = new Document();
        final List<Document> or = new ArrayList<>();

        for (Criteria child : children) {
            or.add(child.toDocument());
        }

        document.put("$or", or);

        return document;
    }

    @Override
    public String getFieldName() {
        return joinMethod.toString();
    }

    @Override
    public CriteriaContainer and(Criteria... criteria) {
        return collect(AND, criteria);
    }

    @Override
    public FieldEnd<? extends CriteriaContainer> criteria(String name) {
        return new FieldEndImpl<>(mapper, name, this, mappedClass, query.isValidatingNames());
    }

    /**
     * @return the Query for this CriteriaContainer
     */
    public LegacyQuery<?> getQuery() {
        return query;
    }

    /**
     * Sets the Query for this CriteriaContainer
     *
     * @param query the query
     */
    public void setQuery(LegacyQuery<?> query) {
        this.query = query;
    }

    @Override
    public String toString() {
        return children.toString();
    }

    @Override
    public CriteriaContainer or(Criteria... criteria) {
        return collect(CriteriaJoin.OR, criteria);
    }

    @Override
    public void remove(Criteria criteria) {
        children.remove(criteria);
    }

    private CriteriaContainer collect(CriteriaJoin cj, Criteria... criteria) {
        final CriteriaContainerImpl parent = new CriteriaContainerImpl(mapper, query, cj);

        for (Criteria c : criteria) {
            parent.add(c);
        }

        add(parent);

        return parent;
    }
}
