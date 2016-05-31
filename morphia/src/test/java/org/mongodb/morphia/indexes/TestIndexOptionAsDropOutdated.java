package org.mongodb.morphia.indexes;

import com.mongodb.MongoCommandException;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Ulrich Cech
 */
@SuppressWarnings("unused")
public class TestIndexOptionAsDropOutdated extends TestBase {

    @Test
    public void testDropOutdatedIndexWhenOptionsHaveChangedWithUnnamedIndex() {
        executeTest(EnsureIndexWithDropOutdatedIndex1.class,
                    EnsureIndexWithDropOutdatedIndex2.class,
                    EnsureIndexWithDropOutdatedIndex3.class);
    }

    @Test
    public void testDropOutdatedIndexWhenOptionsHaveChangedWithNamedIndex() {
        executeTest(EnsureIndexWithDropOutdatedIndex4.class,
                    EnsureIndexWithDropOutdatedIndex5.class,
                    EnsureIndexWithDropOutdatedIndex6.class);
    }

    @Test
    public void testDropOutdatedIndexWhenOptionsHaveChangedWithClassIndex() {
        executeTest(EnsureIndexWithDropOutdatedIndex7.class,
                    EnsureIndexWithDropOutdatedIndex8.class,
                    EnsureIndexWithDropOutdatedIndex9.class);
    }

    private void executeTest(final Class entityWithIndex, final Class entityWithDropOutdated, final Class entityWithoutDropOutdated) {
        if (serverIsAtLeastVersion(2.6)) {
            getMorphia().map(entityWithIndex);
            getDs().ensureIndexes();
            getMorphia().map(entityWithDropOutdated);
            try {
                getDs().ensureIndexes();
            } catch (MongoCommandException ex) {
                fail("Changed index should have been updated.");
            }
            getMorphia().map(entityWithoutDropOutdated);
            try {
                getDs().ensureIndexes();
                fail("Should throw a MongoCommandException for existing index with different options.");
            } catch (MongoCommandException ex) {
                assertThat(ex.getMessage().startsWith("Command failed with error 85"), is(true));
            }
        }
    }


    private static class BaseEntity {
        @Id
        private ObjectId id;
    }


    @Entity(value = "IndexCollection")
    private static class EnsureIndexWithDropOutdatedIndex1 extends BaseEntity {
        @Indexed(options = @IndexOptions(unique = true))
        private long indexField;
    }

    @Entity(value = "IndexCollection")
    private static class EnsureIndexWithDropOutdatedIndex2 extends BaseEntity {
        @Indexed(options = @IndexOptions(dropOutdated = true))
        private long indexField;
    }

    @Entity(value = "IndexCollection")
    private static class EnsureIndexWithDropOutdatedIndex3 extends BaseEntity {
        @Indexed(options = @IndexOptions())
        private long indexField;
    }


    @Entity(value = "IndexCollection")
    private static class EnsureIndexWithDropOutdatedIndex4 extends BaseEntity {
        @Indexed(options = @IndexOptions(name = "index1", unique = true))
        private long indexField;
    }

    @Entity(value = "IndexCollection")
    private static class EnsureIndexWithDropOutdatedIndex5 extends BaseEntity {
        @Indexed(options = @IndexOptions(name = "index1", dropOutdated = true))
        private long indexField;
    }

    @Entity(value = "IndexCollection")
    private static class EnsureIndexWithDropOutdatedIndex6 extends BaseEntity {
        @Indexed(options = @IndexOptions(name = "index1"))
        private long indexField;
    }


    @Entity(value = "IndexCollection")
    @Indexes(@Index(fields = @Field(value = "indexField"), options = @IndexOptions(unique = true)))
    private static class EnsureIndexWithDropOutdatedIndex7 extends BaseEntity {
        private long indexField;
    }

    @Entity(value = "IndexCollection")
    @Indexes(@Index(fields = @Field(value = "indexField"), options = @IndexOptions(dropOutdated = true)))
    private static class EnsureIndexWithDropOutdatedIndex8 extends BaseEntity {
        private long indexField;
    }

    @Entity(value = "IndexCollection")
    @Indexes(@Index(fields = @Field(value = "indexField")))
    private static class EnsureIndexWithDropOutdatedIndex9 extends BaseEntity {
        private long indexField;
    }

}
