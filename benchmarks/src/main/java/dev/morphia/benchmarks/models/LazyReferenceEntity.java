package dev.morphia.benchmarks.models;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;

import org.bson.types.ObjectId;

/**
 * An entity with lazy references. Decoding builds a proxy per reference without touching the database, so the cost
 * measured is proxy creation only.
 */
@Entity("lazy_references")
public class LazyReferenceEntity {
    @Id
    private ObjectId id;
    @Reference(lazy = true)
    private SimpleEntity single;
    @Reference(lazy = true)
    private List<SimpleEntity> many;

    public LazyReferenceEntity() {
    }

    public static LazyReferenceEntity sample() {
        LazyReferenceEntity entity = new LazyReferenceEntity();
        entity.id = new ObjectId();
        entity.single = SimpleEntity.sample();
        entity.many = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            entity.many.add(SimpleEntity.sample());
        }
        return entity;
    }

    public ObjectId getId() {
        return id;
    }
}
