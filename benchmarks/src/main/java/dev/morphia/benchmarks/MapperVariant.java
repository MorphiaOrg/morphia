package dev.morphia.benchmarks;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.CritterMapper;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.ReflectiveMapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

/**
 * The mapper configurations under comparison. Each CI leg runs exactly one of these via {@code -p variant=...}.
 */
public enum MapperVariant {
    /**
     * {@link ReflectiveMapper} over the plain entity classes.
     */
    REFLECTION(MapperType.REFLECTION) {
        @Override
        void verify(Mapper mapper) {
            expect(mapper, Models.CONCRETE_TYPES, Tier.REFLECTION);
        }
    },

    /**
     * {@link CritterMapper} with the critter-aot jar ahead of the benchmarks jar on the classpath. The root entities
     * must load pre-generated models. Types critter-maven can't handle ahead of time (e.g. types with final fields) are
     * generated at runtime, which is what an application would get too; nothing may fall back to
     * reflection.
     */
    CRITTER(MapperType.CRITTER) {
        @Override
        void verify(Mapper mapper) {
            expect(mapper, Models.ROOT_TYPES, Tier.AOT);
            expect(mapper, Models.CONCRETE_TYPES, Tier.AOT, Tier.RUNTIME);
        }
    },

    /**
     * {@link CritterMapper} generating every model at runtime (no critter-aot jar on the classpath).
     */
    CRITTER_RUNTIME(MapperType.CRITTER) {
        @Override
        void verify(Mapper mapper) {
            expect(mapper, Models.CONCRETE_TYPES, Tier.RUNTIME);
        }
    };

    /**
     * Where an entity model came from.
     */
    public enum Tier {
        REFLECTION,
        AOT,
        RUNTIME;

        static Tier of(EntityModel model) {
            if (!(model instanceof CritterEntityModel)) {
                return REFLECTION;
            }
            return model.getClass().getClassLoader() instanceof CritterClassLoader ? RUNTIME : AOT;
        }
    }

    private final MapperType mapperType;

    MapperVariant(MapperType mapperType) {
        this.mapperType = mapperType;
    }

    /**
     * @param name the JMH parameter value, e.g. {@code critter-runtime}
     * @return the matching variant
     */
    public static MapperVariant of(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT).replace('-', '_'));
    }

    public MapperType mapperType() {
        return mapperType;
    }

    /**
     * Creates a fresh, empty mapper for this variant.
     */
    public Mapper newMapper(MorphiaConfig config) {
        return switch (mapperType) {
            case REFLECTION -> new ReflectiveMapper(config);
            case CRITTER -> new CritterMapper(config);
        };
    }

    /**
     * Fails if any type was not mapped the way this variant promises, so a misconfigured leg can't silently measure the
     * wrong thing.
     */
    abstract void verify(Mapper mapper);

    /**
     * @return a one-line summary of the tier each concrete type was mapped with
     */
    public static String describeTiers(Mapper mapper) {
        return Models.CONCRETE_TYPES.stream()
                .map(type -> type.getSimpleName() + "=" + Tier.of(mapper.getEntityModel(type)).name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
    }

    private static void expect(Mapper mapper, List<Class<?>> types, Tier... allowed) {
        List<Tier> expected = List.of(allowed);
        for (Class<?> type : types) {
            Tier actual = Tier.of(mapper.getEntityModel(type));
            if (!expected.contains(actual)) {
                throw new IllegalStateException("Expected %s to be mapped as %s but it was %s. Mapped tiers: %s"
                        .formatted(type.getName(), expected, actual, describeTiers(mapper)));
            }
        }
    }
}
