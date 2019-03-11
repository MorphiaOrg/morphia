package dev.morphia.mapping;


import org.junit.Assert;
import org.junit.Test;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.lazy.LazyFeatureDependencies;
import dev.morphia.mapping.lazy.ProxyTestBase;
import dev.morphia.testutil.TestEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;


public class MapWithNonStringKeyAndReferenceValueTest extends ProxyTestBase {
    @Test
    public void testMapKeyShouldBeInteger() throws Exception {
        getMorphia().map(ChildEntity.class, ParentEntity.class);

        final ChildEntity ce1 = new ChildEntity();
        ce1.value = "first";
        final ChildEntity ce2 = new ChildEntity();
        ce2.value = "second";

        final ParentEntity pe = new ParentEntity();
        pe.childMap.put(1, ce1);
        pe.childMap.put(2, ce2);

        getDs().save(asList(ce1, ce2, pe));

        final ParentEntity fetched = getDs().get(ParentEntity.class, pe.getId());
        Assert.assertNotNull(fetched);
        Assert.assertNotNull(fetched.childMap);
        Assert.assertEquals(2, fetched.childMap.size());
        //it is really String without fixing the reference mapper
        //so ignore IDE's complains if any
        Set<Integer> set = fetched.childMap.keySet();
        Assert.assertTrue(set.iterator().next() != null);
    }

    @Test
    public void testWithProxy() throws Exception {
        if (!LazyFeatureDependencies.assertDependencyFullFilled()) {
            return;
        }
        getMorphia().map(ChildEntity.class, ParentEntity.class);

        final ChildEntity ce1 = new ChildEntity();
        ce1.value = "first";
        final ChildEntity ce2 = new ChildEntity();
        ce2.value = "second";

        final ParentEntity pe = new ParentEntity();
        pe.lazyChildMap.put(1, ce1);
        pe.lazyChildMap.put(2, ce2);

        getDs().save(asList(ce1, ce2, pe));

        final ParentEntity fetched = getDs().get(ParentEntity.class, pe.getId());
        Assert.assertNotNull(fetched);
        assertIsProxy(fetched.lazyChildMap);
        assertNotFetched(fetched.lazyChildMap);
        Assert.assertEquals(2, fetched.lazyChildMap.size());
        assertFetched(fetched.lazyChildMap);
        //it is really String without fixing the reference mapper
        //so ignore IDE's complains if any
        Assert.assertTrue(fetched.lazyChildMap.keySet().iterator().next() != null);
    }

    private static class ParentEntity extends TestEntity {
        @Reference
        private Map<Integer, ChildEntity> childMap = new HashMap<Integer, ChildEntity>();
        @Reference(lazy = true)
        private Map<Integer, ChildEntity> lazyChildMap = new HashMap<Integer, ChildEntity>();
    }

    private static class ChildEntity extends TestEntity {
        private String value;

        @Override
        public int hashCode() {
            int result = getId() != null ? getId().hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ChildEntity that = (ChildEntity) o;

            if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) {
                return false;
            }
            if (value != null ? !value.equals(that.value) : that.value != null) {
                return false;
            }

            return true;
        }
    }
}
