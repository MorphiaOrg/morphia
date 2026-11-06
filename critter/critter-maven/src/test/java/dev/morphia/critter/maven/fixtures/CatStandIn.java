package dev.morphia.critter.maven.fixtures;

import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.Id;
import dev.morphia.critter.maven.shared.Stored;

import org.bson.types.ObjectId;

/**
 * A stand-in that also collects {@code @Entity} from its interface.
 */
@ExternalEntity(target = Cat.class, value = "stand_ins")
public class CatStandIn implements Stored {
    @Id
    private ObjectId id;
    private String name;
}
