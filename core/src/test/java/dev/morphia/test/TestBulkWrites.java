package dev.morphia.test;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.MongoDatabase;

import dev.morphia.BulkWriteFailure;
import dev.morphia.InsertManyOptions;
import dev.morphia.MorphiaBulkWriteException;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Transient;
import dev.morphia.annotations.Version;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBulkWrites extends TestBase {

    private int insertCommands(String collection, Runnable body) {
        return insertCommands(body, collection)[0];
    }

    /**
     * Counts the insert commands the server sees for each collection while {@code body} runs.
     */
    private int[] insertCommands(Runnable body, String... collections) {
        MongoDatabase database = getDatabase();
        database.runCommand(new Document("profile", 0));
        database.getCollection("system.profile").drop();
        database.runCommand(new Document("profile", 2));
        try {
            body.run();
        } finally {
            database.runCommand(new Document("profile", 0));
        }
        int[] counts = new int[collections.length];
        for (int i = 0; i < collections.length; i++) {
            counts[i] = (int) database.getCollection("system.profile")
                    .countDocuments(new Document("op", "insert").append("ns", database.getName() + "." + collections[i]));
        }
        return counts;
    }

    @Test
    public void parentAndSubclassShareOneInsert() {
        withTestConfig(List.of(Animal.class, Dog.class), () -> {
            int commands = insertCommands("animals",
                    () -> getDs().insert(List.of(new Animal("a"), new Dog("d"), new Animal("a2"))));
            assertEquals(1, commands);
            List<String> types = getDatabase().getCollection("animals").find()
                    .map(d -> d.getString("_t"))
                    .into(new java.util.ArrayList<>());
            assertEquals(List.of(Animal.class.getName(), Dog.class.getName(), Animal.class.getName()), types);
        });
    }

    @Test
    public void unrelatedClassesSameCollectionShareOneInsert() {
        withTestConfig(List.of(Foo.class, Bar.class), () -> {
            int commands = insertCommands("shared", () -> getDs().insert(List.of(new Foo(), new Bar(), new Foo())));
            assertEquals(1, commands);
            assertEquals(3, getDatabase().getCollection("shared").countDocuments());
        });
    }

    @Test
    public void lifecycleCallbacksFireOnce() {
        withTestConfig(List.of(Counted.class, CountedChild.class), () -> {
            Counted parent = new Counted();
            CountedChild child = new CountedChild();
            getDs().insert(List.of(parent, child));
            assertEquals(1, parent.prePersists);
            assertEquals(1, child.prePersists);
        });
    }

    @Test
    public void subclassWithOwnCollectionBatchedSeparately() {
        withTestConfig(List.of(Animal.class, Cat.class), () -> {
            int[] commands = insertCommands(
                    () -> getDs().insert(List.of(new Animal("a"), new Cat("c"), new Animal("a2"))), "animals", "cats");
            assertEquals(1, commands[0]);
            assertEquals(1, commands[1]);
        });
    }

    @Test
    public void differentWriteConcernsBatchedSeparately() {
        withTestConfig(List.of(Majority.class, W1.class, AlsoW1.class), () -> {
            // the options' write concern overrides the entities', so clear it to let the @Entity concerns apply
            InsertManyOptions options = new InsertManyOptions().writeConcern(null);
            assertEquals(2, insertCommands("shared2",
                    () -> getDs().insert(List.of(new Majority(), new W1(), new Majority()), options)));
            assertEquals(1, insertCommands("shared2",
                    () -> getDs().insert(List.of(new W1(), new AlsoW1(), new W1()), options)));
        });
    }

    @Test
    public void emptyAndAllSucceed() {
        withTestConfig(List.of(Animal.class), () -> {
            getDs().insert(List.<Animal> of());
            getDs().insert(List.of(new Animal("a"), new Animal("b"), new Animal("c")));
            assertEquals(3, getDs().find(Animal.class).count());
        });
    }

    private long count(String collection) {
        return getDatabase().getCollection(collection).countDocuments();
    }

    private static List<String> keys(List<Reading> readings) {
        return readings.stream().map(r -> r.key).toList();
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 2, 4 })
    public void orderedDuplicateAtStartMiddleEnd(int position) {
        withTestConfig(List.of(Reading.class), () -> {
            getDs().applyIndexes();
            getDs().save(new Reading("k"));
            List<Reading> list = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                list.add(new Reading(i == position ? "k" : "r" + i));
            }

            MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class, () -> getDs().insert(list));
            assertEquals(List.of(position), e.getFailures().stream().map(BulkWriteFailure::index).toList());
            assertSame(list.get(position), e.getFailedEntities().get(0));
            assertEquals(list.subList(position + 1, 5), e.getUnprocessedEntities());
            assertEquals(position, e.getWriteErrors().get(0).getIndex());
            assertEquals(position + 1, count("readings"));
        });
    }

    @Test
    public void unorderedStoresEverythingValid() {
        withTestConfig(List.of(Reading.class), () -> {
            getDs().applyIndexes();
            getDs().save(new Reading("k"));
            List<Reading> list = List.of(new Reading("a"), new Reading("k"), new Reading("b"), new Reading("k"), new Reading("c"));

            MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class,
                    () -> getDs().insert(list, new InsertManyOptions().ordered(false)));
            assertEquals(List.of(1, 3), e.getFailures().stream().map(BulkWriteFailure::index).toList());
            assertTrue(e.getUnprocessedEntities().isEmpty());
            assertEquals(4, count("readings"));
        });
    }

    @Test
    public void twoCollectionsOrdered() {
        withTestConfig(List.of(Reading.class, Note.class), () -> {
            getDs().applyIndexes();
            getDs().save(new Reading("k"));
            Note n1 = new Note();
            Note n2 = new Note();
            List<Object> list = List.of(new Reading("a"), n1, new Reading("k"), n2);

            MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class, () -> getDs().insert(list));
            assertEquals(2, e.getWriteErrors().get(0).getIndex());
            assertSame(list.get(2), e.getFailedEntities().get(0));
            assertEquals(List.of(n1, n2), e.getUnprocessedEntities());
            assertEquals(0, count("notes"));
        });
    }

    @Test
    public void twoCollectionsUnordered() {
        withTestConfig(List.of(Reading.class, Note.class), () -> {
            getDs().applyIndexes();
            getDs().save(new Reading("k"));
            List<Object> list = List.of(new Note(), new Reading("k"), new Note(), new Reading("a"));

            MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class,
                    () -> getDs().insert(list, new InsertManyOptions().ordered(false)));
            assertEquals(1, e.getWriteErrors().get(0).getIndex());
            assertSame(list.get(1), e.getFailedEntities().get(0));
            assertTrue(e.getUnprocessedEntities().isEmpty());
            assertEquals(2, count("notes"));
            assertEquals(2, count("readings"));
        });
    }

    @Test
    public void mixedClassBatchOrdered() {
        withTestConfig(List.of(Reading.class, SpecialReading.class), () -> {
            getDs().applyIndexes();
            getDs().save(new Reading("k"));
            SpecialReading s1 = new SpecialReading("s1");
            SpecialReading s2 = new SpecialReading("s2");
            List<Reading> list = List.of(new Reading("a"), new Reading("k"), s1, s2);

            MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class, () -> getDs().insert(list));
            assertEquals(List.of(s1, s2), e.getUnprocessedEntities());
            assertEquals(2, count("readings"));
        });
    }

    @Test
    public void sameInstanceTwice() {
        withTestConfig(List.of(Reading.class), () -> {
            getDs().applyIndexes();
            Reading r = new Reading("a");

            MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class, () -> getDs().insert(List.of(r, r)));
            assertEquals(List.of(1), e.getFailures().stream().map(BulkWriteFailure::index).toList());
            assertEquals(1, count("readings"));
            assertNotNull(r.id, "the instance was stored at index 0, so its id must survive the failure at index 1");
        });
    }

    @Test
    public void equalByValueEntitiesStayDistinct() {
        withTestConfig(List.of(Reading.class), () -> {
            getDs().applyIndexes();
            getDs().save(new Reading("k"));
            Reading first = new Reading("k");
            Reading second = new Reading("k");

            MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class,
                    () -> getDs().insert(List.of(first, second), new InsertManyOptions().ordered(false)));
            assertEquals(List.of(0, 1), e.getFailures().stream().map(BulkWriteFailure::index).toList());
            assertSame(first, e.getFailedEntities().get(0));
            assertSame(second, e.getFailedEntities().get(1));
        });
    }

    @Test
    public void immutableInputList() {
        withTestConfig(List.of(Reading.class), () -> {
            getDs().applyIndexes();
            getDs().save(new Reading("k"));
            List<Reading> list = List.of(new Reading("a"), new Reading("k"), new Reading("b"));

            MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class, () -> getDs().insert(list));
            assertEquals(List.of("a", "k", "b"), keys(list));
            assertThrows(UnsupportedOperationException.class, () -> e.getFailedEntities().clear());
        });
    }

    @Test
    public void versionedInsertRollsBackOnlyNotStored() {
        withTestConfig(List.of(VersionedReading.class), () -> {
            getDs().applyIndexes();
            getDs().save(new VersionedReading("k"));
            List<VersionedReading> list = List.of(new VersionedReading("a"), new VersionedReading("b"),
                    new VersionedReading("k"), new VersionedReading("c"));

            assertThrows(MorphiaBulkWriteException.class, () -> getDs().insert(list));
            assertEquals(1L, list.get(0).version);
            assertEquals(1L, list.get(1).version);
            assertNull(list.get(2).version);
            assertNull(list.get(3).version);
        });
    }

    @Test
    public void generatedIdsClearedOnNotStored() {
        withTestConfig(List.of(Reading.class), () -> {
            getDs().applyIndexes();
            getDs().save(new Reading("k"));
            Reading stored = new Reading("x");
            Reading dup = new Reading("k");
            Reading unsent = new Reading("y");
            Reading assigned = new Reading("z");
            ObjectId assignedId = new ObjectId();
            assigned.id = assignedId;

            assertThrows(MorphiaBulkWriteException.class, () -> getDs().insert(List.of(stored, dup, unsent, assigned)));
            assertNotNull(stored.id);
            assertNull(dup.id);
            assertNull(unsent.id);
            assertEquals(assignedId, assigned.id);
        });
    }

    @Test
    public void retryNotStoredSucceeds() {
        withTestConfig(List.of(Reading.class), () -> {
            getDs().applyIndexes();
            getDs().save(new Reading("k"));
            List<Reading> list = List.of(new Reading("a"), new Reading("k"), new Reading("b"), new Reading("c"));

            MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class, () -> getDs().insert(list));
            getDatabase().getCollection("readings").deleteOne(new Document("key", "k"));
            List<Reading> retry = new ArrayList<>(e.<Reading> getFailedEntities());
            retry.addAll(e.getUnprocessedEntities());

            getDs().save(retry);
            assertEquals(4, count("readings"));
        });
    }

    @Test
    public void nonGeneratedIdTypeNotReset() {
        withTestConfig(List.of(LongIdReading.class), () -> {
            getDs().applyIndexes();
            getDs().save(new LongIdReading(1, "k"));
            LongIdReading dup = new LongIdReading(2, "k");
            LongIdReading unsent = new LongIdReading(3, "y");

            assertThrows(MorphiaBulkWriteException.class, () -> getDs().insert(List.of(dup, unsent)));
            assertEquals(2, dup.id);
            assertEquals(3, unsent.id);
        });
    }

    @Entity("readings")
    @Indexes(@Index(fields = @Field("key"), options = @IndexOptions(unique = true)))
    private static class Reading {
        @Id
        ObjectId id;
        String key;

        Reading() {
        }

        Reading(String key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Reading other && java.util.Objects.equals(key, other.key);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hashCode(key);
        }

        @Override
        public String toString() {
            return "Reading(" + key + ")";
        }
    }

    @Entity("readings")
    private static class SpecialReading extends Reading {
        SpecialReading() {
        }

        SpecialReading(String key) {
            super(key);
        }
    }

    @Entity("notes")
    private static class Note {
        @Id
        ObjectId id;
    }

    @Entity("vreadings")
    @Indexes(@Index(fields = @Field("key"), options = @IndexOptions(unique = true)))
    private static class VersionedReading {
        @Id
        ObjectId id;
        @Version
        Long version;
        String key;

        VersionedReading() {
        }

        VersionedReading(String key) {
            this.key = key;
        }
    }

    @Entity("lreadings")
    @Indexes(@Index(fields = @Field("key"), options = @IndexOptions(unique = true)))
    private static class LongIdReading {
        @Id
        long id;
        String key;

        LongIdReading() {
        }

        LongIdReading(long id, String key) {
            this.id = id;
            this.key = key;
        }
    }

    @Entity("animals")
    private static class Animal {
        @Id
        ObjectId id;
        String name;

        Animal() {
        }

        Animal(String name) {
            this.name = name;
        }
    }

    @Entity("animals")
    private static class Dog extends Animal {
        Dog() {
        }

        Dog(String name) {
            super(name);
        }
    }

    @Entity("cats")
    private static class Cat extends Animal {
        Cat() {
        }

        Cat(String name) {
            super(name);
        }
    }

    @Entity("shared")
    private static class Foo {
        @Id
        ObjectId id;
    }

    @Entity("shared")
    private static class Bar {
        @Id
        ObjectId id;
    }

    @Entity("counted")
    private static class Counted {
        @Id
        ObjectId id;
        @Transient
        int prePersists;

        @PrePersist
        void prePersist() {
            prePersists++;
        }
    }

    @Entity("counted")
    private static class CountedChild extends Counted {
    }

    @Entity(value = "shared2", concern = "MAJORITY")
    private static class Majority {
        @Id
        ObjectId id;
    }

    @Entity(value = "shared2", concern = "W1")
    private static class W1 {
        @Id
        ObjectId id;
    }

    @Entity(value = "shared2", concern = "W1")
    private static class AlsoW1 {
        @Id
        ObjectId id;
    }
}
