package dev.morphia.test.lifecycle;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.DatastoreImpl;
import dev.morphia.EntityInterceptor;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Transient;
import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@SuppressWarnings({ "resource", "DataFlowIssue", "unused", "removal" })
public class TestLifecycles extends TestBase {
    @Test
    public void ensureDateConfigurationIsAppliedEverywhere() {
        withConfig(buildConfig().dateStorage(DateStorage.SYSTEM_DEFAULT), () -> {
            getDs().getMapper().map(SimpleBean.class);
            SimpleBean simpleBean = new SimpleBean();
            simpleBean.id = ObjectId.get();
            simpleBean.ldt = LocalDateTime.of(2021, Month.APRIL, 15, 18, 0, 0, 0);

            Calendar instance = Calendar.getInstance();
            instance.set(2021, Calendar.APRIL, 15, 18, 0, 0);
            simpleBean.date = instance.getTime();

            getDs().save(simpleBean);

            SimpleBean reloaded = getDs().find(SimpleBean.class).first();

            Assert.assertEquals(reloaded.ldt.getHour(), simpleBean.ldt.getHour(), format("'%s' vs '%s'", reloaded.ldt, simpleBean.ldt));
            Assert.assertEquals(reloaded.date, simpleBean.date, format("'%s' vs '%s'", reloaded.date, simpleBean.date));
        });

    }

    @Test
    public void classMethodPairings() {
        List<EntityModel> map = getMapper().map(Parent.class, Child.class);
        var parent = map.get(0);
        var child = map.get(1);

        assertTrue(parent.hasLifecycle(PrePersist.class));
        assertFalse(child.hasLifecycle(PrePersist.class));
    }

    @Test
    public void testCallbackMethods() {
        LifecyleA a = new LifecyleA();
        a.b = new LifecycleB();
        a.bs.add(new LifecycleB());

        Assert.assertFalse(a.isPostLoad());
        Assert.assertFalse(a.b.isPostLoad());
        Assert.assertFalse(a.bs.get(0).isPostLoad());

        Assert.assertFalse(a.isPostPersist());
        Assert.assertFalse(a.b.isPostPersist());
        Assert.assertFalse(a.bs.get(0).isPostPersist());

        Assert.assertFalse(a.isPreLoad());
        Assert.assertFalse(a.b.isPreLoad());
        Assert.assertFalse(a.bs.get(0).isPreLoad());

        Assert.assertFalse(a.isPrePersist());
        Assert.assertFalse(a.b.isPrePersist());
        Assert.assertFalse(a.bs.get(0).isPrePersist());

        getDs().save(a);

        Assert.assertFalse(a.isPreLoad());
        Assert.assertFalse(a.b.isPreLoad());
        Assert.assertFalse(a.bs.get(0).isPreLoad());

        Assert.assertTrue(a.isPostPersist());
        Assert.assertTrue(a.b.isPostPersist()); //PostPersist in not only called on entities
        Assert.assertTrue(a.bs.get(0).isPostPersist()); //PostPersist is not only called on entities

        Assert.assertFalse(a.isPreLoad());
        Assert.assertFalse(a.b.isPreLoad());
        Assert.assertFalse(a.bs.get(0).isPreLoad());

        Assert.assertTrue(a.isPrePersist());
        Assert.assertTrue(a.b.isPrePersist());
        Assert.assertTrue(a.bs.get(0).isPrePersist());

        a = getDs().find(LifecyleA.class).filter(eq("_id", a.id)).first();

        Assert.assertTrue(a.isPostLoad());
        Assert.assertTrue(a.b.isPostLoad());
        Assert.assertTrue(a.bs.get(0).isPostLoad());

        Assert.assertTrue(a.isPreLoad());
        Assert.assertTrue(a.b.isPreLoad());
        Assert.assertTrue(a.bs.get(0).isPreLoad());
    }

    @Test
    public void testGlobalInterceptorRunsAfterEntityCallback() {
        getMapper().addInterceptor(new NonNullValidation());
        getMapper().map(ValidNullHolder.class);
        getMapper().map(InvalidNullHolder.class);

        getDs().save(new ValidNullHolder());
        try {
            getDs().save(new InvalidNullHolder());
            Assert.fail();
        } catch (NonNullValidationException e) {
            // expected
        }

    }

    @Test
    public void testMultipleCallbackAnnotation() {
        final SomeEntity entity = new SomeEntity();
        Assert.assertFalse(entity.isPersistent());
        getDs().save(entity);
        Assert.assertTrue(entity.isPersistent());
        final SomeEntity reloaded = getDs().find(SomeEntity.class).filter(eq("id", entity.getId())).first();
        Assert.assertTrue(reloaded.isPersistent());
    }

    @Test
    public void testWithGeoJson() {
        final Polygon polygon = new Polygon(
                asList(new Position(0d, 0d), new Position(1d, 1d), new Position(2d, 2d), new Position(3d, 3d), new Position(0d, 0d)));
        getDs().save(new HoldsPolygon(ObjectId.get(), polygon));

        Assert.assertFalse(HoldsPolygon.lifecycle);
        Assert.assertNotNull(getDs().find(HoldsPolygon.class).first());
        Assert.assertTrue(HoldsPolygon.lifecycle);
    }

    private static class Callbacks {
        @Transient
        private boolean prePersist;
        @Transient
        private boolean postPersist;
        @Transient
        private boolean preLoad;
        @Transient
        private boolean postLoad;

        boolean isPostLoad() {
            return postLoad;
        }

        boolean isPostPersist() {
            return postPersist;
        }

        boolean isPreLoad() {
            return preLoad;
        }

        boolean isPrePersist() {
            return prePersist;
        }

        @PostLoad
        void postLoad() {
            postLoad = true;
        }

        @PostPersist
        void postPersist() {
            postPersist = true;
        }

        @PreLoad
        void preLoad() {
            preLoad = true;
        }

        @PrePersist
        void prePersist() {
            prePersist = true;
        }
    }

    @SuppressWarnings("unused")
    @Entity(value = "polygon", useDiscriminator = false)
    private static class HoldsPolygon {
        private static boolean lifecycle = false;
        @Id
        private ObjectId id;
        private Polygon polygon;

        protected HoldsPolygon() {
        }

        public HoldsPolygon(ObjectId id, Polygon polygon) {
            this.id = id;
            this.polygon = polygon;
        }

        public ObjectId getId() {
            return id;
        }

        public Polygon getPolygon() {
            return polygon;
        }

        @PostLoad
        void somePostLoadMethod() {
            lifecycle = true;
        }
    }

    @Entity
    @SuppressWarnings({ "NotNullFieldNotInitialized" })
    private static class InvalidNullHolder {
        @Id
        private final ObjectId id = new ObjectId();

        @NonNull
        private String mustFailValidation;
    }

    @Entity
    private static class LifecycleB extends Callbacks {
    }

    @Entity
    private static class LifecyleA extends Callbacks {
        private final List<LifecycleB> bs = new LinkedList<>();
        @Id
        private ObjectId id;
        private LifecycleB b;
    }

    private static class NonNullValidation implements EntityInterceptor {
        @Override
        public boolean hasAnnotation(Class type) {
            return type.equals(PrePersist.class);
        }

        @Override
        public void prePersist(@NotNull Object ent, @NotNull Document document, @NotNull Datastore datastore) {
            DatastoreImpl ds = (DatastoreImpl) datastore;
            final List<PropertyModel> fieldsToTest = ds.getMapper().getEntityModel(ent.getClass())
                    .getProperties(NonNull.class);
            for (PropertyModel mf : fieldsToTest) {
                if (mf.getValue(ent) == null) {
                    throw new NonNullValidationException(mf);
                }
            }
        }

    }

    private static class NonNullValidationException extends RuntimeException {
        NonNullValidationException(PropertyModel model) {
            super("NonNull field is null " + model.getFullName());
        }
    }

    @Entity
    private static class SomeEntity {
        @Id
        private final String id = new ObjectId().toHexString();
        @Transient
        private boolean persistentMarker;

        public String getId() {
            return id;
        }

        public boolean isPersistent() {
            return persistentMarker;
        }

        @PostPersist
        @PostLoad
        void markPersistent() {
            persistentMarker = true;
        }
    }

    @Entity
    @SuppressWarnings("FieldCanBeLocal")
    private static class ValidNullHolder {
        @Id
        private final ObjectId id = new ObjectId();

        @Nullable
        private Date lastModified;

        @PrePersist
        void entityCallback() {
            lastModified = new Date();
        }
    }

    @Entity
    private static class SimpleBean {
        @Id
        private ObjectId id;
        private LocalDateTime ldt;
        private Date date;

        @PostPersist
        void postPersist() {
            // This @PostPersist method ensures DocumentWriter is used during persistance.
            // The method dev.morphia.mapping.codec.DocumentWriter.writeDateTime(long) uses the hardcoded Zone UTC
        }
    }
}
