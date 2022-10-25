package dev.morphia.test.models;

import java.util.UUID;

import dev.morphia.annotations.Entity;

@Entity
public class SpecializedEntity extends GenericEntity<UUID> implements Marker {
}
