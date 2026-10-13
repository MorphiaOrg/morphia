package dev.morphia.benchmarks.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * Like {@link CollectionsEntity}, but with collection and map fields declared as concrete types, which the collection
 * codecs instantiate by reflection rather than picking a default implementation.
 */
@Entity("concrete_collections")
public class ConcreteCollectionsEntity {
    @Id
    private ObjectId id;
    private ArrayList<String> tags;
    private HashSet<Integer> numbers;
    private LinkedHashMap<String, Integer> counts;
    private TreeMap<String, String> sorted;

    public ConcreteCollectionsEntity() {
    }

    public static ConcreteCollectionsEntity sample() {
        ConcreteCollectionsEntity entity = new ConcreteCollectionsEntity();
        entity.id = new ObjectId();
        entity.tags = new ArrayList<>();
        entity.numbers = new HashSet<>();
        entity.counts = new LinkedHashMap<>();
        entity.sorted = new TreeMap<>();
        for (int i = 0; i < 10; i++) {
            entity.tags.add("tag-" + i);
            entity.numbers.add(i);
            entity.counts.put("count-" + i, i);
            entity.sorted.put("key-" + i, "value-" + i);
        }
        return entity;
    }

    public ObjectId getId() {
        return id;
    }
}
