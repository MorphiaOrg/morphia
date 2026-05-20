package dev.morphia.test.mapping.codec.pojo;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.test.TestBase;
import dev.morphia.test.mapping.codec.pojo.generics.FullHashMap;
import dev.morphia.test.mapping.codec.pojo.generics.PartialHashMap;
import dev.morphia.test.mapping.codec.pojo.generics.PartialList;
import dev.morphia.test.mapping.codec.pojo.generics.Subtypes;
import dev.morphia.test.models.Hotel;
import dev.morphia.test.models.Hotel.Type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypeDataTest extends TestBase {
    @Test
    public void testWildcards() throws NoSuchFieldException {
        TypeData<?> typeData = TypeData.get(WildCard.class.getDeclaredField("listOfLists"));

        Assertions.assertEquals(List.class, typeData.getType());
        List<TypeData<?>> typeParameters = typeData.getTypeParameters();

        typeData = typeParameters.get(0);
        Assertions.assertEquals(List.class, typeData.getType());
        typeParameters = typeData.getTypeParameters();

        typeData = typeParameters.get(0);
        Assertions.assertEquals(String.class, typeData.getType());

    }

    @Test
    public void testEnums() {
        withConfig(buildConfig(Hotel.class), () -> {
            try {
                typeData(Hotel.class, "type", Type.class);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @Test
    public void testSubtypes() {
        withConfig(buildConfig(Subtypes.class), () -> {
            try {
                typeData(Subtypes.class, "partialHashMap", PartialHashMap.class, String.class, Integer.class);
                typeData(Subtypes.class, "fullHashMap", FullHashMap.class, String.class, LocalDateTime.class);
                typeData(Subtypes.class, "genericList", List.class, Locale.class);
                typeData(Subtypes.class, "partialList", PartialList.class, BitSet.class);
                typeData(Subtypes.class, "name", String.class);
                typeData(Subtypes.class, "age", int.class);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRecursiveTypes() {
        TypeData.get(MyEntity.class);
        TypeData.get(MyEmbeddedEntity.class);
    }

    // Morphia-3787: TypeData.get() inserts a raw Class into typeParameters when processing
    // self-referential generics (e.g. TypedId<T extends TypedId<T>>). The List<TypeData<?>>
    // contract is violated, causing a ClassCastException when callers iterate and call getType().
    @Test
    public void testSelfReferentialGenericId() throws NoSuchFieldException {
        TypeData<?> typeData = TypeData.get(EntityWithTypedId.class.getDeclaredField("id"));
        for (TypeData<?> param : typeData.getTypeParameters()) {
            param.getType(); // throws ClassCastException: Class cannot be cast to TypeData
        }
    }

    private abstract static class TypedId<T extends TypedId<T>> {
    }

    private static class ConcreteId extends TypedId<ConcreteId> {
    }

    private static class EntityWithTypedId {
        ConcreteId id;
    }

    private static void typeData(Class<?> owner, String fieldName, Class<?> fieldType, Class<?>... parameterTypes)
            throws NoSuchFieldException {
        Field field = owner.getDeclaredField(fieldName);
        TypeData<?> typeData = TypeData.get(field);
        Assertions.assertEquals(fieldType, typeData.getType());
        List<TypeData<?>> typeParameters = typeData.getTypeParameters();
        Assertions.assertEquals(parameterTypes.length, typeParameters.size());
        for (int i = 0; i < parameterTypes.length; i++) {
            Assertions.assertEquals(parameterTypes[i], typeParameters.get(i).getType());
        }
    }

    private static class WildCard {
        private List<? extends List<String>> listOfLists;
    }

    private static class MongoEntity<T> {
    }

    @Entity
    private static class MyEmbeddedEntity extends MongoEntity<MyEmbeddedEntity> {
    }

    @Entity
    private static class MyEntity {
        @Id
        private String id;
        private String address;
        List<MyEmbeddedEntity> embeddedEntities;
    }
}