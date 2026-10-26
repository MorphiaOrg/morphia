package dev.morphia.internal;

import java.time.Duration;
import java.util.List;

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
    private static final List<Class<?>> CHAIN = List.of(Chain01.class, Chain02.class, Chain03.class, Chain04.class, Chain05.class,
            Chain06.class, Chain07.class, Chain08.class, Chain09.class, Chain10.class, Chain11.class, Chain12.class, Chain13.class,
            Chain14.class, Chain15.class, Chain16.class, Chain17.class, Chain18.class, Chain19.class, Chain20.class, Chain21.class,
            Chain22.class, Chain23.class, Chain24.class, Chain25.class, Chain26.class, Chain27.class, Chain28.class, Chain29.class,
            Chain30.class, Chain31.class, Chain32.class);

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

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void linksADeepInterfaceChainRootFirstQuickly(MapperType type) {
        Mapper mapper = mapper(type);
        // each link is linked under every interface above it, so invalidating along every path is exponential
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            for (Class<?> link : CHAIN) {
                mapper.getEntityModel(link);
            }
            mapper.getEntityModel(ChainLink.class);
        });
        Assertions.assertEquals(32, mapper.getEntityModel(Chain01.class).allSubtypes().size());
    }

    @Entity
    interface Chain01 {
    }

    @Entity
    interface Chain02 extends Chain01 {
    }

    @Entity
    interface Chain03 extends Chain02 {
    }

    @Entity
    interface Chain04 extends Chain03 {
    }

    @Entity
    interface Chain05 extends Chain04 {
    }

    @Entity
    interface Chain06 extends Chain05 {
    }

    @Entity
    interface Chain07 extends Chain06 {
    }

    @Entity
    interface Chain08 extends Chain07 {
    }

    @Entity
    interface Chain09 extends Chain08 {
    }

    @Entity
    interface Chain10 extends Chain09 {
    }

    @Entity
    interface Chain11 extends Chain10 {
    }

    @Entity
    interface Chain12 extends Chain11 {
    }

    @Entity
    interface Chain13 extends Chain12 {
    }

    @Entity
    interface Chain14 extends Chain13 {
    }

    @Entity
    interface Chain15 extends Chain14 {
    }

    @Entity
    interface Chain16 extends Chain15 {
    }

    @Entity
    interface Chain17 extends Chain16 {
    }

    @Entity
    interface Chain18 extends Chain17 {
    }

    @Entity
    interface Chain19 extends Chain18 {
    }

    @Entity
    interface Chain20 extends Chain19 {
    }

    @Entity
    interface Chain21 extends Chain20 {
    }

    @Entity
    interface Chain22 extends Chain21 {
    }

    @Entity
    interface Chain23 extends Chain22 {
    }

    @Entity
    interface Chain24 extends Chain23 {
    }

    @Entity
    interface Chain25 extends Chain24 {
    }

    @Entity
    interface Chain26 extends Chain25 {
    }

    @Entity
    interface Chain27 extends Chain26 {
    }

    @Entity
    interface Chain28 extends Chain27 {
    }

    @Entity
    interface Chain29 extends Chain28 {
    }

    @Entity
    interface Chain30 extends Chain29 {
    }

    @Entity
    interface Chain31 extends Chain30 {
    }

    @Entity
    interface Chain32 extends Chain31 {
    }

    @Entity
    static class ChainLink implements Chain32 {
        @Id
        ObjectId id;
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
