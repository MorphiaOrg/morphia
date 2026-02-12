package dev.morphia.critter.sources;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;

import org.bson.types.ObjectId;

/**
 * Test entity with method-based property access including:
 * - Wide types (long, double)
 * - Read-only properties (getter without setter)
 */
@Entity("method_examples")
public class MethodExample {
    private ObjectId id;
    private long count;
    private double score;
    private String computedValue;

    public MethodExample() {
    }

    @Id
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    @Property("count")
    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Property("score")
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Read-only property: has getter with annotation but no setter.
     */
    @Property("computed")
    public String getComputedValue() {
        return computedValue;
    }

    // Note: No setter for computedValue - this is intentionally read-only
}
