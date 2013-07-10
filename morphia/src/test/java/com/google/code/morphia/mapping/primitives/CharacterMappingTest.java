package com.google.code.morphia.mapping.primitives;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class CharacterMappingTest extends TestBase {
  public static class Characters {
    @Id ObjectId id;
    List<Character[]> listWrapperArray  = new ArrayList<Character[]>();
    List<char[]> listPrimitiveArray = new ArrayList<char[]>();
    List<Character> listWrapper = new ArrayList<Character>();
    char singlePrimitive;
    Character singleWrapper;
    char[] primitiveArray;
    Character[] wrapperArray;
    char[][] nestedPrimitiveArray;
    Character[][] nestedWrapperArray;
  }

  @Test
  public void mapping() throws Exception {
    morphia.map(Characters.class);
    final Characters entity = new Characters();
    entity.listWrapperArray.add(new Character[] {'1', 'g', '#'});
    entity.listPrimitiveArray.add(new char[] {'1', 'd', 'z'});
    entity.listWrapper.addAll(Arrays.asList('*', ' ', '\u8888'));
    entity.singlePrimitive = 'a';
    entity.singleWrapper = 'b';
    entity.primitiveArray = new char[] {'a', 'b'};
    entity.wrapperArray = new Character[] { 'X', 'y', 'Z'};
    entity.nestedPrimitiveArray = new char[][] {{'5', '-'}, {'a', 'b'}};
    entity.nestedWrapperArray = new Character[][] {{'*', '$', '\u4824'}, { 'X', 'y', 'Z'}};
    ds.save(entity);

    final Characters loaded = ds.get(entity);
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
}
