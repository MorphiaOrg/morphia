package dev.morphia.benchmarks.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * An entity carrying an embedding vector, the typical large primitive array, plus a small object array.
 */
@Entity("arrays")
public class ArraysEntity {
    /**
     * The dimension of a common text-embedding model.
     */
    public static final int DIMENSIONS = 1536;

    @Id
    private ObjectId id;
    private double[] embedding;
    private String[] labels;

    public ArraysEntity() {
    }

    public static ArraysEntity sample() {
        ArraysEntity entity = new ArraysEntity();
        entity.id = new ObjectId();
        entity.embedding = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            entity.embedding[i] = Math.sin(i) / 2;
        }
        entity.labels = new String[20];
        for (int i = 0; i < entity.labels.length; i++) {
            entity.labels[i] = "label-" + i;
        }
        return entity;
    }

    public ObjectId getId() {
        return id;
    }
}
