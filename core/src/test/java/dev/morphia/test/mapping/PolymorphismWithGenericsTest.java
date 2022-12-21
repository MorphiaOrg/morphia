package dev.morphia.test.mapping;

import com.antwerkz.bottlerocket.BottleRocket;
import com.antwerkz.bottlerocket.BottleRocketTest;
import com.github.zafarkhaja.semver.Version;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.NamingStrategy;

import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.Test;

public class PolymorphismWithGenericsTest extends BottleRocketTest {

    @Test
    public void reproduce() {
        MongoClient mongo = getMongoClient();
        MongoDatabase database = getDatabase();
        database.drop();
        Datastore datastore = Morphia.createDatastore(mongo, getDatabase().getName(),
                MapperOptions.builder()
                        .mapSubPackages(true)
                        .discriminator(DiscriminatorFunction.className())
                        .discriminatorKey("className")
                        .collectionNaming(NamingStrategy.identity())
                        .propertyNaming(NamingStrategy.identity())
                        .storeEmpties(true)
                        .build());
        datastore.save(new EOLCE());
    }

    @NotNull
    @Override
    public String databaseName() {
        return "morphia_repro";
    }

    @Nullable
    @Override
    public Version version() {
        return BottleRocket.DEFAULT_VERSION;
    }

    public static class BaseEntity implements Comparable<BaseEntity> {
        @Id
        protected ObjectId id;

        @Override
        public int compareTo(BaseEntity o) {
            return 0;
        }
    }

    @Entity
    public static class BCM {
        private String text = "";
    }

    public static class BCE<T extends BCM> extends BaseEntity {
        protected T message;
    }

    @Entity(value = "LCE")
    public static abstract class LCE<T extends BCM> extends BCE<T> {
        private ObjectId userId;
    }

    @Entity
    public static class BTCM extends BCM {
        private String messageId;
    }

    @Entity
    public static class ECM extends BTCM {
        private String threadId;
    }

    public static class OLCE<T extends BCM> extends LCE<T> {
        private long scheduledTimestamp;
    }

    public static class EOLCE extends OLCE<ECM> {
        private int priority;
    }
}
