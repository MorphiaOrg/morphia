package org.mongodb.morphia.mapping;


import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.junit.Assert;


/**
 * @author scotthernandez
 */
public class ReferencesWIgnoreMissingTests extends TestBase {
  @Entity
  static class Container {
    @Id
    public ObjectId id;
    @Reference(ignoreMissing = true)
    private StringHolder[] refs;
  }

  @Entity
  static class StringHolder {
    @Id ObjectId id = new ObjectId();
  }

  @Test
  public void TestMissingReference() throws Exception {
    final Container c = new Container();
    c.refs = new StringHolder[] { new StringHolder(), new StringHolder() };
    ds.save(c);
    ds.save(c.refs[0]);

    Container reloadedContainer = ds.find(Container.class).get();
    Assert.assertNotNull(reloadedContainer);
    Assert.assertNotNull(reloadedContainer.refs);
    Assert.assertEquals(1, reloadedContainer.refs.length);

    reloadedContainer = ds.get(c);
    Assert.assertNotNull(reloadedContainer);
    Assert.assertNotNull(reloadedContainer.refs);
    Assert.assertEquals(1, reloadedContainer.refs.length);

    final List<Container> cs = ds.find(Container.class).asList();
    Assert.assertNotNull(cs);
    Assert.assertEquals(1, cs.size());

  }
}
