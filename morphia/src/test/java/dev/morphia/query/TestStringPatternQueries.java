package dev.morphia.query;


import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.TestMapping.BaseEntity;
import dev.morphia.annotations.Entity;

import static java.util.Arrays.asList;


public class TestStringPatternQueries extends TestBase {
    @Test
    public void testContains() throws Exception {

        getDs().save(asList(new E("xBA"), new E("xa"), new E("xAb"), new E("xab"), new E("xcB"), new E("aba")));

        Assert.assertEquals(3, getDs().find(E.class).field("name").contains("b").count());
        Assert.assertEquals(5, getDs().find(E.class).field("name").containsIgnoreCase("b").count());
    }

    @Test
    public void testEndsWith() throws Exception {

        getDs().save(asList(new E("bxA"), new E("xba"), new E("xAb"), new E("xab"), new E("xcB"), new E("aba")));

        Assert.assertEquals(2, getDs().find(E.class).field("name").endsWith("b").count());
        Assert.assertEquals(3, getDs().find(E.class).field("name").endsWithIgnoreCase("b").count());
    }

    @Test
    public void testStartsWith() throws Exception {

        getDs().save(asList(new E("A"), new E("a"), new E("Ab"), new E("ab"), new E("c")));

        Assert.assertEquals(2, getDs().find(E.class).field("name").startsWith("a").count());
        Assert.assertEquals(4, getDs().find(E.class).field("name").startsWithIgnoreCase("a").count());
        Assert.assertEquals(4, getDs().find(E.class).field("name").startsWithIgnoreCase("A").count());
    }

    @Entity
    static class E extends BaseEntity {
        private final String name;

        E(final String name) {
            this.name = name;
        }

        protected E() {
            name = null;
        }
    }

}
