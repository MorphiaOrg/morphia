package dev.morphia.test.mapping.primitives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

public class CharacterMappingTest extends TestBase {
    @Test
    public void mapping() {
        getMapper().map(Characters.class);
        final Characters entity = new Characters();
        entity.listWrapperArray.add(new Character[] { '1', 'g', '#' });
        entity.listPrimitiveArray.add(new char[] { '1', 'd', 'z' });
        entity.listWrapper.addAll(Arrays.asList('*', ' ', '\u8888'));
        entity.singlePrimitive = 'a';
        entity.singleWrapper = 'b';
        entity.primitiveArray = new char[] { 'a', 'b' };
        entity.wrapperArray = new Character[] { 'X', 'y', 'Z' };
        entity.nestedPrimitiveArray = new char[][] { { '5', '-' }, { 'a', 'b' } };
        entity.nestedWrapperArray = new Character[][] { { '*', '$', '\u4824' }, { 'X', 'y', 'Z' } };
        getDs().save(entity);

        final Characters loaded = getDs().find(Characters.class)
                .filter(eq("_id", entity.id))
                .first();
        Assertions.assertNotNull(loaded.id);
        Assertions.assertArrayEquals(entity.listWrapperArray.get(0), loaded.listWrapperArray.get(0));
        Assertions.assertArrayEquals(entity.listPrimitiveArray.get(0), loaded.listPrimitiveArray.get(0));
        Assertions.assertEquals(entity.listWrapper, loaded.listWrapper);
        Assertions.assertEquals(entity.singlePrimitive, loaded.singlePrimitive);
        Assertions.assertEquals(entity.singleWrapper, loaded.singleWrapper);
        Assertions.assertArrayEquals(entity.primitiveArray, loaded.primitiveArray);
        Assertions.assertArrayEquals(entity.wrapperArray, loaded.wrapperArray);
        Assertions.assertArrayEquals(entity.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
        Assertions.assertArrayEquals(entity.nestedWrapperArray, loaded.nestedWrapperArray);
    }

    @Entity
    public static class Characters {
        private final List<Character[]> listWrapperArray = new ArrayList<>();
        private final List<char[]> listPrimitiveArray = new ArrayList<>();
        private final List<Character> listWrapper = new ArrayList<>();
        @Id
        private ObjectId id;
        private char singlePrimitive;
        private Character singleWrapper;
        private char[] primitiveArray;
        private Character[] wrapperArray;
        private char[][] nestedPrimitiveArray;
        private Character[][] nestedWrapperArray;
    }
}
