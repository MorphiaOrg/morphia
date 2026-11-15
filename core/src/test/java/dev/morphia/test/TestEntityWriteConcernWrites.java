package dev.morphia.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;

import dev.morphia.DeleteOptions;
import dev.morphia.InsertOneOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.query.FindAndDeleteOptions;
import dev.morphia.query.FindOptions;

import org.awaitility.Awaitility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.updates.UpdateOperators.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Writes made with default options use the entity's {@code @Entity(concern = ...)}. The entity here is unacknowledged, so a write
 * that would fail returns normally only when that concern is applied.
 */
public class TestEntityWriteConcernWrites extends TestBase {
    @Test
    public void insertUsesEntityConcern() {
        withTestConfig(List.of(Unacknowledged.class), () -> {
            getDs().applyIndexes();
            getDs().insert(new Unacknowledged("k"));
            getDs().insert(new Unacknowledged("k"));
        });
    }

    @Test
    public void saveUsesEntityConcern() {
        withTestConfig(List.of(Unacknowledged.class), () -> {
            getDs().applyIndexes();
            getDs().save(new Unacknowledged("k"));
            getDs().save(new Unacknowledged("k"));
        });
    }

    @Test
    public void listWritesUseEntityConcern() {
        withTestConfig(List.of(Unacknowledged.class), () -> {
            getDs().applyIndexes();
            getDs().insert(List.of(new Unacknowledged("k"), new Unacknowledged("k")));
            getDs().save(List.of(new Unacknowledged("j"), new Unacknowledged("j")));
        });
    }

    @Test
    public void replaceUsesEntityConcern() {
        withTestConfig(List.of(Unacknowledged.class), () -> {
            Unacknowledged missing = new Unacknowledged("k");
            missing.id = new ObjectId();
            // acknowledged, matching nothing would throw DocumentNotFoundException
            getDs().replace(missing);
        });
    }

    @Test
    public void deleteUsesEntityConcern() {
        withTestConfig(List.of(Unacknowledged.class), () -> {
            assertFalse(getDs().find(Unacknowledged.class).delete().wasAcknowledged());
            Unacknowledged entity = new Unacknowledged("k");
            entity.id = new ObjectId();
            assertFalse(getDs().delete(entity).wasAcknowledged());
        });
    }

    @Test
    public void alternateCollectionUsesEntityConcern() {
        withTestConfig(List.of(Unacknowledged.class), () -> {
            Unacknowledged entity = new Unacknowledged("k");
            entity.id = new ObjectId();
            InsertOneOptions options = new InsertOneOptions().collection("unack_alt");
            getDs().insert(entity, options);
            // acknowledged, the duplicate _id would throw
            getDs().insert(entity, options);
            assertFalse(getDs().find(Unacknowledged.class, new FindOptions().collection("unack_alt"))
                    .delete().wasAcknowledged());
        });
    }

    @Test
    public void optionsConcernOverridesEntityConcern() {
        withTestConfig(List.of(Unacknowledged.class), () -> {
            getDs().applyIndexes();
            InsertOneOptions options = new InsertOneOptions().writeConcern(WriteConcern.ACKNOWLEDGED);
            getDs().insert(new Unacknowledged("k"), options);
            assertThrows(MongoWriteException.class, () -> getDs().insert(new Unacknowledged("k"), options));
        });
    }

    @Test
    public void updateUsesEntityConcern() {
        withTestConfig(List.of(Unacknowledged.class), () -> {
            assertFalse(getDs().find(Unacknowledged.class).update(set("key", "u")).wasAcknowledged());
        });
    }

    @Test
    public void updateOptionsConcernOverridesEntityConcern() {
        withTestConfig(List.of(Unacknowledged.class), () -> {
            UpdateOptions options = new UpdateOptions().writeConcern(WriteConcern.ACKNOWLEDGED);
            assertTrue(getDs().find(Unacknowledged.class).update(options, set("key", "u")).wasAcknowledged());
        });
    }

    @Test
    public void deleteOptionsConcernOverridesEntityConcern() {
        withTestConfig(List.of(Unacknowledged.class), () -> {
            DeleteOptions options = new DeleteOptions().writeConcern(WriteConcern.ACKNOWLEDGED);
            assertTrue(getDs().find(Unacknowledged.class).delete(options).wasAcknowledged());
        });
    }

    @Test
    public void unacknowledgedMergeReturnsEntity() {
        withTestConfig(List.of(Unacknowledged.class), () -> {
            Unacknowledged entity = new Unacknowledged("k");
            getDs().insert(entity, new InsertOneOptions().writeConcern(WriteConcern.ACKNOWLEDGED));

            entity.key = "merged";
            assertSame(entity, getDs().merge(entity));
            awaitKey(entity.id, "merged");

            entity.key = "unset";
            assertSame(entity, getDs().merge(entity, new InsertOneOptions().unsetMissing(true)));
            awaitKey(entity.id, "unset");
        });
    }

    @Test
    public void findAndDeleteUsesEntityConcern() {
        // the driver sends no write concern at all for an unacknowledged findAndModify, so this uses an entity declaring w: 1
        withTestConfig(List.of(W1.class), () -> {
            MongoDatabase database = getDatabase();
            database.runCommand(new Document("profile", 0));
            database.getCollection("system.profile").drop();
            database.runCommand(new Document("profile", 2));
            try {
                getDs().find(W1.class).findAndDelete();
                getDs().find(W1.class).findAndDelete(new FindAndDeleteOptions().writeConcern(WriteConcern.JOURNALED));

                // profiler entries can show up after the commands return
                List<Document> concerns = Awaitility.await()
                        .atMost(5, TimeUnit.SECONDS)
                        .pollInterval(50, TimeUnit.MILLISECONDS)
                        .until(() -> database.getCollection("system.profile")
                                .find(new Document("command.findAndModify", "w1_writes"))
                                .map(entry -> entry.get("command", Document.class).get("writeConcern", Document.class))
                                .into(new ArrayList<>()), found -> found.size() >= 2);
                // {w: 1} can only come from the entity and {j: true} only from the options; entries in the same millisecond have no set order
                assertEquals(Set.of(new Document("w", 1), new Document("j", true)), new HashSet<>(concerns));
            } finally {
                database.runCommand(new Document("profile", 0));
            }
        });
    }

    private void awaitKey(ObjectId id, String key) {
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .until(() -> {
                    Document stored = getDatabase().getCollection("unack_writes").find(new Document("_id", id)).first();
                    return stored != null && key.equals(stored.getString("key"));
                });
    }

    @Entity(value = "unack_writes", concern = "UNACKNOWLEDGED")
    @Indexes(@Index(fields = @Field("key"), options = @IndexOptions(unique = true)))
    private static class Unacknowledged {
        @Id
        ObjectId id;
        String key;

        Unacknowledged() {
        }

        Unacknowledged(String key) {
            this.key = key;
        }
    }

    @Entity(value = "w1_writes", concern = "W1")
    private static class W1 {
        @Id
        ObjectId id;
    }
}
