package com.google.code.morphia.mapping;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class CharacterMappingTest extends TestBase {
  public static class Characters {
    @Id ObjectId id;
    char c;
    Character character;
    char charArray[];
    Character characters[];
    List<char[]> charList;
    List<Character> characterList;
    List<Character[]> characterArrayList;
  }

  @Test
  public void mapping() throws Exception {
    morphia.map(Characters.class);
    final Characters entity = new Characters();
    entity.c = 'a';
    entity.character = 'b';
    entity.charArray = new char[] {'a', 'b'};
    entity.characters = new Character[] { 'X', 'y', 'Z'};
    entity.charList = new ArrayList<char[]>(Arrays.asList(new char[] {'1', 'd', 'z'}));
    ds.save(entity);

    final Characters loaded = ds.get(entity);
    Assert.assertNotNull(loaded.id);
    Assert.assertEquals(entity.c, loaded.c);
    Assert.assertEquals(entity.character, loaded.character);
    Assert.assertArrayEquals(entity.charArray, loaded.charArray);
    Assert.assertArrayEquals(entity.characters, loaded.characters);
  }

  @Test
  public void legacyStrings() {
    morphia.map(Characters.class);

    final DBCollection collection = ds.getCollection(Characters.class);
    collection.insert(new BasicDBObject("charArray", "bob"));

    final Characters characters = ds.find(Characters.class).get();
    Assert.assertArrayEquals("bob".toCharArray(), characters.charArray);
    ds.save(characters, WriteConcern.FSYNCED);

    final DBObject one = collection.findOne();
    final Object charArray = one.get("charArray");
    Assert.assertTrue(charArray instanceof List);

    final List list = (List) charArray;
    Assert.assertEquals("b", list.get(0));
    Assert.assertEquals("o", list.get(1));
    Assert.assertEquals("b", list.get(2));
  }
}
