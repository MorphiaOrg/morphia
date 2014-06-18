package org.mongodb.morphia.entities;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity
@SuppressWarnings("UnusedDeclaration")
public class SimpleEntity {
    @Id
    private ObjectId id;

    private String name;
    private Integer integer;
}
