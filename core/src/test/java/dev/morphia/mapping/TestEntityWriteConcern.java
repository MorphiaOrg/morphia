package dev.morphia.mapping;

import com.mongodb.WriteConcern;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.config.MorphiaConfig;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Covers the write concern an entity names in {@code @Entity(concern = ...)}.
 */
public class TestEntityWriteConcern {
    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void namedConcernsResolve(MapperType type) {
        Mapper mapper = mapper(type);

        Assertions.assertEquals(WriteConcern.MAJORITY, mapper.getEntityModel(Majority.class).writeConcern());
        Assertions.assertNull(mapper.getEntityModel(Unnamed.class).writeConcern());
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void unknownConcernsAreRejected(MapperType type) {
        Mapper mapper = mapper(type);

        MappingException e = Assertions.assertThrows(MappingException.class,
                () -> mapper.getEntityModel(Misspelled.class).writeConcern());
        Assertions.assertTrue(e.getMessage().contains("JOURNALLED"), e.getMessage());
    }

    private static Mapper mapper(MapperType type) {
        MorphiaConfig config = MorphiaConfig.load().mapper(type);
        return type == MapperType.CRITTER ? new CritterMapper(config) : new ReflectiveMapper(config);
    }

    @Entity(concern = "MAJORITY")
    static class Majority {
        @Id
        private ObjectId id;
    }

    @Entity
    static class Unnamed {
        @Id
        private ObjectId id;
    }

    @Entity(concern = "JOURNALLED")
    static class Misspelled {
        @Id
        private ObjectId id;
    }
}
