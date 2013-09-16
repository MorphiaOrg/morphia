package org.mongodb.morphia.issueA;


import java.io.Serializable;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;
import com.mongodb.DBObject;


/**
 * Test from email to mongodb-users list.
 */
public class TestMapping extends TestBase {

  @Test
  public void testMapping() {
    morphia.map(Class_level_three.class);
    final Class_level_three sp = new Class_level_three();

    //Old way
    final DBObject wrapObj = morphia.toDBObject(sp);  //the error points here from the user
    ds.getDB().getCollection("testColl").save(wrapObj);


    //better way
    ds.save(sp);

  }

  private interface Interface_one<K> {
    K getK();
  }

  private static class Class_level_one<K> implements Interface_one<K>, Cloneable, Serializable {
    K k;

    public K getK() {
      return k;
    }
  }

  private static class Class_level_two extends Class_level_one<String> {

  }

  private static class Class_level_three {
    @Id
    private ObjectId id;

    private String name;

    @Embedded
    private Class_level_two value;
  }


}
