package org.mongodb.morphia.logging.slf4j;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

public class LoggingTestEntity {
    @Id
    private ObjectId id;

    private int i = 5;
}
