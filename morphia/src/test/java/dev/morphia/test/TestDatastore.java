package dev.morphia.test;

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.DeleteOptions;
import dev.morphia.ModifyOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.query.FindAndDeleteOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.Update;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.test.models.City;
import dev.morphia.test.models.CurrentStatus;
import dev.morphia.test.models.FacebookUser;
import org.junit.jupiter.api.Test;

import static com.mongodb.client.model.ReturnDocument.AFTER;
import static com.mongodb.client.model.ReturnDocument.BEFORE;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.gte;
import static dev.morphia.query.experimental.updates.UpdateOperators.inc;
import static dev.morphia.query.experimental.updates.UpdateOperators.set;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDatastore extends TestBase {
    @Test
    public void testCappedEntity() {
        // given
        getMapper().map(CurrentStatus.class);
        getDs().ensureCaps();

        assertCapped(CurrentStatus.class, 1, null);

        // when-then
        Query<CurrentStatus> query = getDs().find(CurrentStatus.class);

        getDs().save(new CurrentStatus("All Good"));
        assertEquals(1, query.count());

        getDs().save(new CurrentStatus("Kinda Bad"));
        assertEquals(1, query.count());

        assertTrue(query.iterator(new FindOptions().limit(1))
                        .next()
                       .message.contains("Bad"));

        getDs().save(new CurrentStatus("Kinda Bad2"));
        assertEquals(1, query.count());

        getDs().save(new CurrentStatus("Kinda Bad3"));
        assertEquals(1, query.count());

        getDs().save(new CurrentStatus("Kinda Bad4"));
        assertEquals(1, query.count());
    }


    @Test
    public void testDeleteWithCollation() {
        getMapper().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .filter(eq("username", "john doe"));
        assertEquals(1, query.delete().getDeletedCount());

        assertEquals(1, query.delete(new DeleteOptions()
                                         .collation(Collation.builder()
                                                             .locale("en")
                                                             .collationStrength(CollationStrength.SECONDARY)
                                                             .build()))
                             .getDeletedCount());
    }

    @Test
    public void testDeletes() {
        DeleteResult delete = getDs().find(City.class).delete();
        assertEquals(1, delete.getDeletedCount(), "Should only delete 1");

        City first = getDs().find(City.class).first();
        delete = getDs().delete(first);
        assertEquals(1, delete.getDeletedCount(), "Should only delete 1");

        first = getDs().find(City.class).first();
        delete = getDs().delete(first, new DeleteOptions().multi(true));
        assertEquals(1, delete.getDeletedCount(), "Should only delete 1");

        delete = getDs().find(City.class).delete(new DeleteOptions().multi(true));
        assertTrue(delete.getDeletedCount() > 1, "Should the rest");
    }

    @Test
    public void testFindAndDeleteWithCollation() {
        getMapper().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .filter(eq("username", "john doe"));
        assertNotNull(query.findAndDelete());
        assertNull(query.findAndDelete());

        FindAndDeleteOptions options = new FindAndDeleteOptions()
                                           .collation(Collation.builder()
                                                               .locale("en")
                                                               .collationStrength(CollationStrength.SECONDARY)
                                                               .build());
        assertNotNull(query.findAndDelete(options));
        assertNull(query.iterator().tryNext());
    }

    @Test
    public void testFindAndModifyWithOptions() {
        getMapper().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        FacebookUser result = getDs().find(FacebookUser.class)
                                     .filter(eq("username", "john doe"))
                                     .modify(inc("loginCount"))
                                     .execute();

        assertEquals(0, getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(0, result.loginCount);

        result = getDs().find(FacebookUser.class)
                        .filter(eq("username", "john doe"))
                        .modify(inc("loginCount"))
                        .execute(new ModifyOptions()
                                     .returnDocument(BEFORE)
                                     .collation(Collation.builder()
                                                         .locale("en")
                                                         .collationStrength(CollationStrength.SECONDARY)
                                                         .build()));
        assertEquals(1, getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(0, result.loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                               .next()
                            .loginCount);

        result = getDs().find(FacebookUser.class)
                        .filter(eq("id", 3L),
                            eq("username", "Jon Snow"))
                        .modify(inc("loginCount"))
                        .execute(new ModifyOptions()
                                     .returnDocument(BEFORE)
                                     .upsert(true));

        assertNull(result);
        FacebookUser user = getDs().find(FacebookUser.class).filter(eq("id", 3)).iterator(new FindOptions().limit(1))
                                   .next();
        assertEquals(1, user.loginCount);
        assertEquals("Jon Snow", user.username);


        result = getDs().find(FacebookUser.class)
                        .filter(eq("id", 4L),
                            eq("username", "Ron Swanson"))
                        .modify(inc("loginCount"))
                        .execute(new ModifyOptions()
                                     .returnDocument(AFTER)
                                     .upsert(true));

        assertNotNull(result);
        user = getDs().find(FacebookUser.class).filter(eq("id", 4)).iterator(new FindOptions().limit(1))
                      .next();
        assertEquals(1, result.loginCount);
        assertEquals("Ron Swanson", result.username);
        assertEquals(1, user.loginCount);
        assertEquals("Ron Swanson", user.username);
    }

    @Test
    public void testQueries() {
        MorphiaCursor<City> cursor = getDs().find(City.class)
                                            .filter(gte("population", 50_000))
                                            .iterator(new FindOptions()
                                                          .sort(Sort.ascending("name")));

        assertTrue(cursor.hasNext());
    }

    @Test
    public void testRefresh() {
        FacebookUser steve = getDs().save(new FacebookUser(1, "Steve"));

        assertEquals(0, steve.loginCount);
        UpdateResult loginCount = getDs().find(FacebookUser.class)
                                         .update(inc("loginCount", 10))
                                         .execute();

        assertEquals(1, loginCount.getModifiedCount());

        getDs().refresh(steve);
        assertEquals(10, steve.loginCount);

        loginCount = getDs().find(FacebookUser.class)
                            .update(
                                set("username", "Mark"),
                                set("loginCount", 1))
                            .execute();

        assertEquals(1, loginCount.getModifiedCount());
        getDs().refresh(steve);
        assertEquals(1, steve.loginCount);
        assertEquals("Mark", steve.username);

    }

    @Test
    public void testUpdateWithCollation() {
        getMapper().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        final Update<FacebookUser> update = getDs().find(FacebookUser.class)
                                                   .filter(eq("username", "john doe"))
                                                   .update(inc("loginCount"));

        UpdateResult results = update.execute();

        assertEquals(1, results.getModifiedCount());
        assertEquals(0, getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1)).next()
                            .loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                               .next()
                            .loginCount);

        results = update.execute(new UpdateOptions()
                                     .multi(true)
                                     .collation(Collation.builder()
                                                         .locale("en")
                                                         .collationStrength(CollationStrength.SECONDARY)
                                                         .build()));
        assertEquals(2, results.getModifiedCount());
        assertEquals(1, getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(2, getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                               .next()
                            .loginCount);
    }

}