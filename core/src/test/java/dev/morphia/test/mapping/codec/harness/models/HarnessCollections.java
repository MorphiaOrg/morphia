package dev.morphia.test.mapping.codec.harness.models;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity("harness_collections")
public class HarnessCollections {
    @Id
    private ObjectId id;
    private List<String> tags;
    private Set<Integer> numbers;
    private List<HarnessAddress> addresses;
    private Map<String, HarnessAddress> addressesByName;
    private Map<Integer, String> namesByNumber;
    private List<List<String>> grid;

    public HarnessCollections() {
    }

    public static HarnessCollections sample() {
        HarnessCollections entity = new HarnessCollections();
        entity.id = new ObjectId("650000000000000000000004");
        entity.tags = List.of("a", "b", "c");
        // small Integers hash to themselves, so any Set implementation iterates these in this order
        entity.numbers = new LinkedHashSet<>(List.of(1, 2, 3));
        entity.addresses = List.of(new HarnessAddress("1 Main St", "Springfield"), new HarnessAddress("2 Side St", "Shelbyville"));
        // single-character keys hash to their own char value, so HashMap and LinkedHashMap agree on this order
        entity.addressesByName = new LinkedHashMap<>();
        entity.addressesByName.put("a", new HarnessAddress("3 Elm St", "Ogdenville"));
        entity.addressesByName.put("b", new HarnessAddress("4 Oak St", "North Haverbrook"));
        entity.namesByNumber = new LinkedHashMap<>();
        entity.namesByNumber.put(1, "one");
        entity.namesByNumber.put(2, "two");
        entity.grid = List.of(List.of("x", "o"), List.of("o", "x"));
        return entity;
    }

    public ObjectId getId() {
        return id;
    }
}
