package dev.morphia.internal;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.CritterMapper;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.ReflectiveMapper;
import dev.morphia.mapping.codec.pojo.EntityModel;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Covers how {@link PathTarget} resolves a path segment through the subtypes of the model it's resolving against.
 */
public class TestPathTargetSubtypes {
    private static Mapper mapper(MapperType type) {
        MorphiaConfig config = MorphiaConfig.load().mapper(type);
        return type == MapperType.CRITTER ? new CritterMapper(config) : new ReflectiveMapper(config);
    }

    private static String translate(Mapper mapper, Class<?> root, String path) {
        return new PathTarget(mapper, mapper.getEntityModel(root), path, false).translatedPath();
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void resolvesThroughSubtypes(MapperType type) {
        Mapper mapper = mapper(type);
        mapper.map(Shape.class, Circle.class, Ring.class);

        Assertions.assertEquals("r", translate(mapper, Shape.class, "radius"));
        Assertions.assertEquals("w", translate(mapper, Shape.class, "width"), "a grandchild's property resolves from the root");
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void leavesLaterSegmentsAloneAfterAMiss(MapperType type) {
        Mapper mapper = mapper(type);
        mapper.map(Shape.class, Circle.class);

        // nothing in the hierarchy is called "unknown", so "radius" after it isn't Circle's radius
        Assertions.assertEquals("unknown.radius", translate(mapper, Shape.class, "unknown.radius"));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void resolvesThroughSubtypesMappedAfterTheFirstLookup(MapperType type) {
        Mapper mapper = mapper(type);
        mapper.map(Shape.class);
        Assertions.assertEquals("radius", translate(mapper, Shape.class, "radius"), "Circle isn't mapped yet");

        mapper.map(Circle.class);
        Assertions.assertEquals("r", translate(mapper, Shape.class, "radius"));
        Assertions.assertEquals("width", translate(mapper, Shape.class, "width"), "Ring isn't mapped yet");

        // linked under Circle, so Shape's view of its subtypes has to change too
        mapper.map(Ring.class);
        Assertions.assertEquals("w", translate(mapper, Shape.class, "width"));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void resolvesThroughImplementationsMappedLater(MapperType type) {
        Mapper mapper = mapper(type);
        EntityModel labeled = mapper.getEntityModel(Labeled.class);
        Assertions.assertEquals("label", new PathTarget(mapper, labeled, "label", false).translatedPath());

        mapper.map(Tag.class);
        Assertions.assertEquals("l", new PathTarget(mapper, labeled, "label", false).translatedPath());
    }

    @Entity("shapes")
    abstract static class Shape {
        @Id
        ObjectId id;
        String name;
    }

    @Entity("shapes")
    static class Circle extends Shape {
        @Property("r")
        double radius;
    }

    @Entity("shapes")
    static class Ring extends Circle {
        @Property("w")
        double width;
    }

    @Entity
    interface Labeled {
    }

    @Entity
    static class Tag implements Labeled {
        @Id
        ObjectId id;
        @Property("l")
        String label;
    }
}
