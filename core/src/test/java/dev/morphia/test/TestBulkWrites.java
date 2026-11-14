package dev.morphia.test;

import java.util.List;

import com.mongodb.client.MongoDatabase;

import dev.morphia.InsertManyOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Transient;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
