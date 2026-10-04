package dev.morphia.benchmarks.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * An entity dominated by collections and maps, of both scalars and embedded types.
 */
@Entity("collections")
public class CollectionsEntity {
    @Id
    private ObjectId id;
    private List<String> tags;
    private Set<Integer> numbers;
    private Map<String, String> attributes;
    private List<Address> addresses;
    private Map<String, Address> addressBook;

    public CollectionsEntity() {
    }

    public static CollectionsEntity sample() {
        CollectionsEntity entity = new CollectionsEntity();
        entity.id = new ObjectId();
        entity.tags = new ArrayList<>();
        entity.numbers = new LinkedHashSet<>();
        entity.attributes = new HashMap<>();
        entity.addresses = new ArrayList<>();
        entity.addressBook = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            entity.tags.add("tag-" + i);
            entity.numbers.add(i * 31);
            entity.attributes.put("key-" + i, "value-" + i);
        }
        for (int i = 0; i < 5; i++) {
            Address address = new Address(i + " Elm St", "City " + i, "0000" + i, "US");
            entity.addresses.add(address);
            entity.addressBook.put("contact-" + i, address);
        }
        return entity;
    }

    public ObjectId getId() {
        return id;
    }

    public List<String> getTags() {
        return tags;
    }
}
