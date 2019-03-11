package dev.morphia.mapping.primitives;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MappingException;
import dev.morphia.query.FindOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class CharacterMappingTest extends TestBase {
    @Test
    public void emptyStringToPrimitive() {
        final Characters characters = testMapping("singlePrimitive", "");
        Assert.assertEquals(0, characters.singlePrimitive);
    }

    @Test
    public void emptyStringToPrimitiveArray() {
        final Characters characters = testMapping("primitiveArray", "");
        Assert.assertArrayEquals("".toCharArray(), characters.primitiveArray);
    }

    @Test
    public void emptyStringToWrapper() {
        final Characters characters = testMapping("singleWrapper", "");
        Assert.assertEquals(new Character((char) 0), characters.singleWrapper);
    }

    @Test
    public void emptyStringToWrapperArray() {
        final Characters characters = testMapping("wrapperArray", "");
        compare("", characters.wrapperArray);
    }

    @Test
    public void mapping() throws Exception {
        getMorphia().map(Characters.class);
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

        final Characters loaded = getDs().get(entity);
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

    @Test
    public void singleCharToPrimitive() {
        final Characters characters = testMapping("singlePrimitive", "a");
        Assert.assertEquals('a', characters.singlePrimitive);
    }

    @Test
    public void singleCharToPrimitiveArray() {
        final Characters characters = testMapping("primitiveArray", "a");
        Assert.assertArrayEquals("a".toCharArray(), characters.primitiveArray);
        getDs().save(characters);
    }

    @Test
    public void singleCharToWrapper() {
        final Characters characters = testMapping("singleWrapper", "a");
        Assert.assertEquals(new Character('a'), characters.singleWrapper);
    }

    @Test
    public void singleCharToWrapperArray() {
        final Characters characters = testMapping("wrapperArray", "a");
        compare("a", characters.wrapperArray);
    }

    @Test(expected = MappingException.class)
    public void stringToPrimitive() {
        final Characters characters = testMapping("singlePrimitive", "ab");
    }

    @Test
    public void stringToPrimitiveArray() {
        final Characters characters = testMapping("primitiveArray", "abc");
        Assert.assertArrayEquals("abc".toCharArray(), characters.primitiveArray);
    }

    @Test(expected = MappingException.class)
    public void stringToWrapper() {
        final Characters characters = testMapping("singleWrapper", "ab");
    }

    @Test
    public void stringToWrapperArray() {
        final Characters characters = testMapping("wrapperArray", "abc");
        compare("abc", characters.wrapperArray);
    }

    private void compare(final String abc, final Character[] wrapperArray) {
        Assert.assertEquals(abc.length(), wrapperArray.length);
        for (int i = 0; i < wrapperArray.length; i++) {
            Assert.assertEquals(abc.charAt(i), wrapperArray[i].charValue());
        }
    }

    private Characters testMapping(final String field, final String value) {
        getMorphia().map(Characters.class);

        final DBCollection collection = getDs().getCollection(Characters.class);
        collection.insert(new BasicDBObject(field, value));

        return getDs().find(Characters.class).find(new FindOptions().limit(1)).tryNext();
    }

    public static class Characters {
        @Id
        private ObjectId id;
        private List<Character[]> listWrapperArray = new ArrayList<Character[]>();
        private List<char[]> listPrimitiveArray = new ArrayList<char[]>();
        private List<Character> listWrapper = new ArrayList<Character>();
        private char singlePrimitive;
        private Character singleWrapper;
        private char[] primitiveArray;
        private Character[] wrapperArray;
        private char[][] nestedPrimitiveArray;
        private Character[][] nestedWrapperArray;
    }
}
