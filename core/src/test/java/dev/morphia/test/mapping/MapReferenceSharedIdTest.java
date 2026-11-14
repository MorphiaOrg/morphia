package dev.morphia.test.mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

/**
 * Map values referencing entities in different collections that share the same {@code _id} must each resolve from their own
 * collection.
 *
 * @see <a href="https://github.com/MorphiaOrg/morphia/issues/2881">#2881</a>
 * @see <a href="https://github.com/MorphiaOrg/morphia/issues/2996">#2996</a>
 */
@Tag("references")
public class MapReferenceSharedIdTest extends ProxyTestBase {
    @Test
    public void eager() {
        SharedIdHolder loaded = saveAndLoad();

        assertResolved(loaded.eager);
    }

    @Test
    public void lazy() {
        checkForProxyTypes();

        SharedIdHolder loaded = saveAndLoad();

        assertIsProxy(loaded.lazy);
        assertNotFetched(loaded.lazy);
        assertResolved(loaded.lazy);
    }

    private SharedIdHolder saveAndLoad() {
        getMapper().map(SharedIdBase.class, SharedIdSubA.class, SharedIdSubB.class, SharedIdHolder.class);

        SharedIdSubA subA = new SharedIdSubA();
        subA.id = 100;
        SharedIdSubB subB = new SharedIdSubB();
        subB.id = 100;

        SharedIdHolder holder = new SharedIdHolder();
        holder.id = 100;
        holder.eager.put(SharedIdSubA.class, subA);
        holder.eager.put(SharedIdSubB.class, subB);
        holder.lazy.put(SharedIdSubA.class, subA);
        holder.lazy.put(SharedIdSubB.class, subB);

        getDs().save(List.of(subA, subB, holder));

        Assertions.assertNotEquals(getMapper().getEntityModel(SharedIdSubA.class).collectionName(),
                getMapper().getEntityModel(SharedIdSubB.class).collectionName(),
                "The referenced types must live in different collections for this test to be meaningful");

        SharedIdHolder loaded = getDs().find(SharedIdHolder.class)
                .filter(eq("_id", holder.id))
                .first();
        Assertions.assertNotNull(loaded);
        return loaded;
    }

    private void assertResolved(Map<Class<? extends SharedIdBase>, SharedIdBase> map) {
        Assertions.assertEquals(2, map.size());
        for (Map.Entry<Class<? extends SharedIdBase>, SharedIdBase> entry : map.entrySet()) {
            Assertions.assertInstanceOf(Class.class, entry.getKey());
            Assertions.assertEquals(entry.getKey(), entry.getValue().getClass());
            Assertions.assertEquals(100, entry.getValue().id);
        }
    }

    @Entity
    private static class SharedIdBase {
        @Id
        protected long id;
    }

    @Entity
    private static class SharedIdSubA extends SharedIdBase {
    }

    @Entity
    private static class SharedIdSubB extends SharedIdBase {
    }

    @Entity
    private static class SharedIdHolder {
        @Id
        private long id;
        @Reference
        private Map<Class<? extends SharedIdBase>, SharedIdBase> eager = new HashMap<>();
        @Reference(lazy = true)
        private Map<Class<? extends SharedIdBase>, SharedIdBase> lazy = new HashMap<>();
    }
}
