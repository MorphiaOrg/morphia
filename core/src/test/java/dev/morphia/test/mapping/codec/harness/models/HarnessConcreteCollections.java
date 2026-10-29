package dev.morphia.test.mapping.codec.harness.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * Properties declared as concrete collection and map types, which the codecs instantiate when decoding.
 */
@Entity("harness_concrete_collections")
public class HarnessConcreteCollections {
    @Id
    private ObjectId id;
    private ArrayList<String> list = new ArrayList<>();
    private LinkedHashSet<String> set = new LinkedHashSet<>();
    private LinkedList<String> linked = new LinkedList<>();
    private TreeMap<String, Integer> sorted = new TreeMap<>();
    private LinkedHashMap<String, Integer> ordered = new LinkedHashMap<>();
    private Labels labels = new Labels();

    public HarnessConcreteCollections() {
    }

    public static HarnessConcreteCollections sample() {
        HarnessConcreteCollections entity = new HarnessConcreteCollections();
        entity.id = new ObjectId("650000000000000000000120");
        entity.list.addAll(List.of("a", "b"));
        entity.set.addAll(List.of("c", "d"));
        entity.linked.addAll(List.of("e"));
        entity.sorted.put("z", 1);
        entity.sorted.put("y", 2);
        entity.ordered.put("first", 1);
        entity.ordered.put("second", 2);
        entity.labels.put("en", "hello");
        return entity;
    }

    public List<Object> values() {
        return List.of(list, set, linked, sorted, ordered, labels);
    }

    /**
     * A map type of the application's own, with a constructor the codec has to make accessible.
     */
    public static class Labels extends HashMap<String, String> {
        private Labels() {
        }
    }
}
