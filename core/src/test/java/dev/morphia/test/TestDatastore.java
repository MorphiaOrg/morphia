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
import dev.morphia.query.Update;
import dev.morphia.test.models.City;
import dev.morphia.test.models.CurrentStatus;
import dev.morphia.test.models.FacebookUser;
import org.testng.annotations.Test;

import static com.mongodb.client.model.ReturnDocument.AFTER;
import static com.mongodb.client.model.ReturnDocument.BEFORE;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.updates.UpdateOperators.inc;
import static dev.morphia.query.experimental.updates.UpdateOperators.set;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

class TestDatastore extends TestBase {
    @Test
    public void testCappedEntity() {
        // given
        getMapper().map(CurrentStatus.class);
        getDatastore().ensureCaps();

        assertCapped(CurrentStatus.class, 1, 1048576);

        // when-then
        Query<CurrentStatus> query = getDatastore().find(CurrentStatus.class);

        getDatastore().save(new CurrentStatus("All Good"));
        assertEquals(query.count(), 1);

        getDatastore().save(new CurrentStatus("Kinda Bad"));
        assertEquals(query.count(), 1);

        assertTrue(query.iterator(new FindOptions().limit(1))
                        .next()
                       .message.contains("Bad"));

        getDatastore().save(new CurrentStatus("Kinda Bad2"));
        assertEquals(query.count(), 1);

        getDatastore().save(new CurrentStatus("Kinda Bad3"));
        assertEquals(query.count(), 1);

        getDatastore().save(new CurrentStatus("Kinda Bad4"));
        assertEquals(query.count(), 1);
    }


    @Test
    public void testDeleteWithCollation() {
        getDatastore().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDatastore().find(FacebookUser.class)
                                                  .filter(eq("username", "john doe"));
        assertEquals(query.delete().getDeletedCount(), 1);

        assertEquals(query.delete(new DeleteOptions()
                                      .collation(Collation.builder()
                                                          .locale("en")
                                                          .collationStrength(CollationStrength.SECONDARY)
                                                          .build()))
                          .getDeletedCount(), 1);
    }

    @Test
    public void testDeletes() {
        for (int i = 0; i < 100; i++) {
            getDatastore().save(new City());
        }
        DeleteResult delete = getDatastore().find(City.class).delete();
        assertEquals(delete.getDeletedCount(), 1, "Should only delete 1");

        City first = getDatastore().find(City.class).first();
        delete = getDatastore().delete(first);
        assertEquals(delete.getDeletedCount(), 1, "Should only delete 1");

        first = getDatastore().find(City.class).first();
        delete = getDatastore().delete(first, new DeleteOptions().multi(true));
        assertEquals(delete.getDeletedCount(), 1, "Should only delete 1");

        delete = getDatastore().find(City.class).delete(new DeleteOptions().multi(true));
        assertTrue(delete.getDeletedCount() > 1, "Should the rest");
    }

    @Test
    public void testFindAndDeleteWithCollation() {
        getDatastore().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDatastore().find(FacebookUser.class)
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
        getDatastore().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        FacebookUser result = getDatastore().find(FacebookUser.class)
                                            .filter(eq("username", "john doe"))
                                            .modify(inc("loginCount"))
                                            .execute();

        assertEquals(getDatastore().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                                   .next()
                         .loginCount, 0);
        assertEquals(getDatastore().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                                   .next()
                         .loginCount, 1);
        assertEquals(result.loginCount, 0);

        result = getDatastore().find(FacebookUser.class)
                               .filter(eq("username", "john doe"))
                               .modify(inc("loginCount"))
                               .execute(new ModifyOptions()
                                            .returnDocument(BEFORE)
                                            .collation(Collation.builder()
                                                                .locale("en")
                                                                .collationStrength(CollationStrength.SECONDARY)
                                                                .build()));
        assertEquals(getDatastore().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                                   .next()
                         .loginCount, 1);
        assertEquals(result.loginCount, 0);
        assertEquals(getDatastore().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                                   .next()
                         .loginCount, 1);

        result = getDatastore().find(FacebookUser.class)
                               .filter(eq("id", 3L),
                                   eq("username", "Jon Snow"))
                               .modify(inc("loginCount"))
                               .execute(new ModifyOptions()
                                            .returnDocument(BEFORE)
                                            .upsert(true));

        assertNull(result);
        FacebookUser user = getDatastore().find(FacebookUser.class).filter(eq("id", 3)).iterator(new FindOptions().limit(1))
                                          .next();
        assertEquals(user.loginCount, 1);
        assertEquals(user.username, "Jon Snow");


        result = getDatastore().find(FacebookUser.class)
                               .filter(eq("id", 4L),
                                   eq("username", "Ron Swanson"))
                               .modify(inc("loginCount"))
                               .execute(new ModifyOptions()
                                            .returnDocument(AFTER)
                                            .upsert(true));

        assertNotNull(result);
        user = getDatastore().find(FacebookUser.class).filter(eq("id", 4)).iterator(new FindOptions().limit(1))
                             .next();
        assertEquals(result.loginCount, 1);
        assertEquals(result.username, "Ron Swanson");
        assertEquals(user.loginCount, 1);
        assertEquals(user.username, "Ron Swanson");
    }

    @Test
    public void testRefresh() {
        FacebookUser steve = getDatastore().save(new FacebookUser(1, "Steve"));

        assertEquals(steve.loginCount, 0);
        UpdateResult loginCount = getDatastore().find(FacebookUser.class)
                                                .update(inc("loginCount", 10))
                                                .execute();

        assertEquals(loginCount.getModifiedCount(), 1);

        getDatastore().refresh(steve);
        assertEquals(steve.loginCount, 10);

        loginCount = getDatastore().find(FacebookUser.class)
                                   .update(
                                       set("username", "Mark"),
                                       set("loginCount", 1))
                                   .execute();

        assertEquals(loginCount.getModifiedCount(), 1);
        getDatastore().refresh(steve);
        assertEquals(steve.loginCount, 1);
        assertEquals(steve.username, "Mark");

    }

    @Test
    public void testUpdateWithCollation() {
        getDatastore().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        final Update<FacebookUser> update = getDatastore().find(FacebookUser.class)
                                                          .filter(eq("username", "john doe"))
                                                          .update(inc("loginCount"));

        UpdateResult results = update.execute();

        assertEquals(results.getModifiedCount(), 1);
        assertEquals(getDatastore().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1)).next()
                         .loginCount, 0);
        assertEquals(getDatastore().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                                   .next()
                         .loginCount, 1);

        results = update.execute(new UpdateOptions()
                                     .multi(true)
                                     .collation(Collation.builder()
                                                         .locale("en")
                                                         .collationStrength(CollationStrength.SECONDARY)
                                                         .build()));
        assertEquals(results.getModifiedCount(), 2);
        assertEquals(getDatastore().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                                   .next()
                         .loginCount, 1);
        assertEquals(getDatastore().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                                   .next()
                         .loginCount, 2);
    }

}