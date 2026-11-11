package dev.morphia.test.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.Reference;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Entities that store their discriminator under their own key, {@code @Entity(discriminatorKey = ...)}, where nothing
 * else says what type a stored document is: untyped values, and ids stored as documents.
 */
public class TestOwnDiscriminatorKey extends TestBase {

    /**
     * An entity held by an {@code Object}, or in an untyped list or map, loads back as itself rather than as a
     * {@code Document}, as it does when its discriminator is under the configured key.
     */
    @Test
    public void untypedValuesLoadAsTheirEntity() {
        getMapper().map(OwnKeyHolder.class, OwnKeyKind.class, OwnKeyPlain.class);
        OwnKeyHolder holder = new OwnKeyHolder();
        holder.kind = new OwnKeyKind("a");
        holder.plain = new OwnKeyPlain("p");
        holder.list = new ArrayList<>(List.of(new OwnKeyKind("b"), new OwnKeyPlain("q")));
        holder.map = new LinkedHashMap<>(Map.of("k", new OwnKeyKind("c")));
        getDs().save(holder);

        OwnKeyHolder loaded = getDs().find(OwnKeyHolder.class).first();
        Assertions.assertEquals(new OwnKeyPlain("p"), loaded.plain, "under the configured key");
        Assertions.assertEquals(new OwnKeyKind("a"), loaded.kind);
        Assertions.assertEquals(List.of(new OwnKeyKind("b"), new OwnKeyPlain("q")), loaded.list);
        Assertions.assertEquals(Map.of("k", new OwnKeyKind("c")), loaded.map);
    }

    /**
     * A plain document with a field named like an entity's own key stays a document unless the field holds that entity's
     * discriminator.
     */
    @Test
    public void documentsWithAFieldOfThatNameStayDocuments() {
        getMapper().map(OwnKeyHolder.class, OwnKeyKind.class, OwnKeyPlain.class);
        Document notADiscriminator = new Document("_kind", "something else").append("name", "x");
        Document notAString = new Document("_kind", 5).append("name", "y");
        Document wrongKey = new Document("_kind", OwnKeyPlain.class.getName()).append("name", "z");
        OwnKeyHolder holder = new OwnKeyHolder();
        holder.list = new ArrayList<>(List.of(notADiscriminator, notAString, wrongKey));
        getDs().save(holder);

        OwnKeyHolder loaded = getDs().find(OwnKeyHolder.class).first();
        Assertions.assertEquals(List.of(notADiscriminator, notAString, wrongKey), loaded.list);
    }

    /**
     * References to an entity whose id is an entity with its own key resolve, however they're stored.
     */
    @Test
    public void referencesByAnIdWithItsOwnKey() {
        getMapper().map(OwnKeyOwner.class, OwnKeyTarget.class, OwnKeyKind.class);
        OwnKeyTarget target = new OwnKeyTarget();
        target.id = new OwnKeyKind("t");
        target.value = "v";
        getDs().save(target);
        OwnKeyOwner owner = new OwnKeyOwner();
        owner.eager = target;
        owner.lazy = target;
        owner.idOnly = target;
        owner.many = new ArrayList<>(List.of(target));
        getDs().save(owner);

        OwnKeyOwner loaded = getDs().find(OwnKeyOwner.class).first();
        Assertions.assertEquals(new OwnKeyKind("t"), loaded.eager.id);
        Assertions.assertEquals("v", loaded.eager.value);
        Assertions.assertEquals("v", loaded.lazy.getValue());
        Assertions.assertEquals("v", loaded.idOnly.value);
        Assertions.assertEquals("v", loaded.many.get(0).value);
    }

    /**
     * An entity with load callbacks whose id is an entity with its own key and callbacks of its own: the id isn't read
     * ahead as an entity, which would run its callbacks an extra time.
     */
    @Test
    public void anIdWithItsOwnKeyLoadsOnce() {
        getMapper().map(OwnKeyLoaded.class, OwnKeyLoadedId.class);
        OwnKeyLoaded entity = new OwnKeyLoaded();
        entity.id = new OwnKeyLoadedId("l");
        getDs().save(entity);
        OwnKeyLoadedId.loads = 0;

        OwnKeyLoaded loaded = getDs().find(OwnKeyLoaded.class).first();
        Assertions.assertEquals(new OwnKeyLoadedId("l"), loaded.id);
        Assertions.assertEquals(1, OwnKeyLoadedId.loads, "@PostLoad runs on the id");
    }

    /**
     * The ids below are stored as documents with their own discriminator key. The decode session and references both read
     * those as plain documents, as before, so they agree on the key an entity is in flight under however the owner is
     * decoded.
     */
    @Test
    public void aLifecycleOwnerClosesACycleThroughAnIdWithItsOwnKey() {
        getMapper().map(OwnKeyLifecycleCycle.class, OwnKeyKind.class);
        OwnKeyLifecycleCycle cycle = new OwnKeyLifecycleCycle();
        cycle.id = new OwnKeyKind("loop");
        cycle.self = cycle;
        getDs().save(cycle);

        OwnKeyLifecycleCycle loaded = getDs().find(OwnKeyLifecycleCycle.class).first();
        Assertions.assertEquals(new OwnKeyKind("loop"), loaded.id);
        Assertions.assertSame(loaded, loaded.self, "the cycle closes on the entity being decoded");
    }

    @Test
    public void aMapOfReferencesClosesACycleThroughAnIdWithItsOwnKey() {
        getMapper().map(OwnKeyMapCycle.class, OwnKeyKind.class);
        OwnKeyMapCycle cycle = new OwnKeyMapCycle();
        cycle.id = new OwnKeyKind("map");
        cycle.others = new LinkedHashMap<>(Map.of("me", cycle));
        getDs().save(cycle);

        OwnKeyMapCycle loaded = getDs().find(OwnKeyMapCycle.class).first();
        Assertions.assertSame(loaded, loaded.others.get("me"), "the cycle closes on the entity being decoded");
    }

    @Test
    public void anIdWithItsOwnKeyLoadsOnceUnderAnOwnerWithoutCallbacks() {
        getMapper().map(OwnKeyPlainOwner.class, OwnKeyLoadedId.class);
        OwnKeyPlainOwner entity = new OwnKeyPlainOwner();
        entity.id = new OwnKeyLoadedId("p");
        getDs().save(entity);
        OwnKeyLoadedId.loads = 0;

        OwnKeyPlainOwner loaded = getDs().find(OwnKeyPlainOwner.class).first();
        Assertions.assertEquals(new OwnKeyLoadedId("p"), loaded.id);
        Assertions.assertEquals(1, OwnKeyLoadedId.loads, "@PostLoad runs on the id");
    }

    @Entity("ownKey_lifecycleCycles")
    public static class OwnKeyLifecycleCycle {
        @Id
        private OwnKeyKind id;
        @Reference(idOnly = true)
        private OwnKeyLifecycleCycle self;

        @PostLoad
        void loaded() {
        }
    }

    @Entity("ownKey_mapCycles")
    public static class OwnKeyMapCycle {
        @Id
        private OwnKeyKind id;
        @Reference(idOnly = true)
        private Map<String, OwnKeyMapCycle> others;
    }

    @Entity("ownKey_plainOwners")
    public static class OwnKeyPlainOwner {
        @Id
        private OwnKeyLoadedId id;
    }

    @Entity("ownKey_holders")
    public static class OwnKeyHolder {
        @Id
        private ObjectId id;
        private Object kind;
        private Object plain;
        private List<Object> list;
        private Map<String, Object> map;
    }

    @Entity(discriminatorKey = "_kind")
    public static class OwnKeyKind {
        private String name;

        public OwnKeyKind() {
        }

        OwnKeyKind(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof OwnKeyKind kind && Objects.equals(name, kind.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        @Override
        public String toString() {
            return "OwnKeyKind(" + name + ")";
        }
    }

    @Entity
    public static class OwnKeyPlain {
        private String name;

        public OwnKeyPlain() {
        }

        OwnKeyPlain(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof OwnKeyPlain plain && Objects.equals(name, plain.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        @Override
        public String toString() {
            return "OwnKeyPlain(" + name + ")";
        }
    }

    @Entity("ownKey_targets")
    public static class OwnKeyTarget {
        @Id
        private OwnKeyKind id;
        private String value;

        public String getValue() {
            return value;
        }
    }

    @Entity("ownKey_owners")
    public static class OwnKeyOwner {
        @Id
        private ObjectId id;
        @Reference
        private OwnKeyTarget eager;
        @Reference(lazy = true)
        private OwnKeyTarget lazy;
        @Reference(idOnly = true)
        private OwnKeyTarget idOnly;
        @Reference
        private List<OwnKeyTarget> many;
    }

    @Entity("ownKey_loaded")
    public static class OwnKeyLoaded {
        @Id
        private OwnKeyLoadedId id;

        @PostLoad
        void loaded() {
        }
    }

    @Entity(discriminatorKey = "_lid")
    public static class OwnKeyLoadedId {
        static int loads;

        private String name;

        public OwnKeyLoadedId() {
        }

        OwnKeyLoadedId(String name) {
            this.name = name;
        }

        @PostLoad
        void loaded() {
            loads++;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof OwnKeyLoadedId other && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }
}
