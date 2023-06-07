package dev.morphia.test.sharding;

import java.time.LocalDateTime;

import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.ShardKey;
import dev.morphia.annotations.ShardKeys;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.mapping.ShardKeyType.HASHED;
import static org.testng.Assert.assertEquals;

public class TestSharding extends TestBase {
    @Test
    public void testMapping() {
        Assert.assertThrows(ConstraintViolationException.class, () -> {
            getMapper().map(BadShardKeys.class);
        });
    }

    @Test
    public void testShardCollection() {
        checkMinServerVersion(6.0);
        withSharding(() -> {
            Datastore datastore = getDs();
            datastore.getDatabase().createCollection("split_brain"); // make sure the db exists on 4.0.x
            datastore.getMapper().map(Sharded.class);
            datastore.shardCollections();

            datastore.insert(new Sharded(new ObjectId(), "Linda Belcher"));

            Sharded bob = datastore.save(new Sharded(new ObjectId(), "Bob Belcher"));
            assertEquals(bob.name, "Bob Belcher");

            bob.nickName = "Bob 'The Burger Guy' Belcher";
            Sharded replaced = datastore.replace(bob);
            assertEquals(replaced.nickName, "Bob 'The Burger Guy' Belcher");

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
