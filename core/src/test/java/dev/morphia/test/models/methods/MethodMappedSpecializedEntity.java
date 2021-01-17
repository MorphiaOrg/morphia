package dev.morphia.test.models.methods;

import dev.morphia.annotations.Entity;
import dev.morphia.test.models.Marker;

import java.util.UUID;

@Entity
public class MethodMappedSpecializedEntity extends MethodMappedGenericEntity<UUID> implements Marker {
}
