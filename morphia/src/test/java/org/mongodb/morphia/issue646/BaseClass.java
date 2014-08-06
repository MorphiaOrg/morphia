package org.mongodb.morphia.issue646;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;

@Converters(SquareConverter.class)
public class BaseClass {

    @Id
    private ObjectId id;

    @Embedded
    private Square square;

}
