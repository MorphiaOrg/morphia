package dev.morphia.callbacks;


import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostPersist;
import dev.morphia.callbacks.TestPostPersist.NestedEventEntity.Inner;
import dev.morphia.mapping.codec.pojo.ClassMethodPair;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;


public class TestPostPersist extends TestBase {

    @Test
    public void testBulkLifecycleEvents() {
        TestObject to1 = new TestObject("post value 1");
        TestObject to2 = new TestObject("post value 2");
        getDs().insert(asList(to1, to2));

        assertNotNull(to1.id);
        assertNotNull(to1.one);
        assertNotNull(to2.id);
        assertNotNull(to2.one);
    }

    @Test
    public void testCallback() {
        getMapper().map(List.of(NestedEventEntity.class, Inner.class));
        MorphiaCodec<NestedEventEntity> codec = (MorphiaCodec<NestedEventEntity>) getMapper().getCodecRegistry()
                                                                                             .get(NestedEventEntity.class);
        Map<Class<? extends Annotation>, List<ClassMethodPair>> lifecycleMethods = codec.getEntityModel().getLifecycleMethods();
        final NestedEventEntity p = new NestedEventEntity();
        getDs().save(p);
        Assert.assertTrue(p.called);
        Assert.assertTrue(p.i.innerCalled);
    }

    @Entity
    public static class NestedEventEntity {
        @Id
        private ObjectId id;
        private final Inner i = new Inner();
        private boolean called;

        @Embedded
        static class Inner {
            private boolean innerCalled;

            @PostPersist
            void m2() {
                innerCalled = true;
            }
        }

        @PostPersist
        void m1() {
            called = true;
        }
    }

    @Entity
    public static class TestObject {
        @Id
        private ObjectId id;
        private String value;
        private String one;

        private TestObject() {
        }

        public TestObject(String value) {
            this.value = value;
        }

        @PostPersist
        public void doIt() {
            if (one != null) {
                throw new RuntimeException("@PostPersist methods should only be called once");
            }
            one = value;
        }
    }
}
