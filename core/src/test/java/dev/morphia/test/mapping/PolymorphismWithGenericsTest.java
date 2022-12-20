package dev.morphia.test.mapping;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

public class PolymorphismWithGenericsTest extends TestBase {

    @Test
    public void reproduce() {
        getDs().save(new EOLCE());
    }

    private static class BaseEntity implements Comparable<BaseEntity> {
        @Id
        private ObjectId id;

        @Override
        public int compareTo(BaseEntity o) {
            return 0;
        }
    }

    @Entity
    private static class BCM {
        private String text = "";
    }

    private static class BCE<T extends BCM> extends BaseEntity {
        protected T message;
    }

    @Entity(value = "LCE")
    private static abstract class LCE<T extends BCM> extends BCE<T> {
        private ObjectId userId;
    }

    @Entity
    private static class BTCM extends BCM {
        private String messageId;
    }

    @Entity
    private static class ECM extends BTCM {
        private String threadId;
    }

    private static class OLCE<T extends BCM> extends LCE<T> {
        private long scheduledTimestamp;
    }

    private static class EOLCE extends OLCE<ECM> {
        private int priority;
    }
}
