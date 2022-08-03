package dev.morphia.test.sharding;

import com.antwerkz.bottlerocket.clusters.ShardedCluster;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.ShardKey;
import dev.morphia.annotations.ShardKeys;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDateTime;

import static dev.morphia.annotations.ShardKeyType.HASHED;
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
        try (var cluster = new ShardedCluster()) {
            cluster.clean();
            cluster.start();
            withClient(cluster.getClient(), (datastore -> {
                datastore.getMapper().map(Sharded.class);
                datastore.shardCollections();

                datastore.insert(new Sharded(new ObjectId(), "Linda Belcher"));

                Sharded bob = datastore.save(new Sharded(new ObjectId(), "Bob Belcher"));
                assertEquals(bob.name, "Bob Belcher");

                bob.nickName = "Bob 'The Burger Guy' Belcher";
                Sharded replaced = datastore.replace(bob);
                assertEquals(replaced.nickName, "Bob 'The Burger Guy' Belcher");

                datastore.delete(bob);
            }));
        }
    }

    @Entity
    @ShardKeys({@ShardKey("name"), @ShardKey("age")})
    private static class BadShardKeys {
        @Id
        private ObjectId id;
    }

    @Entity("split_brain")
    @ShardKeys({@ShardKey(value = "name", type = HASHED), @ShardKey("date")})
    private static class Sharded {
        private final String name;
        @Id
        private ObjectId id;
        private String nickName;
        private LocalDateTime date;

        public Sharded(ObjectId id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
