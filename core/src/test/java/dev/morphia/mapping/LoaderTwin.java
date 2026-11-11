package dev.morphia.mapping;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * Loaded again by another class loader in {@link TestRegistrationProtocol}.
 */
@Entity
public class LoaderTwin {
    @Id
    private ObjectId id;
}
