package dev.morphia;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.generics.model.Child;
import dev.morphia.generics.model.ChildEntity;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateException;
import dev.morphia.testmodel.Address;
import dev.morphia.testmodel.Hotel;
import dev.morphia.testmodel.Rectangle;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.in;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestDatastore extends TestBase {

    @Test(expected = UpdateException.class)
    public void saveNull() {
        getDs().save((Hotel) null);
    }

    @Test
    public void shouldSaveGenericTypeVariables() {
        // given
        ChildEntity child = new ChildEntity();
        child.setEmbeddedList(singletonList(new Child()));

        // when
        getDs().save(child);

        // then
        assertNotNull(child.getId());
    }

    @Test
    public void testCollectionNames() {
        assertEquals("facebook_users", getMapper().getEntityModel(FacebookUser.class).getCollectionName());
    }


    @Test
    public void testDoesNotExistAfterDelete() {
        // given
        long id = System.currentTimeMillis();
        final long key = getDs().save(new FacebookUser(id, "user 1")).getId();

        // when
        getDs().find(FacebookUser.class).findAndDelete();

        // then
        assertNull("Shouldn't exist after delete", getDs().find(FacebookUser.class)
                                                          .filter(eq("_id", key))
                                                          .first());
    }

    @Test
    public void testEmbedded() {
        getDs().find(Hotel.class).findAndDelete();
        final Hotel borg = new Hotel();
        borg.setName("Hotel Borg");
        borg.setStars(4);
        borg.setTakesCreditCards(true);
        borg.setStartDate(new Date());
        borg.setType(Hotel.Type.LEISURE);
        final Address address = new Address();
        address.setStreet("Posthusstraeti 11");
        address.setPostCode("101");
        borg.setAddress(address);


        getDs().save(borg);
        assertEquals(1, getDs().find(Hotel.class).count());
        assertNotNull(borg.getId());

        final Hotel hotelLoaded = getDs().find(Hotel.class)
                                         .filter(eq("_id", borg.getId()))
                                         .first();
        assertEquals(borg.getName(), hotelLoaded.getName());
        assertEquals(borg.getAddress().getPostCode(), hotelLoaded.getAddress().getPostCode());
    }


    @Test
    public void testFindAndDeleteWithNoQueryMatch() {
        assertNull(getDs().find(FacebookUser.class)
                          .filter(eq("username", "David S. Pumpkins"))
                          .findAndDelete());
    }

    @Test
    public void testIdUpdatedOnSave() {
        final Rectangle rect = new Rectangle(10, 10);
        getDs().save(rect);
        assertNotNull(rect.getId());
    }

    @Test
    public void testLifecycle() {
        final LifecycleTestObj life1 = new LifecycleTestObj();
        getMapper().map(List.of(LifecycleTestObj.class));
        getDs().save(life1);
        assertTrue(LifecycleListener.foundDatastore);
        assertTrue(life1.prePersist);
        assertTrue(life1.prePersistWithParam);
        assertTrue(life1.prePersistWithParamAndReturn);
        assertTrue(life1.postPersist);
        assertTrue(life1.postPersistWithParam);

        final Datastore datastore = getDs();

        final LifecycleTestObj loaded = datastore.find(LifecycleTestObj.class)
                                                 .filter(eq("_id", life1.id))
                                                 .first();
        assertTrue(loaded.preLoad);
        assertTrue(loaded.preLoadWithParam);
        assertTrue(loaded.postLoad);
        assertTrue(loaded.postLoadWithParam);
    }

    @Test
    public void testLifecycleListeners() {
        final LifecycleTestObj life1 = new LifecycleTestObj();
        getMapper().map(List.of(LifecycleTestObj.class));
        getDs().save(life1);
        assertTrue(LifecycleListener.prePersist);
        assertTrue(LifecycleListener.prePersistWithEntity);
    }

    @Test
    public void testMorphiaDS() {
        Morphia.createDatastore(getMongoClient(), "test");
    }

    @Test
    public void testMultipleDatabasesSingleThreaded() {
        getMapper().map(List.of(FacebookUser.class));

        final Datastore ds1 = Morphia.createDatastore(getMongoClient(), "db1");
        final Datastore ds2 = Morphia.createDatastore(getMongoClient(), "db2");

        final FacebookUser db1Friend = new FacebookUser(3, "DB1 FaceBook Friend");
        ds1.save(db1Friend);
        final FacebookUser db1User = new FacebookUser(1, "DB1 FaceBook User");
        db1User.friends.add(db1Friend);
        ds1.save(db1User);

        final FacebookUser db2Friend = new FacebookUser(4, "DB2 FaceBook Friend");
        ds2.save(db2Friend);
        final FacebookUser db2User = new FacebookUser(2, "DB2 FaceBook User");
        db2User.friends.add(db2Friend);
        ds2.save(db2User);

        testFirstDatastore(ds1);
        testSecondDatastore(ds2);

        testFirstDatastore(ds1);
        testSecondDatastore(ds2);

        testFirstDatastore(ds1);
        testSecondDatastore(ds2);

        testFirstDatastore(ds1);
        testSecondDatastore(ds2);

        testStandardDatastore();
    }


    @Test
    public void testSaveAndRemove() {
        final Rectangle rect = new Rectangle(10, 10);
        ObjectId id = new ObjectId();
        rect.setId(id);

        //test delete(entity)
        getDs().save(rect);
        assertEquals(1, getDs().find(rect.getClass()).count());
        getDs().delete(rect);
        assertEquals(0, getDs().find(rect.getClass()).count());

        //test delete(entity, id)
        getDs().save(rect);
        assertEquals(1, getDs().find(rect.getClass()).count());
        getDs().find(rect.getClass()).filter(eq("_id", 1)).findAndDelete();
        assertEquals(1, getDs().find(rect.getClass()).count());
        getDs().find(rect.getClass()).filter(eq("_id", id)).findAndDelete();
        assertEquals(0, getDs().find(rect.getClass()).count());

        //test delete(entity, {id})
        getDs().save(rect);
        assertEquals(1, getDs().find(rect.getClass()).count());
        getDs().find(rect.getClass()).filter(in("_id", singletonList(rect.getId()))).findAndDelete();
        assertEquals(0, getDs().find(rect.getClass()).count());

        //test delete(entity, {id,id})
        ObjectId id1 = getDs().save(new Rectangle(10, 10)).getId();
        ObjectId id2 = getDs().save(new Rectangle(10, 10)).getId();
        assertEquals(2, getDs().find(rect.getClass()).count());
        Query<? extends Rectangle> query = getDs().find(rect.getClass())
                                                  .filter(in("_id", asList(id1, id2)));

        query.delete(new DeleteOptions()
                         .multi(true));
        assertEquals(0, getDs().find(rect.getClass()).count());

        //test delete(Class, {id,id})
        id1 = getDs().save(new Rectangle(20, 20)).getId();
        id2 = getDs().save(new Rectangle(20, 20)).getId();
        assertEquals("datastore should have saved two entities with autogenerated ids", 2, getDs().find(rect.getClass()).count());
        getDs().find(rect.getClass())
               .filter(in("_id", asList(id1, id2)))
               .delete(new DeleteOptions()
                           .multi(true));
        assertEquals("datastore should have deleted two entities with autogenerated ids", 0, getDs().find(rect.getClass()).count());

        //test delete(entity, {id}) with one left
        id1 = getDs().save(new Rectangle(20, 20)).getId();
        getDs().save(new Rectangle(20, 20));
        assertEquals(2, getDs().find(rect.getClass()).count());
        getDs().find(rect.getClass()).filter(in("_id", singletonList(id1))).findAndDelete();
        assertEquals(1, getDs().find(rect.getClass()).count());
        getMapper().getCollection(Rectangle.class).drop();

        //test delete(Class, {id}) with one left
        id1 = getDs().save(new Rectangle(20, 20)).getId();
        getDs().save(new Rectangle(20, 20));
        assertEquals(2, getDs().find(rect.getClass()).count());
        getDs().find(Rectangle.class).filter(in("_id", singletonList(id1))).findAndDelete();
        assertEquals(1, getDs().find(rect.getClass()).count());
    }


    private void testFirstDatastore(Datastore ds1) {
        final FacebookUser user = ds1.find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                                     .next();
        Assert.assertNotNull(user);
        Assert.assertNotNull(ds1.find(FacebookUser.class).filter(eq("id", 3)).iterator(new FindOptions().limit(1))
                                .next());

        Assert.assertEquals("Should find 1 friend", 1, user.friends.size());
        Assert.assertEquals("Should find the right friend", 3, user.friends.get(0).id);

        Assert.assertNull(ds1.find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                             .tryNext());
        Assert.assertNull(ds1.find(FacebookUser.class).filter(eq("id", 4)).iterator(new FindOptions().limit(1))
                             .tryNext());
    }

    private void testSecondDatastore(Datastore ds2) {
        Assert.assertNull(ds2.find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                             .tryNext());
        Assert.assertNull(ds2.find(FacebookUser.class).filter(eq("id", 3)).iterator(new FindOptions().limit(1))
                             .tryNext());

        final FacebookUser db2FoundUser = ds2.find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                                             .next();
        Assert.assertNotNull(db2FoundUser);
        Assert.assertNotNull(ds2.find(FacebookUser.class).filter(eq("id", 4)).iterator(new FindOptions().limit(1))
                                .next());
        Assert.assertEquals("Should find 1 friend", 1, db2FoundUser.friends.size());
        Assert.assertEquals("Should find the right friend", 4, db2FoundUser.friends.get(0).id);
    }

    private void testStandardDatastore() {
        Assert.assertNull(getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                                 .tryNext());
        Assert.assertNull(getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                                 .tryNext());
        Assert.assertNull(getDs().find(FacebookUser.class).filter(eq("id", 3)).iterator(new FindOptions().limit(1))
                                 .tryNext());
        Assert.assertNull(getDs().find(FacebookUser.class).filter(eq("id", 4)).iterator(new FindOptions().limit(1))
                                 .tryNext());
    }

    @Entity("facebook_users")
    public static class FacebookUser {
        public int loginCount;
        @Id
        private long id;
        private String username;
        @Reference
        private final List<FacebookUser> friends = new ArrayList<>();

        public FacebookUser(long id, String name) {
            this();
            this.id = id;
            username = name;
        }

        public FacebookUser() {
        }

        public long getId() {
            return id;
        }
    }

    @Entity
    @SuppressWarnings({"UnusedDeclaration", "removal"})
    public static class Keys {
        @Id
        private ObjectId id;
        private List<Key<FacebookUser>> users;
        private Key<Rectangle> rect;

        private Keys() {
        }

        public Keys(Key<Rectangle> rectKey, List<Key<FacebookUser>> users) {
            rect = rectKey;
            this.users = users;
        }

        public ObjectId getId() {
            return id;
        }

        public Key<Rectangle> getRect() {
            return rect;
        }

        public List<Key<FacebookUser>> getUsers() {
            return users;
        }
    }

    public static class LifecycleListener {
        private static boolean prePersist;
        private static boolean prePersistWithEntity;
        private static boolean foundDatastore;

        @PrePersist
        void prePersist(Datastore datastore) {
            foundDatastore = datastore != null;
            prePersist = true;
        }

        @PrePersist
        void prePersist(LifecycleTestObj obj) {
            if (obj == null) {
                throw new RuntimeException();
            }
            prePersistWithEntity = true;

        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Entity
    @EntityListeners(LifecycleListener.class)
    public static class LifecycleTestObj {
        @Id
        private ObjectId id;
        @Transient
        private boolean prePersist;
        @Transient
        private boolean postPersist;
        @Transient
        private boolean preLoad;
        @Transient
        private boolean postLoad;
        @Transient
        private boolean postLoadWithParam;
        private boolean prePersistWithParamAndReturn;
        private boolean prePersistWithParam;
        private boolean postPersistWithParam;
        private boolean preLoadWithParamAndReturn;
        private boolean preLoadWithParam;

        @PrePersist
        public Document prePersistWithParamAndReturn(Document document) {
            if (prePersistWithParamAndReturn) {
                throw new RuntimeException("already called");
            }
            prePersistWithParamAndReturn = true;
            return null;
        }

        @PrePersist
        protected void prePersistWithParam(Document document) {
            if (prePersistWithParam) {
                throw new RuntimeException("already called");
            }
            prePersistWithParam = true;
        }

        @PostPersist
        private void postPersistPersist() {
            if (postPersist) {
                throw new RuntimeException("already called");
            }
            postPersist = true;

        }

        @PostLoad
        void postLoad() {
            if (postLoad) {
                throw new RuntimeException("already called");
            }

            postLoad = true;
        }

        @PostLoad
        void postLoadWithParam(Document document) {
            if (postLoadWithParam) {
                throw new RuntimeException("already called");
            }
            postLoadWithParam = true;
        }

        @PostPersist
        void postPersistWithParam(Document document) {
            postPersistWithParam = true;
            if (!document.containsKey("_id")) {
                throw new RuntimeException("missing " + "_id");
            }
        }

        @PreLoad
        void preLoad() {
            if (preLoad) {
                throw new RuntimeException("already called");
            }

            preLoad = true;
        }

        @PreLoad
        void preLoadWithParam(Document document) {
            document.put("preLoadWithParam", true);
        }

        @PreLoad
        Document preLoadWithParamAndReturn(Document document) {
            final Document retObj = new Document();
            retObj.putAll(document);
            retObj.put("preLoadWithParamAndReturn", true);
            return retObj;
        }

        @PrePersist
        void prePersist() {
            if (prePersist) {
                throw new RuntimeException("already called");
            }

            prePersist = true;
        }
    }
}
