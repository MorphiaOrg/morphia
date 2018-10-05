package xyz.morphia.logging.slf4j;

import org.bson.types.ObjectId;
import xyz.morphia.annotations.Id;

public class LoggingTestEntity {
    @Id
    private ObjectId id;

    private int i = 5;
}
