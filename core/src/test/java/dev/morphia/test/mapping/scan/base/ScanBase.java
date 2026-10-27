package dev.morphia.test.mapping.scan.base;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity
public class ScanBase {
    @Id
    public ObjectId id;
}
