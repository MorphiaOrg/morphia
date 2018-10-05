package xyz.morphia.issue646;

import org.bson.types.ObjectId;
import xyz.morphia.annotations.Converters;
import xyz.morphia.annotations.Embedded;
import xyz.morphia.annotations.Id;

@Converters(SquareConverter.class)
public class BaseClass {

    @Id
    private ObjectId id;

    @Embedded
    private Square square;

}
