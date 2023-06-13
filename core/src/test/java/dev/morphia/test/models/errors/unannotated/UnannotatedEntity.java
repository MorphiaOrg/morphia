package dev.morphia.test.models.errors.unannotated;

import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

public class UnannotatedEntity {
    @Id
    private ObjectId id;
    private String field;
    private Long number;
}
