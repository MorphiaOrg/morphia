package dev.morphia.test.models;

import dev.morphia.annotations.Entity;

import java.util.UUID;

@Entity
public class SpecializedEntity extends GenericEntity<UUID> implements Marker {
}
