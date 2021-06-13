package dev.morphia.test.mapping.codec.pojo;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.Transient;
import dev.morphia.mapping.codec.pojo.ClassMethodPair;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.test.TestBase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * asserts some common behaviour between entities with or without lifecycle
 */
public class DiscriminantLifecycleTest extends TestBase {

    @DataProvider(name = "classes")
    public Object[][] classes() {
        return new Object[][]{
            {BaseLifecycleEntity.class, ChildLifecycleEntity.class},
            {BaseEntity.class, ChildEntity.class}
        };
    }

    @Test(dataProvider = "classes")
    public void testCorrectEntity(Class<?> baseClass, Class<? extends Child> childClass) {
        getMapper().map(baseClass, childClass);
        ObjectId id = saveChildEntity(childClass);
        Child saved = (Child) getDs().find(baseClass).filter(Filters.eq("_id", id)).first();
        Assert.assertTrue(childClass.isInstance(saved));
        Assert.assertTrue(saved.getAudited());
        Assert.assertTrue(saved.getBaseAudited());
        assertEquals(saved.getEmbed().embeddedValue, "embedded");
    }

    @Test
    public void testFoundAllMethods() {
        EntityModel model = getMapper().map(BaseLifecycleEntity.class).get(0);

        Map<Class<? extends Annotation>, List<ClassMethodPair>> methods = model.getLifecycleMethods();
        assertEquals(methods.size(), 1);
        List<ClassMethodPair> list = methods.get(PostLoad.class);
        assertNotNull(list);
        assertEquals(list.size(), 1);

        ClassMethodPair pair = list.get(0);
        assertEquals(pair.getMethod().getName(), "audit");

        model = getMapper().map(ChildLifecycleEntity.class).get(0);

        methods = model.getLifecycleMethods();
        assertEquals(methods.size(), 1);
        list = methods.get(PostLoad.class);
        assertNotNull(list);
        assertEquals(list.size(), 2);

        assertEquals(list.get(0).getMethod().getName(), "audit");
        assertEquals(list.get(1).getMethod().getName(), "childAudit");
    }

    @Test(expectedExceptions = CodecConfigurationException.class, dataProvider = "classes")
    public void testNonEntityDiscriminator(Class<?> baseClass, Class<?> childClass) {
        ObjectId id = saveChildEntity(NonEntity.class);
        getDs().find(baseClass).filter(Filters.eq("_id", id)).first();
    }

    @Test(expectedExceptions = CodecConfigurationException.class, dataProvider = "classes")
    public void testWrongDiscriminator(Class<?> baseClass, Class<?> childClass) {
        Document entity = new Document("_t", "Nonsense");
        ObjectId id = getDatabase().getCollection("entity").insertOne(entity).getInsertedId().asObjectId().getValue();
        getDs().find(baseClass).filter(Filters.eq("_id", id)).first();
    }

    private ObjectId saveChildEntity(Class<?> entityClass) {
        Document entity = new Document("_t", entityClass.getName());
        Document embed = new Document("_t", Embed.class.getName());
        embed.put("embeddedValue", "embedded");
        entity.put("embed", embed);
        return getDatabase().getCollection("entity").insertOne(entity).getInsertedId().asObjectId().getValue();
    }

    private interface Child {
        boolean getAudited();

        boolean getBaseAudited();

        Embed getEmbed();
    }

    @Entity(value = "entity")
    private static class BaseEntity {
        @Id
        ObjectId id;
        @Transient
        boolean audited = true;
    }

    @Entity(value = "entity")
    private static class BaseLifecycleEntity {
        @Id
        ObjectId id;
        @Transient
        boolean audited;

        @PostLoad
        void audit() {
            audited = true;
        }
    }

    private static class ChildEntity extends BaseEntity implements Child {
        Embed embed;

        @Override
        public boolean getAudited() {
            return audited;
        }

        @Override
        public boolean getBaseAudited() {
            return audited;
        }

        @Override
        public Embed getEmbed() {
            return embed;
        }
    }

    private static class ChildLifecycleEntity extends BaseLifecycleEntity implements Child {
        Embed embed;
        @Transient
        boolean childAudit;

        @Override
        public boolean getAudited() {
            return childAudit;
        }

        @Override
        public boolean getBaseAudited() {
            return audited;
        }

        @Override
        public Embed getEmbed() {
            return embed;
        }

        @PostLoad
        void childAudit() {
            childAudit = true;
        }
    }

    @Entity
    private static class Embed {
        String embeddedValue;
        @Id
        private ObjectId id;
    }

    private static class NonEntity implements Child {
        Embed embed;

        @Override
        public boolean getBaseAudited() {
            return true;
        }

        @Override
        public boolean getAudited() {
            return true;
        }

        @Override
        public Embed getEmbed() {
            return embed;
        }
    }
}
