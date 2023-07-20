package dev.morphia.test.mapping.codec.pojo;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.test.TestBase;
import dev.morphia.test.mapping.codec.pojo.generics.FullHashMap;
import dev.morphia.test.mapping.codec.pojo.generics.PartialHashMap;
import dev.morphia.test.mapping.codec.pojo.generics.PartialList;
import dev.morphia.test.mapping.codec.pojo.generics.Subtypes;
import dev.morphia.test.models.Hotel;
import dev.morphia.test.models.Hotel.Type;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TypeDataTest extends TestBase {
    @Test
    public void testWildcards() throws NoSuchFieldException {
        TypeData<?> typeData = TypeData.newInstance(WildCard.class.getDeclaredField("listOfLists"));

        assertEquals(typeData.getType(), List.class);
        List<TypeData<?>> typeParameters = typeData.getTypeParameters();

        typeData = typeParameters.get(0);
        assertEquals(typeData.getType(), List.class);
        typeParameters = typeData.getTypeParameters();

        typeData = typeParameters.get(0);
        assertEquals(typeData.getType(), String.class);

    }

    @Test
    public void testEnums() {
        try {
            typeData(Hotel.class, "type", Type.class);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSubtypes() {
        getMapper().mapPackageFromClass(Subtypes.class);
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
    }

    private static void typeData(Class<?> owner, String fieldName, Class<?> fieldType, Class<?>... parameterTypes)
            throws NoSuchFieldException {
        Field field = owner.getDeclaredField(fieldName);
        TypeData<?> typeData = TypeData.newInstance(field);
        assertEquals(typeData.getType(), fieldType);
        List<TypeData<?>> typeParameters = typeData.getTypeParameters();
        assertEquals(typeParameters.size(), parameterTypes.length);
        for (int i = 0; i < parameterTypes.length; i++) {
            assertEquals(typeParameters.get(i).getType(), parameterTypes[i]);
        }
    }

    private static class WildCard {
        private List<? extends List<String>> listOfLists;
    }
}