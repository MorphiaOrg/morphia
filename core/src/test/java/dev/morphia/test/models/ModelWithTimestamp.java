package dev.morphia.test.models;

import dev.morphia.annotations.*;
import org.bson.types.ObjectId;

import java.sql.Time;
import java.sql.Timestamp;

@Entity(value = "TestModelWithTimestamp", useDiscriminator = false)
@Indexes({
        @Index(fields = @Field(value = "ts"), options = @IndexOptions(expireAfterSeconds = ModelWithTimestamp.SESSION_LINGER_SECONDS)),
})

public class ModelWithTimestamp {

    public static final int SESSION_LINGER_SECONDS = 60 * 60 * 24;

    @Id
    private ObjectId id;

    @Property(concreteClass = Timestamp.class)
    private Timestamp timestamp;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

}
