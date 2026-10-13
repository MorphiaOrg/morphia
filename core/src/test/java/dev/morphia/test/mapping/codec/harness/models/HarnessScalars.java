package dev.morphia.test.mapping.codec.harness.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity("harness_scalars")
public class HarnessScalars {
    @Id
    private ObjectId id;
    private String name;
    private int age;
    private long count;
    private double score;
    private boolean active;
    private Integer boxed;
    private HarnessStatus status;
    private LocalDateTime created;
    private Date updated;
    private BigDecimal amount;

    public HarnessScalars() {
    }

    public static HarnessScalars sample() {
        HarnessScalars entity = new HarnessScalars();
        entity.id = new ObjectId("650000000000000000000001");
        entity.name = "scalars";
        entity.age = 42;
        entity.count = 9_000_000_000L;
        entity.score = 98.6;
        entity.active = true;
        entity.boxed = 7;
        entity.status = HarnessStatus.CLOSED;
        entity.created = LocalDateTime.of(2024, 2, 29, 13, 45, 30, 123_000_000);
        entity.updated = new Date(1_700_000_000_000L);
        entity.amount = new BigDecimal("1234.5600");
        return entity;
    }

    public ObjectId getId() {
        return id;
    }
}
