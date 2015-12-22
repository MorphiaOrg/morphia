package org.mongodb.morphia.query;


import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.TestMapping.BaseEntity;
import org.mongodb.morphia.annotations.Entity;


public class TestStringPatternQueries extends TestBase {
    @Test
    public void testContains() throws Exception {

        getDs().save(new E("xBA"), new E("xa"), new E("xAb"), new E("xab"), new E("xcB"), new E("aba"));

        Assert.assertEquals(3, getDs().createQuery(E.class).field("name").contains("b").countAll());
        Assert.assertEquals(5, getDs().createQuery(E.class).field("name").containsIgnoreCase("b").countAll());
    }

    @Test
    public void testEndsWith() throws Exception {

        getDs().save(new E("bxA"), new E("xba"), new E("xAb"), new E("xab"), new E("xcB"), new E("aba"));

        Assert.assertEquals(2, getDs().createQuery(E.class).field("name").endsWith("b").countAll());
        Assert.assertEquals(3, getDs().createQuery(E.class).field("name").endsWithIgnoreCase("b").countAll());
    }

    @Test
    public void testStartsWith() throws Exception {

        getDs().save(new E("A"), new E("a"), new E("Ab"), new E("ab"), new E("c"));

        Assert.assertEquals(2, getDs().createQuery(E.class).field("name").startsWith("a").countAll());
        Assert.assertEquals(4, getDs().createQuery(E.class).field("name").startsWithIgnoreCase("a").countAll());
        Assert.assertEquals(4, getDs().createQuery(E.class).field("name").startsWithIgnoreCase("A").countAll());
    }

    @Entity
    static class E extends BaseEntity {
        private final String name;

        public E(final String name) {
            this.name = name;
        }

        protected E() {
            name = null;
        }
    }

}
