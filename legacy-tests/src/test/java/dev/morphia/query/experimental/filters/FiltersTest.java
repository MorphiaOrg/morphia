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

import java.util.Date;
import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.query.experimental.filters.Filters.bitsAllClear;
import static dev.morphia.query.experimental.filters.Filters.bitsAllSet;
import static dev.morphia.query.experimental.filters.Filters.bitsAnyClear;
import static dev.morphia.query.experimental.filters.Filters.bitsAnySet;
import static dev.morphia.query.experimental.filters.Filters.expr;
import static dev.morphia.query.experimental.filters.Filters.gt;
import static dev.morphia.query.experimental.filters.Filters.lt;
import static dev.morphia.query.experimental.filters.Filters.nor;
import static dev.morphia.query.experimental.filters.Filters.size;
import static java.util.Arrays.asList;

public class FiltersTest extends TestBase {

    @Test
    public void testBitsAllClear() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");

        collection.insertMany(asList(
            new Document("a", 54).append("binaryValueofA", "00110110").append("_t", "User"),
            new Document("a", 20).append("binaryValueofA", "00010100").append("_t", "User"),
            new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", "User")));

        FindOptions options = new FindOptions().logQuery();

        List<User> found = getDs().find(User.class)
                                  .disableValidation()
                                  .filter(bitsAllClear("a", 35)).iterator(options)
                                  .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 2, found.size());

        found = getDs().find(User.class)
                       .disableValidation()
                       .filter(bitsAllClear("a", new int[]{1, 5})).iterator(options)
                       .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 2, found.size());
    }

    @Test
    public void testBitsAllSet() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");

        collection.insertMany(asList(
            new Document("a", 54).append("binaryValueofA", "00110110").append("_t", "User"),
            new Document("a", 20).append("binaryValueofA", "00010100").append("_t", "User"),
            new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", "User")));

        FindOptions options = new FindOptions().logQuery();

        List<User> found = getDs().find(User.class)
                                  .disableValidation()
                                  .filter(bitsAllSet("a", 50)).iterator(options)
                                  .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 1, found.size());

        options = new FindOptions().logQuery();
        found = getDs().find(User.class)
                       .disableValidation()
                       .filter(bitsAllSet("a", new int[]{1, 5})).iterator(options)
                       .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 1, found.size());
    }

    @Test
    public void testBitsAnyClear() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");

        collection.insertMany(asList(
            new Document("a", 54).append("binaryValueofA", "00110110").append("_t", "User"),
            new Document("a", 20).append("binaryValueofA", "00010100").append("_t", "User"),
            new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", "User")));

        FindOptions options = new FindOptions().logQuery();

        List<User> found = getDs().find(User.class)
                                  .disableValidation()
                                  .filter(bitsAnyClear("a", 35)).iterator(options)
                                  .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 3, found.size());

        options = new FindOptions().logQuery();
        found = getDs().find(User.class)
                       .disableValidation()
                       .filter(bitsAnyClear("a", new int[]{1, 5})).iterator(options)
                       .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 2, found.size());
    }

    @Test
    public void testBitsAnySet() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");

        collection.insertMany(asList(
            new Document("a", 54).append("binaryValueofA", "00110110").append("_t", "User"),
            new Document("a", 20).append("binaryValueofA", "00010100").append("_t", "User"),
            new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", "User")));

        FindOptions options = new FindOptions().logQuery();

        List<User> found = getDs().find(User.class)
                                  .disableValidation()
                                  .filter(bitsAnySet("a", 35)).iterator(options)
                                  .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 1, found.size());

        options = new FindOptions().logQuery();
        found = getDs().find(User.class)
                       .disableValidation()
                       .filter(bitsAnySet("a", new int[]{1, 5})).iterator(options)
                       .toList();

        Assert.assertEquals(getDs().getLoggedQuery(options), 1, found.size());
    }

    @Test
    public void testExpr() {
        insert("budget", asList(
            Document.parse("{ '_id' : 1, 'category' : 'food', 'budget': 400, 'spent': 450 }"),
            Document.parse("{ '_id' : 2, 'category' : 'drinks', 'budget': 100, 'spent': 150 }"),
            Document.parse("{ '_id' : 3, 'category' : 'clothes', 'budget': 100, 'spent': 50 }"),
            Document.parse("{ '_id' : 4, 'category' : 'misc', 'budget': 500, 'spent': 300 }"),
            Document.parse("{ '_id' : 5, 'category' : 'travel', 'budget': 200, 'spent': 650 }")));

        List<Budget> budgets = getDs().find(Budget.class)
                                      .filter(expr(gt(field("spent"), field("budget")))).iterator()
                                      .toList();

        Assert.assertEquals(3, budgets.size());
    }

    @Test
    public void testSize() {
        getDs().save(List.of(new User("John", new Date(), "puppies", "kittens", "heavy metal"),
            new User("Janice", new Date(), "Chandler", "NYC")));

        User likes = getDs().find(User.class)
                            .filter(size("likes", 3)).iterator()
                            .next();

        Assert.assertEquals("John", likes.name);

        likes = getDs().find(User.class)
                       .filter(size("likes", 2)).iterator()
                       .next();

        Assert.assertEquals("Janice", likes.name);

        likes = getDs().find(User.class)
                       .filter(size("likes", 20)).iterator()
                       .tryNext();

        Assert.assertNull(likes);
    }

    @Test
    public void testNor() {
        getDs().find(Budget.class)
               .filter(nor(lt("budget", 10000), gt("budget", 12)))
               .iterator();
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