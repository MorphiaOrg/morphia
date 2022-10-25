package dev.morphia.test.models.methods;

import java.util.UUID;

import dev.morphia.annotations.Entity;
import dev.morphia.test.models.Marker;

@Entity
public class MethodMappedSpecializedEntity extends MethodMappedGenericEntity<UUID> implements Marker {
}
