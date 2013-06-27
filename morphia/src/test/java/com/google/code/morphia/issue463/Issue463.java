package com.google.code.morphia.issue463;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.mongodb.BasicDBObject;


public class Issue463 extends TestBase {
  @Entity(value = "class1", noClassnameStored = true)
  public static class Class1 {
    @Id
    public ObjectId id;
    public String text;
  }

  @Entity(value = "class2", noClassnameStored = true)
  public static class Class2 extends Class1 {

  }

  @Test
  public void save() {
    morphia.map(Class1.class, Class2.class);

    final Class2 class2 = new Class2();
    class2.id = new ObjectId();
    class2.text = "hello world";
    ds.save(class2);

    final BasicDBObject query = new BasicDBObject("_id", class2.id);
    Assert.assertFalse(ds.getCollection(Class1.class).find(query).hasNext());
    Assert.assertTrue(ds.getCollection(Class2.class).find(query).hasNext());
  }
}
