package dev.morphia.query.experimental.filters;

import com.mongodb.client.MongoCollection;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.testmodel.User;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.bitsAllClear;
import static dev.morphia.query.experimental.filters.Filters.bitsAllSet;
import static dev.morphia.query.experimental.filters.Filters.bitsAnyClear;
import static dev.morphia.query.experimental.filters.Filters.bitsAnySet;
import static dev.morphia.query.experimental.filters.Filters.expr;
import static dev.morphia.query.experimental.filters.Filters.gt;
import static java.util.Arrays.asList;

public class FiltersTest extends TestBase {

    @Test
    public void testBitsAllClear() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");
        collection.drop();

        collection.insertMany(asList(
            new Document("a", 54).append("binaryValueofA", "00110110").append("_t", "User"),
            new Document("a", 20).append("binaryValueofA", "00010100").append("_t", "User"),
            new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", "User")));

        FindOptions options = new FindOptions().logQuery();

        List<User> found = getDs().find(User.class)
                                  .disableValidation()
                                  .filter(bitsAllClear("a", 35))
                                  .execute(options)
                                  .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 2, found.size());

        found = getDs().find(User.class)
                       .disableValidation()
                       .filter(bitsAllClear("a", new int[]{1, 5}))
                       .execute(options)
                       .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 2, found.size());
    }

    @Test
    public void testBitsAllSet() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");
        collection.drop();

        collection.insertMany(asList(
            new Document("a", 54).append("binaryValueofA", "00110110").append("_t", "User"),
            new Document("a", 20).append("binaryValueofA", "00010100").append("_t", "User"),
            new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", "User")));

        FindOptions options = new FindOptions().logQuery();

        List<User> found = getDs().find(User.class)
                                  .disableValidation()
                                  .filter(bitsAllSet("a", 50))
                                  .execute(options)
                                  .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 1, found.size());

        options = new FindOptions().logQuery();
        found = getDs().find(User.class)
                       .disableValidation()
                       .filter(bitsAllSet("a", new int[]{1, 5}))
                       .execute(options)
                       .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 1, found.size());
    }

    @Test
    public void testBitsAnySet() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");
        collection.drop();

        collection.insertMany(asList(
            new Document("a", 54).append("binaryValueofA", "00110110").append("_t", "User"),
            new Document("a", 20).append("binaryValueofA", "00010100").append("_t", "User"),
            new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", "User")));

        FindOptions options = new FindOptions().logQuery();

        List<User> found = getDs().find(User.class)
                                  .disableValidation()
                                  .filter(bitsAnySet("a", 35))
                                  .execute(options)
                                  .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 1, found.size());

        options = new FindOptions().logQuery();
        found = getDs().find(User.class)
                       .disableValidation()
                       .filter(bitsAnySet("a", new int[]{1, 5}))
                       .execute(options)
                       .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 1, found.size());
    }

    @Test
    public void testBitsAnyClear() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");
        collection.drop();

        collection.insertMany(asList(
            new Document("a", 54).append("binaryValueofA", "00110110").append("_t", "User"),
            new Document("a", 20).append("binaryValueofA", "00010100").append("_t", "User"),
            new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", "User")));

        FindOptions options = new FindOptions().logQuery();

        List<User> found = getDs().find(User.class)
                                  .disableValidation()
                                  .filter(bitsAnyClear("a", 35))
                                  .execute(options)
                                  .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 3, found.size());

        options = new FindOptions().logQuery();
        found = getDs().find(User.class)
                       .disableValidation()
                       .filter(bitsAnyClear("a", new int[]{1, 5}))
                       .execute(options)
                       .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 2, found.size());
    }

    @Test
    public void testExpr() {
        getDatabase().getCollection("budget").insertMany(asList(
            Document.parse("{ '_id' : 1, 'category' : 'food', 'budget': 400, 'spent': 450 }"),
            Document.parse("{ '_id' : 2, 'category' : 'drinks', 'budget': 100, 'spent': 150 }"),
            Document.parse("{ '_id' : 3, 'category' : 'clothes', 'budget': 100, 'spent': 50 }"),
            Document.parse("{ '_id' : 4, 'category' : 'misc', 'budget': 500, 'spent': 300 }"),
            Document.parse("{ '_id' : 5, 'category' : 'travel', 'budget': 200, 'spent': 650 }")));

        List<Budget> budgets = getDs().find(Budget.class)
                                      .filter(expr(gt("$spent", "$budget")))
                                      .execute()
                                      .toList();

        Assert.assertEquals(3, budgets.size());
    }


    @Entity(value = "budget", useDiscriminator = false)
    static class Budget {
        @Id
        private int id;
        private String category;
        private int budget;
        private int spent;
    }
}