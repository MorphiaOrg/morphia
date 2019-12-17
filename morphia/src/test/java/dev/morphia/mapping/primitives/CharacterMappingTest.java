package dev.morphia.mapping.primitives;


import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CharacterMappingTest extends TestBase {
    @Test
    public void mapping() {
        getMapper().map(Characters.class);
        final Characters entity = new Characters();
        entity.listWrapperArray.add(new Character[]{'1', 'g', '#'});
        entity.listPrimitiveArray.add(new char[]{'1', 'd', 'z'});
        entity.listWrapper.addAll(Arrays.asList('*', ' ', '\u8888'));
        entity.singlePrimitive = 'a';
        entity.singleWrapper = 'b';
        entity.primitiveArray = new char[]{'a', 'b'};
        entity.wrapperArray = new Character[]{'X', 'y', 'Z'};
        entity.nestedPrimitiveArray = new char[][]{{'5', '-'}, {'a', 'b'}};
        entity.nestedWrapperArray = new Character[][]{{'*', '$', '\u4824'}, {'X', 'y', 'Z'}};
        getDs().save(entity);

        final Characters loaded = getDs().find(Characters.class)
                                         .filter("_id", entity.id)
                                         .first();
        Assert.assertNotNull(loaded.id);
        Assert.assertArrayEquals(entity.listWrapperArray.get(0), loaded.listWrapperArray.get(0));
        Assert.assertArrayEquals(entity.listPrimitiveArray.get(0), loaded.listPrimitiveArray.get(0));
        Assert.assertEquals(entity.listWrapper, loaded.listWrapper);
        Assert.assertEquals(entity.singlePrimitive, loaded.singlePrimitive);
        Assert.assertEquals(entity.singleWrapper, loaded.singleWrapper);
        Assert.assertArrayEquals(entity.primitiveArray, loaded.primitiveArray);
        Assert.assertArrayEquals(entity.wrapperArray, loaded.wrapperArray);
        Assert.assertArrayEquals(entity.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
        Assert.assertArrayEquals(entity.nestedWrapperArray, loaded.nestedWrapperArray);
    }

    @Entity
    public static class Characters {
        @Id
        private ObjectId id;
        private List<Character[]> listWrapperArray = new ArrayList<>();
        private List<char[]> listPrimitiveArray = new ArrayList<>();
        private List<Character> listWrapper = new ArrayList<>();
        private char singlePrimitive;
        private Character singleWrapper;
        private char[] primitiveArray;
        private Character[] wrapperArray;
        private char[][] nestedPrimitiveArray;
        private Character[][] nestedWrapperArray;
    }
}
