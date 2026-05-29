package dev.morphia.test.mapping;

import java.util.List;
import java.util.Set;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSingleToMultipleConversion extends TestBase {
    @Test
    public void testBasicType() {
        getDs().find(HasSingleString.class)
                .delete();
        getDs().save(new HasSingleString());
        Assertions.assertNotNull(getDs().find(HasSingleString.class).iterator()
                .next());
        Assertions.assertEquals(1, getDs().find(HasSingleString.class).count());
        final HasManyStringsArray hms = getDs().find(HasManyStringsArray.class).iterator()
                .next();
        Assertions.assertNotNull(hms);
        Assertions.assertNotNull(hms.strings);
        Assertions.assertEquals(1, hms.strings.length);

        final HasManyStringsList hms2 = getDs().find(HasManyStringsList.class).iterator()
                .next();
        Assertions.assertNotNull(hms2);
        Assertions.assertNotNull(hms2.strings);
        Assertions.assertEquals(1, hms2.strings.size());
    }

    @Test
    public void testEmbeddedType() {
        getDs().save(new HasEmbeddedStringy());
        Assertions.assertNotNull(getDs().find(HasEmbeddedStringy.class).iterator()
                .next());
        Assertions.assertEquals(1, getDs().find(HasEmbeddedStringy.class).count());
        final HasEmbeddedStringyArray has = getDs().find(HasEmbeddedStringyArray.class).first();
        Assertions.assertNotNull(has);
        Assertions.assertNotNull(has.hss);
        Assertions.assertEquals(1, has.hss.length);

        final HasEmbeddedStringySet has2 = getDs().find(HasEmbeddedStringySet.class).first();
        Assertions.assertNotNull(has2);
        Assertions.assertNotNull(has2.hss);
        Assertions.assertEquals(1, has2.hss.size());
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
