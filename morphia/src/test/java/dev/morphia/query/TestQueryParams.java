package dev.morphia.query;


import dev.morphia.mapping.DefaultCreator;
import dev.morphia.mapping.InstanceCreatorFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.TestMapping.BaseEntity;
import dev.morphia.annotations.Entity;
import dev.morphia.utils.Assert;

import java.util.Collections;


public class TestQueryParams extends TestBase {
    private FieldEnd<?> e;

    @Before
    public void createQ() {
        e = getDs().find(E.class).field("_id");
    }

    @Test(expected = QueryException.class)
    public void testAnyOfNull() {
        e.hasAnyOf(null);
    }

    @Test(expected = QueryException.class)
    public void testGreaterThanNull() {
        e.greaterThan(null);
    }

    @Test(expected = QueryException.class)
    public void testGreaterThanOrEqualNull() {
        e.greaterThanOrEq(null);
    }

    @Test(expected = QueryException.class)
    public void testHasAllOfEmptyList() {
        final Query<E> q = getDs().find(E.class);
        q.field("_id").hasAllOf(Collections.emptyList());
    }

    @Test(expected = QueryException.class)
    public void testHasAllOfNull() {
        e.hasAllOf(null);
    }

    @Test(expected = QueryException.class)
    public void testHasNoneOfNull() {
        e.hasNoneOf(null);
    }

    @Test(expected = QueryException.class)
    public void testLessThanNull() {
        e.lessThan(null);
    }

    @Test(expected = QueryException.class)
    public void testLessThanOrEqualNull() {
        e.lessThanOrEq(null);
    }

    @Test(expected = QueryException.class)
    public void testNoneOfEmptyList() {
        final Query<E> q = getDs().find(E.class);
        q.field("_id").hasNoneOf(Collections.emptyList());
    }

    @Test
    public void testNullAcceptance() {

//        new DefaultCreator().createInstance(BaseEntity.class);
//        final InstanceCreatorFactoryImpl creatorFactory = new InstanceCreatorFactoryImpl(getMapper().getDatastore(), BaseEntity.class);

        // have to succeed:
        e.equal(null);
        e.notEqual(null);
        e.hasThisOne(null);
    }

    @Test(expected = QueryException.class)
    public void testStartsWithIgnoreCaseNull() {
        e.startsWithIgnoreCase(null);
    }

    @Test(expected = QueryException.class)
    public void testStartsWithNull() {
        e.startsWith(null);
    }

    @Entity
    static class E extends BaseEntity {

    }
}
