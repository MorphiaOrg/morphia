package org.mongodb.morphia.query;


import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.TestMapping.BaseEntity;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.utils.Assert;

import java.util.Collections;


public class TestQueryParams extends TestBase {
    private FieldEnd<?> e;

    @Before
    public void createQ() {
        e = getDs().createQuery(E.class).field("_id");
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testAnyOfNull() {
        e.hasAnyOf(null);
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testGreaterThanNull() {
        e.greaterThan(null);
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testGreaterThanOrEqualNull() {
        e.greaterThanOrEq(null);
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testHasAllOfEmptyList() {
        final Query<E> q = getDs().createQuery(E.class);
        q.field("_id").hasAllOf(Collections.emptyList());
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testHasAllOfNull() {
        e.hasAllOf(null);
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testHasNoneOfNull() {
        e.hasNoneOf(null);
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testHasThisNullElement() {
        e.hasThisElement(null);
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testLessThanNull() {
        e.lessThan(null);
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testLessThanOrEqualNull() {
        e.lessThanOrEq(null);
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testNoneOfEmptyList() {
        final Query<E> q = getDs().createQuery(E.class);
        q.field("_id").hasNoneOf(Collections.emptyList());
    }

    @Test
    public void testNullAcceptance() {

        // have to succeed:
        e.equal(null);
        e.notEqual(null);
        e.hasThisOne(null);
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testStartsWithIgnoreCaseNull() {
        e.startsWithIgnoreCase(null);
    }

    @Test(expected = Assert.AssertionFailedException.class)
    public void testStartsWithNull() {
        e.startsWith(null);
    }

    @Entity
    static class E extends BaseEntity {

    }
}
