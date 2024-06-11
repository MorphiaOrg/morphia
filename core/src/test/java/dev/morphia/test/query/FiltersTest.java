package dev.morphia.test.query;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.result.InsertManyResult;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.aggregation.stages.Count;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Meta;
import dev.morphia.query.MorphiaQuery;
import dev.morphia.query.Query;
import dev.morphia.query.Type;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Inventory;
import dev.morphia.test.models.Budget;
import dev.morphia.test.models.User;

import org.bson.Document;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Miscellaneous.rand;
import static dev.morphia.aggregation.expressions.Miscellaneous.sampleRate;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.and;
import static dev.morphia.query.filters.Filters.bitsAllClear;
import static dev.morphia.query.filters.Filters.bitsAllSet;
import static dev.morphia.query.filters.Filters.bitsAnyClear;
import static dev.morphia.query.filters.Filters.bitsAnySet;
import static dev.morphia.query.filters.Filters.expr;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.in;
import static dev.morphia.query.filters.Filters.jsonSchema;
import static dev.morphia.query.filters.Filters.lt;
import static dev.morphia.query.filters.Filters.lte;
import static dev.morphia.query.filters.Filters.mod;
import static dev.morphia.query.filters.Filters.nin;
import static dev.morphia.query.filters.Filters.nor;
import static dev.morphia.query.filters.Filters.or;
import static dev.morphia.query.filters.Filters.size;
import static dev.morphia.query.filters.Filters.text;
import static dev.morphia.query.filters.Filters.type;
import static dev.morphia.query.filters.Filters.where;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Deprecated
@SuppressWarnings("resource")
public class FiltersTest extends TemplatedTestBase {
    @AfterClass
    @Override
    public void testCoverage() {
    }

    @Test
    public void testAnd() {
        getDs().find(Budget.class)
                .filter(and(lt("budget", 10000), gt("budget", 12)))
                .iterator();
        getDs().find(Budget.class)
                .filter(and(lte("budget", 10000), gte("budget", 12)))
                .iterator();
    }

    @Test
    public void testType() {
        User entity = new User();
        entity.name = "first_name";
        getDs().save(entity);

        getMapper().map(User.class);

        Query<User> query = getDs().find(User.class);
        query.filter(type("name", Type.STRING));
        Assert.assertTrue(query.count() > 0);
    }

    @Test
    public void testBitsAllClear() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");

        String discriminator = User.class.getName();
        collection.insertMany(asList(
                new Document("a", 54).append("binaryValueofA", "00110110").append("_t", discriminator),
                new Document("a", 20).append("binaryValueofA", "00010100").append("_t", discriminator),
                new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", discriminator)));

        FindOptions options = new FindOptions().logQuery();

        Query<User> query = getDs().find(User.class)
                .disableValidation()
                .filter(bitsAllClear("a", 35));
        assertEquals(query.iterator(options).toList().size(), 2, query.getLoggedQuery());

        query = getDs().find(User.class)
                .disableValidation()
                .filter(bitsAllClear("a", new int[] { 1, 5 }));
        assertEquals(query.iterator(options).toList().size(), 2, query.getLoggedQuery());
    }

    @Test
    public void testBitsAllSet() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");

        String discriminator = User.class.getName();
        collection.insertMany(asList(
                new Document("a", 54).append("binaryValueofA", "00110110").append("_t", discriminator),
                new Document("a", 20).append("binaryValueofA", "00010100").append("_t", discriminator),
                new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", discriminator)));

        final FindOptions options = new FindOptions().logQuery();

        Query<User> query = getDs().find(User.class)
                .disableValidation()
                .filter(bitsAllSet("a", 50));
        assertEquals(query.iterator(options).toList().size(), 1, query.getLoggedQuery());

        query = getDs().find(User.class)
                .disableValidation()
                .filter(bitsAllSet("a", new int[] { 1, 5 }));
        assertEquals(query.iterator(options)
                .toList().size(), 1, query.getLoggedQuery());
    }

    @Test
    public void testBitsAnyClear() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");

        String discriminator = User.class.getName();
        collection.insertMany(asList(
                new Document("a", 54).append("binaryValueofA", "00110110").append("_t", discriminator),
                new Document("a", 20).append("binaryValueofA", "00010100").append("_t", discriminator),
                new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", discriminator)));

        FindOptions options = new FindOptions().logQuery();

        Query<User> query = getDs().find(User.class)
                .disableValidation()
                .filter(bitsAnyClear("a", 35));
        assertEquals(query.iterator(options).toList().size(), 3, query.getLoggedQuery());

        query = getDs().find(User.class)
                .disableValidation()
                .filter(bitsAnyClear("a", new int[] { 1, 5 }));
        assertEquals(query.iterator(options)
                .toList().size(), 2, query.getLoggedQuery());
    }

    @Test
    public void testBitsAnySet() {
        MongoCollection<Document> collection = getDatabase().getCollection("users");

        String discriminator = User.class.getName();
        collection.insertMany(asList(
                new Document("a", 54).append("binaryValueofA", "00110110").append("_t", discriminator),
                new Document("a", 20).append("binaryValueofA", "00010100").append("_t", discriminator),
                new Document("a", 20.0).append("binaryValueofA", "00010100").append("_t", discriminator)));

        FindOptions options = new FindOptions().logQuery();

        Query<User> query = getDs().find(User.class)
                .disableValidation()
                .filter(bitsAnySet("a", 35));
        assertEquals(query.iterator(options).toList().size(), 1, query.getLoggedQuery());

        assertEquals(getDs().find(User.class)
                .disableValidation()
                .filter(bitsAnySet("a", new int[] { 1, 5 })).iterator(options)
                .toList().size(), 1, query.getLoggedQuery());
    }

    @Test
    public void testExpr() {
        insert("budget", asList(
                parse("{ '_id' : 1, 'category' : 'food', 'budget': 400, 'spent': 450 }"),
                parse("{ '_id' : 2, 'category' : 'drinks', 'budget': 100, 'spent': 150 }"),
                parse("{ '_id' : 3, 'category' : 'clothes', 'budget': 100, 'spent': 50 }"),
                parse("{ '_id' : 4, 'category' : 'misc', 'budget': 500, 'spent': 300 }"),
                parse("{ '_id' : 5, 'category' : 'travel', 'budget': 200, 'spent': 650 }")));

        List<Budget> budgets = getDs().find(Budget.class)
                .filter(expr(ComparisonExpressions.gt("$spent", "$budget"))).iterator()
                .toList();

        assertEquals(budgets.size(), 3);
    }

    @Test
    public void testIn() {
        getDs().find(Budget.class)
                .filter(in("budget", asList(123, 234)))
                .iterator();
    }

    @Test
    public void testJsonSchema() {
        insert("inventory", List.of(
                parse("{ item: 'journal', qty: NumberInt(25), size: { h: 14, w: 21, uom: 'cm' }, instock: true }"),
                parse("{ item: 'notebook', qty: NumberInt(50), size: { h: 8.5, w: 11, uom: 'in' }, instock: true }"),
                parse("{ item: 'paper', qty: NumberInt(100), size: { h: 8.5, w: 11, uom: 'in' }, instock: 1 }"),
                parse("{ item: 'planner', qty: NumberInt(75), size: { h: 22.85, w: 30, uom: 'cm' }, instock: 1 }"),
                parse("{ item: 'postcard', qty: NumberInt(45), size: { h: 10, w: 15.25, uom: 'cm' }, instock: true }"),
                parse("{ item: 'apple', qty: NumberInt(45), status: 'A', instock: true }"),
                parse("{ item: 'pears', qty: NumberInt(50), status: 'A', instock: true }")));

        Document myschema = parse("""
                {
                  required: [ 'item', 'qty', 'instock' ],
                  properties: {
                    item: { bsonType: 'string' },
                    qty: { bsonType: 'int' },
                    size: {
                      bsonType: 'object',
                      required: [ 'uom' ],
                      properties: {
                        uom: { bsonType: 'string' },
                        h: { bsonType: 'double' },
                        w: { bsonType: 'double' }
                      }
                    },
                    instock: { bsonType: 'bool' }
                  }
                }""");

        List<Document> inventory = getDs().find("inventory", Document.class).filter(jsonSchema(myschema))
                .iterator()
                .toList();

        Assert.assertFalse(inventory.isEmpty(), "Should find some matches");
    }

    @Test
    public void testMeta() {
        MongoCollection<Document> articles = getDatabase().getCollection("articles");
        articles.insertMany(List.of(
                new Document("_id", 1).append("title", "cakes and ale"),
                new Document("_id", 2).append("title", "more cakes"),
                new Document("_id", 3).append("title", "bread"),
                new Document("_id", 4).append("title", "some cakes"),
                new Document("_id", 5).append("title", "two cakes to go"),
                new Document("_id", 6).append("title", "pie")));
        articles.createIndex(new Document("title", "text"));

        FindOptions options = new FindOptions().logQuery();
        Query<Document> query = getDs().find("articles", Document.class)
                .disableValidation()
                .filter(text("cake"));
        List<Document> list = query
                .iterator(options.projection()
                        .project(Meta.textScore("score")))
                .toList();
        assertEquals(list.size(), 4, query.getLoggedQuery());
        Document document = list.stream().filter(d -> d.get("_id").equals(4))
                .findFirst()
                .orElseThrow();
        assertEquals(document.get("score"), 1.0, query.getLoggedQuery());

    }

    @Test
    public void testMod() {
        var query = getDs().find(Inventory.class)
                .disableValidation()
                .filter(mod("qty", 4, 0));
        testQuery((MorphiaQuery<?>) query, new FindOptions(), true);

    }

    @Test
    public void testNin() {
        getDs().find(Budget.class)
                .filter(nin("budget", asList(123, 234)))
                .iterator();
    }

    @Test
    public void testNor() {
        getDs().find(Budget.class)
                .filter(nor(lt("budget", 10000), gt("budget", 12)))
                .iterator();
    }

    @Test
    public void testOr() {
        getDs().find(Budget.class)
                .filter(or(
                        lt("budget", 10000),
                        gt("budget", 12)))
                .iterator();
    }

    @Test
    public void testRand() {
        int count = 100;
        List<Document> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Document("_id", i).append("r", 0));
        }
        String collectionName = "rand";
        InsertManyResult bulk = getDatabase().getCollection(collectionName).insertMany(list, new InsertManyOptions().ordered(false));
        assertEquals(bulk.getInsertedIds().size(), count);
        long matches = getDs().find(collectionName, Document.class)
                .filter(expr(ComparisonExpressions.lt(0.5, rand())))
                .count();
        assertTrue(matches < 100);
    }

    @Test
    public void testSampleRate() {
        int count = 100;
        List<Document> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Document("_id", i).append("r", 0));
        }
        String collectionName = "sampleRate";
        InsertManyResult bulk = getDatabase().getCollection(collectionName).insertMany(list, new InsertManyOptions().ordered(false));
        assertEquals(bulk.getInsertedIds().size(), count);
        Document matches = getDs().aggregate(collectionName).pipeline(
                match(sampleRate(0.33)),
                Count.count("numMatches"))
                .execute(Document.class)
                .next();
        assertTrue(matches.getInteger("numMatches") < 100);
    }

    @Test
    public void testSize() {
        getDs().save(List.of(new User("John", LocalDate.now(), "puppies", "kittens", "heavy metal"),
                new User("Janice", LocalDate.now(), "Chandler", "NYC")));

        User likes = getDs().find(User.class)
                .filter(size("likes", 3)).iterator()
                .next();

        assertEquals(likes.name, "John");

        likes = getDs().find(User.class)
                .filter(size("likes", 2)).iterator()
                .next();

        assertEquals(likes.name, "Janice");

        likes = getDs().find(User.class)
                .filter(size("likes", 20)).iterator()
                .tryNext();

        assertNull(likes);
    }

    @Test
    public void testWhere() {
        getMapper().map(Player.class);
        insert("players", of(
                parse("{ _id: 12378, name: 'Steve', username: 'steveisawesome', first_login: '2017-01-01' }"),
                parse("{ _id: 2, name: 'Anya', username: 'anya', first_login: '2001-02-02' }")));

        Player player = getDs().find(Player.class)
                .filter(where("return (hex_md5(this.name) == '9b53e667f30cd329dca1ec9e6a83e994')"))
                .first();

        assertEquals(player.name, "Anya");
    }

    @Entity(value = "players", useDiscriminator = false)
    private static class Player {
        @Id
        private int id;
        String name;
        String username;
        String first_login;
    }
}
