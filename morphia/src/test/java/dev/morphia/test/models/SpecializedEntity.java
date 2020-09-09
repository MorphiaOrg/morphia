package dev.morphia.test.models;

import dev.morphia.annotations.Entity;

import java.util.UUID;

interface Marker extends SuperMarker {

}

@Entity("superMarker")
interface SuperMarker {

}

@Entity
public class SpecializedEntity extends GenericEntity<UUID> implements Marker {
}