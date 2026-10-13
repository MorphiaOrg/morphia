package dev.morphia.test.mapping.codec.harness.models;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;

import org.bson.types.ObjectId;

/**
 * Decoding a reference fetches it from the database, so the harness only encodes this entity.
 */
@Entity("harness_references")
public class HarnessReferences {
    @Id
    private ObjectId id;
    @Reference
    private HarnessScalars dbRef;
    @Reference(idOnly = true)
    private HarnessScalars idOnly;
    @Reference(idOnly = true)
    private List<HarnessNested> many;

    public HarnessReferences() {
    }

    public static HarnessReferences sample() {
        HarnessReferences entity = new HarnessReferences();
        entity.id = new ObjectId("65000000000000000000000c");
        entity.dbRef = HarnessScalars.sample();
        entity.idOnly = HarnessScalars.sample();
        entity.many = List.of(HarnessNested.sample(), HarnessNested.sparse());
        return entity;
    }

    public ObjectId getId() {
        return id;
    }
}
