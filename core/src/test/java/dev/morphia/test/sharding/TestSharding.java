package dev.morphia.test.sharding;

import java.time.LocalDateTime;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.ShardKey;
import dev.morphia.annotations.ShardKeys;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.mapping.ShardKeyType.HASHED;

public class TestSharding extends TestBase {
    @Test
    public void testMapping() {
        Assertions.assertThrows(ConstraintViolationException.class, () -> {
            getMapper().map(BadShardKeys.class);
        });
    }

    @Test
    public void testShardCollection() {
        checkMinServerVersion("6.0.0");
        withSharding(() -> {
            var datastore = getDs();
            datastore.getDatabase().createCollection("split_brain"); // make sure the db exists on 4.0.x
            datastore.getMapper().map(Sharded.class);
            datastore.shardCollections();

            datastore.insert(new Sharded(new ObjectId(), "Linda Belcher"));

            Sharded bob = datastore.save(new Sharded(new ObjectId(), "Bob Belcher"));
            Assertions.assertEquals("Bob Belcher", bob.name);

            bob.nickName = "Bob 'The Burger Guy' Belcher";
            Sharded replaced = datastore.replace(bob);
            Assertions.assertEquals("Bob 'The Burger Guy' Belcher", replaced.nickName);

            datastore.delete(bob);
        });
    }

    @Entity
    @ShardKeys({ @ShardKey("name"), @ShardKey("age") })
    private static class BadShardKeys {
        @Id
        private ObjectId id;
    }

    @Entity("split_brain")
    @ShardKeys({ @ShardKey(value = "name", type = HASHED) })
    private static class Sharded {
        @Id
        private ObjectId id;
        private final String name;
        private String nickName;
        private LocalDateTime date;

        public Sharded(ObjectId id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
