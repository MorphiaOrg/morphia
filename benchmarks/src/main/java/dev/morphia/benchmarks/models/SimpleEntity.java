package dev.morphia.benchmarks.models;

import java.time.LocalDateTime;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * A flat entity of scalar fields only. Isolates per-property encode/decode overhead.
 */
@Entity("simple")
public class SimpleEntity {
    @Id
    private ObjectId id;
    private String name;
    private int age;
    private long count;
    private double score;
    private boolean active;
    private Status status;
    private LocalDateTime created;

    public SimpleEntity() {
    }

    public static SimpleEntity sample() {
        SimpleEntity entity = new SimpleEntity();
        entity.id = new ObjectId();
        entity.name = "Jane Doe";
        entity.age = 42;
        entity.count = 1_234_567_890L;
        entity.score = 98.6;
        entity.active = true;
        entity.status = Status.ACTIVE;
        entity.created = LocalDateTime.of(2024, 1, 15, 10, 30);
        return entity;
    }

    public ObjectId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}
