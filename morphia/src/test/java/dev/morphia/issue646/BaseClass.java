package dev.morphia.issue646;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Id;

@Converters(SquareConverter.class)
public class BaseClass {

    @Id
    private ObjectId id;

    @Embedded
    private Square square;

}
