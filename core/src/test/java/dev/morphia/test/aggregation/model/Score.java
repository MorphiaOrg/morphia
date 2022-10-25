package dev.morphia.test.aggregation.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity(value = "scores", useDiscriminator = false)
public class Score {
    @Id
    private ObjectId id;
    private int score;
}
