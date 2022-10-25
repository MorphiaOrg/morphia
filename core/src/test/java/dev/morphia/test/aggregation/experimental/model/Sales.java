package dev.morphia.test.aggregation.experimental.model;

import java.util.Date;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity
public class Sales {
    @Id
    private ObjectId id;
    private String item;
    private Integer price;
    private Integer fee;
    private Date date;
}
