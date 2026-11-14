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
        Holder loaded = saveAndLoad();

        assertResolved(loaded.eager);
    }

    @Test
    public void lazy() {
        checkForProxyTypes();

        Holder loaded = saveAndLoad();

        assertIsProxy(loaded.lazy);
        assertNotFetched(loaded.lazy);
        assertResolved(loaded.lazy);
    }

    private Holder saveAndLoad() {
        getMapper().map(Base.class, SubA.class, SubB.class, Holder.class);

        SubA subA = new SubA();
        subA.id = 100;
        SubB subB = new SubB();
        subB.id = 100;

        Holder holder = new Holder();
        holder.id = 100;
        holder.eager.put(SubA.class, subA);
        holder.eager.put(SubB.class, subB);
        holder.lazy.put(SubA.class, subA);
        holder.lazy.put(SubB.class, subB);

        getDs().save(List.of(subA, subB, holder));

        Assertions.assertNotEquals(getMapper().getEntityModel(SubA.class).collectionName(),
                getMapper().getEntityModel(SubB.class).collectionName(),
                "The referenced types must live in different collections for this test to be meaningful");

        Holder loaded = getDs().find(Holder.class)
                .filter(eq("_id", holder.id))
                .first();
        Assertions.assertNotNull(loaded);
        return loaded;
    }

    private void assertResolved(Map<Class<? extends Base>, Base> map) {
        Assertions.assertEquals(2, map.size());
        for (Map.Entry<Class<? extends Base>, Base> entry : map.entrySet()) {
            Assertions.assertInstanceOf(Class.class, entry.getKey());
            Assertions.assertEquals(entry.getKey(), entry.getValue().getClass());
            Assertions.assertEquals(100, entry.getValue().id);
        }
    }

    @Entity
    private static class Base {
        @Id
        protected long id;
    }

    @Entity
    private static class SubA extends Base {
    }

    @Entity
    private static class SubB extends Base {
    }

    @Entity
    private static class Holder {
        @Id
        private long id;
        @Reference
        private final Map<Class<? extends Base>, Base> eager = new HashMap<>();
        @Reference(lazy = true)
        private final Map<Class<? extends Base>, Base> lazy = new HashMap<>();
    }
}
