package org.mongodb.morphia.issue194;


import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import com.mongodb.MongoException;


public class IndexTest extends TestBase {

  static class E1 {
    @Id
    private ObjectId id;
    @Indexed(name = "NAME", unique = true)
    private String   name;

    public E1() {
    }

    public void setName(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setId(final ObjectId id) {
      this.id = id;
    }

    public ObjectId getId() {
      return id;
    }
  }

  @Entity
  static class E2 {
    @Id
    private ObjectId id;
    @Indexed(name = "NAME", unique = true)
    private String   name;

    public E2() {
    }

    public void setName(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setId(final ObjectId id) {
      this.id = id;
    }

    public ObjectId getId() {
      return id;
    }
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    morphia.map(E1.class);
    morphia.map(E2.class);
    ds.ensureIndexes();
    ds.ensureCaps();
  }

  @Test(expected = MongoException.DuplicateKey.class)
  public void TestDuplicate1() {
    final String name = "J. Doe";

    final E1 ent11 = new E1();
    ent11.setName(name);
    ds.save(ent11);

    final E1 ent12 = new E1();
    ent12.setName(name);
    ds.save(ent12);

  }

  @Test(expected = MongoException.DuplicateKey.class)
  public void TestDuplicate2() {
    final String name = "J. Doe";

    final E2 ent21 = new E2();
    ent21.setName(name);
    ds.save(ent21);

    final E2 ent22 = new E2();
    ent22.setName(name);
    ds.save(ent22);
  }
}
