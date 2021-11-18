package dev.morphia.query;


import dev.morphia.Datastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.EntityModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines a container of Criteria and a join method.
 *
 * @morphia.internal
 * @see CriteriaJoin
 */
@MorphiaInternal
@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
public class CriteriaContainerImpl extends AbstractCriteria implements CriteriaContainer {
    private final Datastore datastore;
    private final EntityModel model;
    private final CriteriaJoin joinMethod;
    private final List<Criteria> children = new ArrayList<>();
    private LegacyQuery<?> query;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    protected CriteriaContainerImpl(Datastore datastore, LegacyQuery<?> query, CriteriaJoin joinMethod) {
        this.joinMethod = joinMethod;
        this.datastore = datastore;
        this.query = query;
        model = datastore.getMapper().getEntityModel(query.getEntityClass());
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
    public CriteriaContainer and(Criteria... criteria) {
        return collect(CriteriaJoin.AND, criteria);
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
    public Document toDocument() {
        if (joinMethod == CriteriaJoin.AND) {
            return and();
        } else {
            return or();
        }
    }

    @Override
    public FieldEnd<? extends CriteriaContainer> criteria(String name) {
        return new FieldEndImpl<>(datastore, name, this, model, query.isValidatingNames());
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
    @SuppressFBWarnings("EI_EXPOSE_REP2")
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
        final CriteriaContainerImpl parent = new CriteriaContainerImpl(datastore, query, cj);

        for (Criteria c : criteria) {
            parent.add(c);
        }

        add(parent);

        return parent;
    }
}
