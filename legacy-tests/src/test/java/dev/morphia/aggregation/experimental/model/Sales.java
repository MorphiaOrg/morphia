package dev.morphia.aggregation.experimental.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.Date;

@Entity
public class Sales {
    @Id
    private ObjectId id;
    private String item;
    private Integer price;
    private Integer fee;
    private Date date;
}
