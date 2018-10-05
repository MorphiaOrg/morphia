package xyz.morphia;


import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import xyz.morphia.annotations.Embedded;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Id;
import xyz.morphia.annotations.PostLoad;
import xyz.morphia.annotations.Property;
import xyz.morphia.annotations.Reference;
import xyz.morphia.mapping.EmbeddedMappingTest.AnotherNested;
import xyz.morphia.mapping.EmbeddedMappingTest.Nested;
import xyz.morphia.mapping.EmbeddedMappingTest.NestedImpl;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.Mapper;
import xyz.morphia.mapping.cache.EntityCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.morphia.mapping.lazy.LazyFeatureDependencies;

import java.io.Serializable;
import java.util.List;

import static java.util.Arrays.asList;


/**
 * Tests mapper functions; this is tied to some of the internals.
 *
 * @author scotthernandez
 */
public class TestMapper extends TestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestMapper.class);

    @Test
    public void serializableId() throws Exception {
        final CustomId cId = new CustomId();
        cId.id = new ObjectId();
        cId.type = "banker";
        final UsesCustomIdObject object = new UsesCustomIdObject();
        object.id = cId;
        object.text = "hllo";
        getDs().save(object);
    }

    @Test
    public void singleLookup() throws Exception {
        A.loadCount = 0;
        final A a = new A();
        HoldsMultipleA holder = new HoldsMultipleA();
        holder.a1 = a;
        holder.a2 = a;
        getDs().save(asList(a, holder));
        holder = getDs().get(HoldsMultipleA.class, holder.id);
        Assert.assertEquals(1, A.loadCount);
        Assert.assertTrue(holder.a1 == holder.a2);
    }

    @Test
    public void singleProxy() throws Exception {
        // TODO us: exclusion does not work properly with maven + junit4
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        A.loadCount = 0;
        final A a = new A();
        HoldsMultipleALazily holder = new HoldsMultipleALazily();
        holder.a1 = a;
        holder.a2 = a;
        holder.a3 = a;
        getDs().save(asList(a, holder));
        Assert.assertEquals(0, A.loadCount);
        holder = getDs().get(HoldsMultipleALazily.class, holder.id);
        Assert.assertNotNull(holder.a2);
        Assert.assertEquals(1, A.loadCount);
        Assert.assertFalse(holder.a1 == holder.a2);
        // FIXME currently not guaranteed:
        // Assert.assertTrue(holder.a1 == holder.a3);

        // A.loadCount=0;
        // Assert.assertEquals(holder.a1.getId(), holder.a2.getId());

    }

    @Test
    public void subTypes() {
        getMorphia().map(NestedImpl.class, AnotherNested.class);

        Mapper mapper = getMorphia().getMapper();
        List<MappedClass> subTypes = mapper.getSubTypes(mapper.getMappedClass(Nested.class));
        Assert.assertTrue(subTypes.contains(mapper.getMappedClass(NestedImpl.class)));
        Assert.assertTrue(subTypes.contains(mapper.getMappedClass(AnotherNested.class)));
    }

    public static class A {
        private static int loadCount;
        @Id
        private ObjectId id;

        @PostLoad
        protected void postConstruct() {
            if (loadCount > 1) {
                throw new RuntimeException();
            }

            loadCount++;
        }

        String getId() {
            return id.toString();
        }
    }

    @Entity("holders")
    public static class HoldsMultipleA {
        @Id
        private ObjectId id;
        @Reference
        private A a1;
        @Reference
        private A a2;
    }

    @Entity("holders")
    public static class HoldsMultipleALazily {
        @Id
        private ObjectId id;
        @Reference(lazy = true)
        private A a1;
        @Reference
        private A a2;
        @Reference(lazy = true)
        private A a3;
    }

    public static class CustomId implements Serializable {

        @Property("v")
        private ObjectId id;
        @Property("t")
        private String type;

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof CustomId)) {
                return false;
            }
            final CustomId other = (CustomId) obj;
            if (id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!id.equals(other.id)) {
                return false;
            }
            if (type == null) {
                if (other.type != null) {
                    return false;
                }
            } else if (!type.equals(other.type)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("CustomId [");
            if (id != null) {
                builder.append("id=").append(id).append(", ");
            }
            if (type != null) {
                builder.append("type=").append(type);
            }
            builder.append("]");
            return builder.toString();
        }
    }

    public static class UsesCustomIdObject {
        @Id
        private CustomId id;
        private String text;

        public CustomId getId() {
            return id;
        }

        public void setId(final CustomId id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(final String text) {
            this.text = text;
        }
    }

    @Test
    @Ignore
    public void testMapperPerformance() {
        Morphia morphia = getMorphia();
        Mapper mapper = morphia.getMapper();
        mapper.addMappedClass(Container.class);
        Datastore ds = morphia.createDatastore(getMongoClient(), "testDB");

        int listSize = 100;
        int iterations = 100000;

        // create the DbObject
        BasicDBList aus = new BasicDBList();
        for (int i = 0; i < listSize; i++) {
            BasicDBObject au = new BasicDBObject("id", i);
            au.put("name", "john doe");
            aus.add(au);
        }
        BasicDBObject p = new BasicDBObject("values", aus);

        EntityCache entityCache = mapper.getOptions().getCacheFactory().createCache();

        long total = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            morphia.fromDBObject(ds, Container.class, p, entityCache);
            long stop = System.nanoTime();

            long delta = stop - start;
            total = total + delta;
            if (i % 10000 == 0) {
                LOGGER.warn("Mapping " + i + " took " + delta + " ns");
            }
        }

        double totalMs = ((double) total) / 1000000;
        LOGGER.warn("Morphia creation took total " + (totalMs) + " ms, avg: " + (totalMs / iterations));
    }

    private static class Customer {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    private static class Container {
        @Embedded
        private List<Customer> values;
        @Id
        private ObjectId id;

        public List<Customer> getValues() {
            return values;
        }

        public void setValues(final List<Customer> values) {
            this.values = values;
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }
    }



}
