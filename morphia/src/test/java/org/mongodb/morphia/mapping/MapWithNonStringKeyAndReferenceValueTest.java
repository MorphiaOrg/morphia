package org.mongodb.morphia.mapping;


import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.lazy.LazyFeatureDependencies;
import org.mongodb.morphia.mapping.lazy.ProxyTestBase;
import org.mongodb.morphia.testutil.TestEntity;


public class MapWithNonStringKeyAndReferenceValueTest extends ProxyTestBase {
  @Test
  public void testMapKeyShouldBeInteger() throws Exception {
    morphia.map(ChildEntity.class, ParentEntity.class);

    final ChildEntity ce1 = new ChildEntity();
    ce1.value = "first";
    final ChildEntity ce2 = new ChildEntity();
    ce2.value = "second";

    final ParentEntity pe = new ParentEntity();
    pe.childMap.put(1, ce1);
    pe.childMap.put(2, ce2);

    ds.save(ce1, ce2, pe);

    final ParentEntity fetched = ds.get(ParentEntity.class, pe.getId());
    Assert.assertNotNull(fetched);
    Assert.assertNotNull(fetched.childMap);
    Assert.assertEquals(2, fetched.childMap.size());
    //it is really String without fixing the reference mapper
    //so ignore IDE's complains if any
    Assert.assertTrue(fetched.childMap.keySet().iterator().next() instanceof Integer);
  }

  @Test
  public void testWithProxy() throws Exception {
    if (!LazyFeatureDependencies.assertDependencyFullFilled()) {
      return;
    }
    morphia.map(ChildEntity.class, ParentEntity.class);

    final ChildEntity ce1 = new ChildEntity();
    ce1.value = "first";
    final ChildEntity ce2 = new ChildEntity();
    ce2.value = "second";

    final ParentEntity pe = new ParentEntity();
    pe.lazyChildMap.put(1, ce1);
    pe.lazyChildMap.put(2, ce2);

    ds.save(ce1, ce2, pe);

    final ParentEntity fetched = ds.get(ParentEntity.class, pe.getId());
    Assert.assertNotNull(fetched);
    assertIsProxy(fetched.lazyChildMap);
    assertNotFetched(fetched.lazyChildMap);
    Assert.assertEquals(2, fetched.lazyChildMap.size());
    assertFetched(fetched.lazyChildMap);
    //it is really String without fixing the reference mapper
    //so ignore IDE's complains if any
    Assert.assertTrue(fetched.lazyChildMap.keySet().iterator().next() instanceof Integer);
  }

  private static class ParentEntity extends TestEntity {
    @Reference
    Map<Integer, ChildEntity> childMap = new HashMap<Integer, ChildEntity>();
    @Reference(lazy = true)
    Map<Integer, ChildEntity> lazyChildMap = new HashMap<Integer, ChildEntity>();
  }

  private static class ChildEntity extends TestEntity {
    String value;

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final ChildEntity that = (ChildEntity) o;

      if (id != null ? !id.equals(that.id) : that.id != null) {
        return false;
      }
      if (value != null ? !value.equals(that.value) : that.value != null) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = id != null ? id.hashCode() : 0;
      result = 31 * result + (value != null ? value.hashCode() : 0);
      return result;
    }
  }
}
