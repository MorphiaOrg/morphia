package dev.morphia.mapping;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.config.MorphiaConfig;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Covers which class {@link Mapper#getClassFromCollection(String)} picks for a collection several classes map to.
 */
public class TestCollectionRoots {
    private static Mapper mapper(MapperType type) {
        MorphiaConfig config = MorphiaConfig.load().mapper(type);
        return type == MapperType.CRITTER ? new CritterMapper(config) : new ReflectiveMapper(config);
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void picksTheRootWhenSubtypesAreMappedFirst(MapperType type) {
        Mapper mapper = mapper(type);
        mapper.map(Circle.class, Square.class, Shape.class);

        Assertions.assertEquals(Shape.class, mapper.getClassFromCollection("shapes"));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void picksTheRootWhenItIsMappedFirst(MapperType type) {
        Mapper mapper = mapper(type);
        mapper.map(Shape.class, Circle.class, Square.class);

        Assertions.assertEquals(Shape.class, mapper.getClassFromCollection("shapes"));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void warnsOnceForUnrelatedRoots(MapperType type) {
        Mapper mapper = mapper(type);
        List<ILoggingEvent> warnings = captureWarnings(() -> {
            mapper.map(Circle.class, Shape.class, Square.class, Unrelated.class);

            Assertions.assertEquals(Shape.class, mapper.getClassFromCollection("shapes"),
                    "the first root registered, even after an unrelated one joins");
            Assertions.assertEquals(Shape.class, mapper.getClassFromCollection("shapes"));
        });

        Assertions.assertEquals(1, warnings.size(), "one warning per collection: " + warnings);
        Assertions.assertTrue(warnings.get(0).getFormattedMessage().contains(Unrelated.class.getName()));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void doesNotWarnForOneHierarchy(MapperType type) {
        Mapper mapper = mapper(type);
        List<ILoggingEvent> warnings = captureWarnings(() -> {
            mapper.map(Circle.class, Square.class, Shape.class);
            mapper.getClassFromCollection("shapes");
        });

        Assertions.assertEquals(List.of(), warnings);
    }

    /**
     * logback-test.xml sets dev.morphia to ERROR, so raise it while capturing. Test classes run concurrently, so only
     * this thread's events count.
     */
    private static List<ILoggingEvent> captureWarnings(Runnable body) {
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractMapper.class);
        Level level = logger.getLevel();
        String thread = Thread.currentThread().getName();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        try {
            body.run();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
        }
        return appender.list.stream()
                .filter(event -> event.getThreadName().equals(thread) && event.getLevel() == Level.WARN)
                .toList();
    }

    @Entity("shapes")
    abstract static class Shape {
        @Id
        ObjectId id;
    }

    @Entity("shapes")
    static class Circle extends Shape {
        double radius;
    }

    @Entity("shapes")
    static class Square extends Shape {
        double side;
    }

    @Entity("shapes")
    static class Unrelated {
        @Id
        ObjectId id;
    }
}
