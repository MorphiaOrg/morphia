package org.mongodb.morphia.issue325;


import com.mongodb.DBObject;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.List;


public class TestEmbeddedClassname extends TestBase {

    @Entity(noClassnameStored = true)
    private static class Root {
        @Id
        private String id = "a";

        @Embedded
        private final List<A> as = new ArrayList<A>();

        @Embedded
        private final List<B> bs = new ArrayList<B>();
    }

    private static class A {
        private String name = "undefined";

        @Transient
        private DBObject raw;

        @PreLoad
        void preLoad(final DBObject dbObj) {
            raw = dbObj;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public DBObject getRaw() {
            return raw;
        }

        public void setRaw(final DBObject raw) {
            this.raw = raw;
        }
    }

    private static class B extends A {
        private String description = "<description here>";
    }

    @Test
    public final void testEmbeddedClassname() {
        Root r = new Root();
        getDs().save(r);

        final A a = new A();
        getDs().update(getDs().createQuery(Root.class), getDs().createUpdateOperations(Root.class).add("as", a));
        r = getDs().get(Root.class, "a");
        Assert.assertFalse(r.as.get(0).raw.containsField(Mapper.CLASS_NAME_FIELDNAME));

        B b = new B();
        getDs().update(getDs().createQuery(Root.class), getDs().createUpdateOperations(Root.class).add("bs", b));
        r = getDs().get(Root.class, "a");
        Assert.assertFalse(r.bs.get(0).getRaw().containsField(Mapper.CLASS_NAME_FIELDNAME));

        getDs().delete(getDs().createQuery(Root.class));
        //test saving an B in as, and it should have the classname.

        getDs().save(new Root());
        b = new B();
        getDs().update(getDs().createQuery(Root.class), getDs().createUpdateOperations(Root.class).add("as", b));
        r = getDs().get(Root.class, "a");
        Assert.assertTrue(r.as.get(0).raw.containsField(Mapper.CLASS_NAME_FIELDNAME));

    }

}
