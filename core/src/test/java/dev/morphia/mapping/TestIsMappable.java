package dev.morphia.mapping;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.Id;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.references.MorphiaProxy;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Pins which types {@link Mapper#isMappable(Class)} accepts: those carrying a mapping annotation themselves, through a
 * superclass, or through any interface they or a superclass implement, directly or by extension.
 */
public class TestIsMappable {
    private static Mapper mapper(MapperType type) {
        MorphiaConfig config = MorphiaConfig.load().mapper(type);
        return type == MapperType.CRITTER ? new CritterMapper(config) : new ReflectiveMapper(config);
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void acceptsAnnotatedTypes(MapperType type) {
        Mapper mapper = mapper(type);
        for (Class<?> mappable : List.of(Annotated.class, Subclass.class, Implementor.class, ExtendedImplementor.class,
                SubclassOfImplementor.class, Tagged.class, ExtendedTagged.class, StandIn.class, Proxied.class)) {
            Assertions.assertTrue(mapper.isMappable(mappable), mappable.getSimpleName() + " should be mappable");
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void rejectsOtherTypes(MapperType type) {
        Mapper mapper = mapper(type);
        for (Class<?> unmappable : List.of(Plain.class, PlainImplementor.class, String.class, Object.class, Document.class,
                int.class, Untagged.class)) {
            Assertions.assertFalse(mapper.isMappable(unmappable), unmappable.getSimpleName() + " shouldn't be mappable");
        }
        Assertions.assertFalse(mapper.isMappable(null));
        Assertions.assertFalse(mapper.isMappable(MorphiaProxy.class), "a proxy type with no entity behind it");
    }

    @Entity
    static class Annotated {
        @Id
        ObjectId id;
    }

    static class Subclass extends Annotated {
    }

    @Entity
    interface Tagged {
    }

    interface ExtendedTagged extends Tagged {
    }

    interface Untagged {
    }

    static class Implementor implements Tagged {
    }

    static class ExtendedImplementor implements ExtendedTagged {
    }

    static class SubclassOfImplementor extends Implementor {
    }

    static class Plain {
    }

    static class PlainImplementor implements Untagged {
    }

    static class Target {
    }

    @ExternalEntity(target = Target.class)
    static class StandIn {
    }

    abstract static class Proxied extends Annotated implements MorphiaProxy {
    }
}
