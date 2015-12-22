package org.mongodb.morphia;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.AlsoLoad;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.List;
import java.util.Set;


public class TestSingleToMultipleConversion extends TestBase {
    @Test
    public void testBasicType() throws Exception {
        getDs().delete(getDs().createQuery(HasSingleString.class));
        getDs().save(new HasSingleString());
        Assert.assertNotNull(getDs().find(HasSingleString.class).get());
        Assert.assertEquals(1, getDs().find(HasSingleString.class).countAll());
        final HasManyStringsArray hms = getDs().find(HasManyStringsArray.class).get();
        Assert.assertNotNull(hms);
        Assert.assertNotNull(hms.strings);
        Assert.assertEquals(1, hms.strings.length);

        final HasManyStringsList hms2 = getDs().find(HasManyStringsList.class).get();
        Assert.assertNotNull(hms2);
        Assert.assertNotNull(hms2.strings);
        Assert.assertEquals(1, hms2.strings.size());
    }

    @Test
    public void testEmbeddedType() throws Exception {
        getDs().save(new HasEmbeddedStringy());
        Assert.assertNotNull(getDs().find(HasEmbeddedStringy.class).get());
        Assert.assertEquals(1, getDs().find(HasEmbeddedStringy.class).countAll());
        final HasEmbeddedStringyArray has = getDs().find(HasEmbeddedStringyArray.class).get();
        Assert.assertNotNull(has);
        Assert.assertNotNull(has.hss);
        Assert.assertEquals(1, has.hss.length);

        final HasEmbeddedStringySet has2 = getDs().find(HasEmbeddedStringySet.class).get();
        Assert.assertNotNull(has2);
        Assert.assertNotNull(has2.hss);
        Assert.assertEquals(1, has2.hss.size());
    }

    @Embedded
    private static class HasString {
        private String s = "foo";
    }

    @Entity(value = "B", noClassnameStored = true)
    private static class HasEmbeddedStringy {
        @Id
        private ObjectId id;
        private HasString hs = new HasString();
    }

    @Entity(value = "B", noClassnameStored = true)
    private static class HasEmbeddedStringyArray {
        @Id
        private ObjectId id;
        @AlsoLoad("hs")
        private HasString[] hss;
    }

    @Entity(value = "B", noClassnameStored = true)
    private static class HasEmbeddedStringySet {
        @Id
        private ObjectId id;
        @AlsoLoad("hs")
        private Set<HasString> hss;
    }

    @Entity(value = "A", noClassnameStored = true)
    private static class HasSingleString {
        @Id
        private ObjectId id;
        private String s = "foo";
    }

    @Entity(value = "A", noClassnameStored = true)
    private static class HasManyStringsArray {
        @Id
        private ObjectId id;
        @AlsoLoad("s")
        private String[] strings;
    }

    @Entity(value = "A", noClassnameStored = true)
    private static class HasManyStringsList {
        @Id
        private ObjectId id;
        @AlsoLoad("s")
        private List<String> strings;
    }
}
