package dev.morphia.test.mapping;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;


public class TestSingleToMultipleConversion extends TestBase {
    @Test
    public void testBasicType() {
        getDs().find(HasSingleString.class)
               .delete();
        getDs().save(new HasSingleString());
        Assert.assertNotNull(getDs().find(HasSingleString.class).iterator(new FindOptions().limit(1))
                                    .next());
        Assert.assertEquals(getDs().find(HasSingleString.class).count(), 1);
        final HasManyStringsArray hms = getDs().find(HasManyStringsArray.class).iterator(new FindOptions().limit(1))
                                               .next();
        Assert.assertNotNull(hms);
        Assert.assertNotNull(hms.strings);
        Assert.assertEquals(hms.strings.length, 1);

        final HasManyStringsList hms2 = getDs().find(HasManyStringsList.class).iterator(new FindOptions().limit(1))
                                               .next();
        Assert.assertNotNull(hms2);
        Assert.assertNotNull(hms2.strings);
        Assert.assertEquals(hms2.strings.size(), 1);
    }

    @Test
    public void testEmbeddedType() {
        getDs().save(new HasEmbeddedStringy());
        Assert.assertNotNull(getDs().find(HasEmbeddedStringy.class).iterator(new FindOptions().limit(1))
                                    .next());
        Assert.assertEquals(getDs().find(HasEmbeddedStringy.class).count(), 1);
        final HasEmbeddedStringyArray has = getDs().find(HasEmbeddedStringyArray.class).first();
        Assert.assertNotNull(has);
        Assert.assertNotNull(has.hss);
        Assert.assertEquals(has.hss.length, 1);

        final HasEmbeddedStringySet has2 = getDs().find(HasEmbeddedStringySet.class).first();
        Assert.assertNotNull(has2);
        Assert.assertNotNull(has2.hss);
        Assert.assertEquals(has2.hss.size(), 1);
    }

    @Entity(value = "B", useDiscriminator = false)
    private static class HasEmbeddedStringy {
        private final HasString hs = new HasString();
        @Id
        private ObjectId id;
    }

    @Entity(value = "B", useDiscriminator = false)
    private static class HasEmbeddedStringyArray {
        @Id
        private ObjectId id;
        @AlsoLoad("hs")
        private HasString[] hss;
    }

    @Entity(value = "B", useDiscriminator = false)
    private static class HasEmbeddedStringySet {
        @Id
        private ObjectId id;
        @AlsoLoad("hs")
        private Set<HasString> hss;
    }

    @Entity(value = "A", useDiscriminator = false)
    private static class HasManyStringsArray {
        @Id
        private ObjectId id;
        @AlsoLoad("s")
        private String[] strings;
    }

    @Entity(value = "A", useDiscriminator = false)
    private static class HasManyStringsList {
        @Id
        private ObjectId id;
        @AlsoLoad("s")
        private List<String> strings;
    }

    @Entity(value = "A", useDiscriminator = false)
    private static class HasSingleString {
        private final String s = "foo";
        @Id
        private ObjectId id;
    }

    @Entity
    private static class HasString {
        private final String s = "foo";
    }
}
