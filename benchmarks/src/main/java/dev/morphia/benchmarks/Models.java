package dev.morphia.benchmarks;

import java.util.List;
import java.util.function.Supplier;

import dev.morphia.benchmarks.models.Address;
import dev.morphia.benchmarks.models.ArraysEntity;
import dev.morphia.benchmarks.models.Circle;
import dev.morphia.benchmarks.models.CollectionsEntity;
import dev.morphia.benchmarks.models.ConcreteCollectionsEntity;
import dev.morphia.benchmarks.models.Drawing;
import dev.morphia.benchmarks.models.LazyReferenceEntity;
import dev.morphia.benchmarks.models.LifecycleEntity;
import dev.morphia.benchmarks.models.NestedEntity;
import dev.morphia.benchmarks.models.Profile;
import dev.morphia.benchmarks.models.Rectangle;
import dev.morphia.benchmarks.models.Shape;
import dev.morphia.benchmarks.models.SimpleEntity;
import dev.morphia.benchmarks.models.Square;

/**
 * The benchmark model set and sample instances.
 */
public enum Models {
    SIMPLE(SimpleEntity.class, SimpleEntity::sample),
    NESTED(NestedEntity.class, NestedEntity::sample),
    COLLECTIONS(CollectionsEntity.class, CollectionsEntity::sample),
    POLYMORPHIC(Drawing.class, Drawing::sample),
    LIFECYCLE(LifecycleEntity.class, LifecycleEntity::sample),
    ARRAYS(ArraysEntity.class, ArraysEntity::sample),
    CONCRETE_COLLECTIONS(ConcreteCollectionsEntity.class, ConcreteCollectionsEntity::sample),
    LAZY_REFERENCE(LazyReferenceEntity.class, LazyReferenceEntity::sample);

    /**
     * Every mapped type, in the order a mapper should see them.
     */
    public static final List<Class<?>> ALL_TYPES = List.of(Address.class, Profile.class, Shape.class, Circle.class,
            Square.class, Rectangle.class, SimpleEntity.class, NestedEntity.class, CollectionsEntity.class, Drawing.class,
            LifecycleEntity.class, ArraysEntity.class, ConcreteCollectionsEntity.class, LazyReferenceEntity.class);

    /**
     * The types {@code MappingBenchmark} maps: the original model set, kept fixed so the recorded mapping history stays
     * comparable. It leaves out {@link ArraysEntity}, which critter can't generate, so every new critter mapper would
     * retry and log the failure.
     */
    public static final List<Class<?>> MAPPING_TYPES = List.of(Address.class, Profile.class, Shape.class, Circle.class,
            Square.class, Rectangle.class, SimpleEntity.class, NestedEntity.class, CollectionsEntity.class, Drawing.class,
            LifecycleEntity.class);

    /**
     * The top-level entities exercised directly by the benchmarks.
     */
    public static final List<Class<?>> ROOT_TYPES = List.of(SimpleEntity.class, NestedEntity.class, CollectionsEntity.class,
            Drawing.class, LifecycleEntity.class, ArraysEntity.class, ConcreteCollectionsEntity.class,
            LazyReferenceEntity.class);

    /**
     * The concrete types whose generated models are checked by {@link MapperVariant#verify}.
     */
    public static final List<Class<?>> CONCRETE_TYPES = ALL_TYPES.stream()
            .filter(type -> !java.lang.reflect.Modifier.isAbstract(type.getModifiers()))
            .toList();

    private final Class<?> type;
    private final Supplier<Object> sample;

    Models(Class<?> type, Supplier<Object> sample) {
        this.type = type;
        this.sample = sample;
    }

    public Class<?> type() {
        return type;
    }

    public Object sample() {
        return sample.get();
    }
}
