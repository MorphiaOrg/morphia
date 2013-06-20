package com.google.code.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import junit.framework.Assert;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ByteArrayMappingTest extends TestBase {
  private static class ContainsByteArray {
    @Id ObjectId id;
    Byte[] ba;
  }


  @Test
  public void testCharMapping() throws Exception {
    morphia.map(ContainsByteArray.class);
    final ContainsByteArray entity = new ContainsByteArray();
    final Byte[] test = new Byte[] { 6, 9, 1, -122 };
    entity.ba = test;
    ds.save(entity);
    final ContainsByteArray loaded = ds.get(entity);

    for (int i = 0; i < test.length; i++) {
      final Byte c = test[i];
      Assert.assertEquals(c, entity.ba[i]);
    }
    Assert.assertNotNull(loaded.id);
  }


}
