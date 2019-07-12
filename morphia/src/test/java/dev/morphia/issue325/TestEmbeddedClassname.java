package dev.morphia.issue325;


import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.Transient;
import dev.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.List;


public class TestEmbeddedClassname extends TestBase {

    @Test
    public final void testEmbeddedClassname() {
        Datastore ds = getDs();

        Root r = new Root();
        r.singleA = new A();
        ds.save(r);

        ds.find(Root.class).update().addToSet("aList", new A()).execute();
        r = ds.find(Root.class).filter("_id", "id").first();
        Document aRaw = r.singleA.raw;

        // Test that singleA does not contain the class name
        final String discriminatorField = getMapper().getOptions().getDiscriminatorField();
        Assert.assertFalse(aRaw.containsKey(discriminatorField));

        // Test that aList does not contain the class name
        aRaw = r.aList.get(0).raw;
        Assert.assertFalse(aRaw.containsKey(discriminatorField));

        // Test that bList does not contain the class name of the subclass
        ds.find(Root.class).update().addToSet("bList", new B()).execute();
        r = ds.find(Root.class).filter("_id", "id").first();

        aRaw = r.aList.get(0).raw;
        Assert.assertFalse(aRaw.containsKey(discriminatorField));

        Document bRaw = r.bList.get(0).getRaw();
        Assert.assertFalse(bRaw.containsKey(discriminatorField));

        ds.delete(ds.find(Root.class));

        //test saving an B in aList, and it should have the classname.
        Root entity = new Root();
        entity.singleA = new B();
        ds.save(entity);
        ds.find(Root.class).update().addToSet("aList", new B()).execute();
        r = ds.find(Root.class).filter("_id", "id").first();

        // test that singleA.raw *does* contain the classname because we stored a subclass there
        aRaw = r.singleA.raw;
        Assert.assertTrue(aRaw.containsKey(discriminatorField));
        Document bRaw2 = r.aList.get(0).raw;
        Assert.assertTrue(bRaw2.containsKey(discriminatorField));
    }

    @Entity(useDiscriminator = false)
    private static class Root {
        private final List<A> aList = new ArrayList<>();
        private final List<B> bList = new ArrayList<>();
        @Id
        private String id = "id";
        private A singleA;
    }

    @Embedded
    private static class A {
        private String name = "some name";

        @Transient
        private Document raw;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Document getRaw() {
            return raw;
        }

        public void setRaw(final Document raw) {
            this.raw = raw;
        }

        @PreLoad
        void preLoad(final Document document) {
            raw = document;
        }
    }

    private static class B extends A {
        private String description = "<description here>";
    }

}
