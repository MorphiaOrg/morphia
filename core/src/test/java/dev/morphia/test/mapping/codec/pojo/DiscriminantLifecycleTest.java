package dev.morphia.test.mapping.codec.pojo;

import org.bson.Document;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.Transient;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.test.TestBase;

/**
 * asserts some common behaviour between entities with or without lifecycle
 */
public class DiscriminantLifecycleTest extends TestBase {

    @DataProvider(name = "classes")
    public Object[][] classes() {
        return new Object[][] {
                {BaseLifecycleEntity.class, ChildLifecycleEntity.class},
                {BaseEntity.class, ChildEntity.class}
        };
    }

    @Test(dataProvider = "classes")
    public void testCorrectEntity(Class<?> baseClass, Class<?> childClass) {
        ObjectId id = saveChildEntity(childClass);
        Child saved = (Child) getDs().find(baseClass).filter(Filters.eq("_id", id)).first();
        Assert.assertTrue(childClass.isInstance(saved));
        Assert.assertTrue(saved.getAudited());
        Assert.assertEquals("embedded", saved.getEmbed().embeddedValue);
    }

    @Test(expectedExceptions = CodecConfigurationException.class, dataProvider = "classes")
    public void testWrongDiscriminator(Class<?> baseClass, Class<?> childClass) {
        Document entity = new Document("_t", "Nonsense");
        ObjectId id = getDatabase().getCollection("entity").insertOne(entity).getInsertedId().asObjectId().getValue();;
        getDs().find(baseClass).filter(Filters.eq("_id", id)).first();
    }

    @Test(expectedExceptions = CodecConfigurationException.class, dataProvider = "classes")
    public void testNonEntityDiscriminator(Class<?> baseClass, Class<?> childClass) {
        ObjectId id = saveChildEntity(NonEntity.class);
        getDs().find(baseClass).filter(Filters.eq("_id", id)).first();
    }

    private ObjectId saveChildEntity(Class<?> entityClass) {
        Document entity = new Document("_t", entityClass.getName());
        Document embed = new Document("_t", Embed.class.getName());
        embed.put("embeddedValue", "embedded");
        entity.put("embed", embed);
        return getDatabase().getCollection("entity").insertOne(entity).getInsertedId().asObjectId().getValue();
    }

    interface Child {
        boolean getAudited();
        Embed getEmbed();
    }

    @Entity(value = "entity")
    static class BaseLifecycleEntity {
        @Id
        ObjectId id;
        @Transient
        boolean audited;

        @PostLoad
        void audit() {
            // audit entity
            audited = true;
        }
    }

    static class ChildLifecycleEntity extends BaseLifecycleEntity implements Child{
        Embed embed;

        @Override
        public boolean getAudited() {
            return audited;
        }

        @Override
        public Embed getEmbed() {
            return embed;
        }
    }

    @Entity(value = "entity")
    static class BaseEntity {
        @Id
        ObjectId id;
        @Transient
        boolean audited = true;
    }

    static class ChildEntity extends BaseEntity implements Child{
        Embed embed;

        @Override
        public boolean getAudited() {
            return audited;
        }

        @Override
        public Embed getEmbed() {
            return embed;
        }
    }


    @Entity
    static class Embed {
        String embeddedValue;
    }

    static class NonEntity implements Child {
        Embed embed;

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