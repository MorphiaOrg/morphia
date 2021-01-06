package dev.morphia;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.generics.model.Child;
import dev.morphia.generics.model.ChildEntity;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateException;
import dev.morphia.testmodel.Address;
import dev.morphia.testmodel.Hotel;
import dev.morphia.testmodel.Rectangle;
import org.bson.types.ObjectId;
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

}
