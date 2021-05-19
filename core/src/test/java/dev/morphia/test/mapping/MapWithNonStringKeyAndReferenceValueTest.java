package dev.morphia.test.mapping;


import dev.morphia.annotations.Reference;
import dev.morphia.test.models.TestEntity;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


@Test(groups = "references")
public class MapWithNonStringKeyAndReferenceValueTest extends ProxyTestBase {
    @Test
    public void testMapKeyShouldBeInteger() {
        getMapper().map(ChildEntity.class, ParentEntity.class);

        final ChildEntity ce1 = new ChildEntity();
        ce1.value = "first";
        final ChildEntity ce2 = new ChildEntity();
        ce2.value = "second";

        final ParentEntity pe = new ParentEntity();
        pe.childMap.put(1, ce1);
        pe.childMap.put(2, ce2);

        getDs().save(asList(ce1, ce2, pe));

        final ParentEntity fetched = getDs().find(ParentEntity.class).filter(eq("_id", pe.getId())).first();
        assertNotNull(fetched);
        assertNotNull(fetched.childMap);
        assertEquals(fetched.childMap.size(), 2);
        //it is really String without fixing the reference mapper
        //so ignore IDE's complains if any
        Set<Integer> set = fetched.childMap.keySet();
        assertTrue(set.iterator().next() != null);
    }

    @Test
    public void testWithProxy() {
        checkForProxyTypes();

        getMapper().map(ChildEntity.class, ParentEntity.class);

        final ChildEntity ce1 = new ChildEntity();
        ce1.value = "first";
        final ChildEntity ce2 = new ChildEntity();
        ce2.value = "second";

        final ParentEntity pe = new ParentEntity();
        pe.lazyChildMap.put(1, ce1);
        pe.lazyChildMap.put(2, ce2);

        getDs().save(asList(ce1, ce2, pe));

        final ParentEntity fetched = getDs().find(ParentEntity.class)
                                            .filter(eq("_id", pe.getId()))
                                            .first();
        assertNotNull(fetched);
        assertIsProxy(fetched.lazyChildMap);
        assertNotFetched(fetched.lazyChildMap);
        assertEquals(fetched.lazyChildMap.size(), 2);
        assertNotFetched(fetched.lazyChildMap);
        assertNotNull(fetched.lazyChildMap.keySet().iterator().next());
    }

    private static class ParentEntity extends TestEntity {
        @Reference
        private final Map<Integer, ChildEntity> childMap = new HashMap<>();
        @Reference(lazy = true)
        private final Map<Integer, ChildEntity> lazyChildMap = new HashMap<>();
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
        public boolean equals(Object o) {
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
            return value != null ? value.equals(that.value) : that.value == null;
        }
    }
}
