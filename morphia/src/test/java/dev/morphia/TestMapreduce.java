package dev.morphia;


import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import dev.morphia.aggregation.AggregationTest.Book;
import dev.morphia.aggregation.AggregationTest.CountResult;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PreLoad;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.testmodel.Circle;
import dev.morphia.testmodel.Rectangle;
import dev.morphia.testmodel.Shape;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.Random;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;


public class TestMapreduce extends TestBase {

    @Test(expected = MongoException.class)
    public void testBadMR() {
        final String map = "function () { if(this['radius']) { doEmit('circle', {count:1}); return; } emit('rect', {count:1}); }";
        final String reduce = "function (key, values) { var total = 0; for ( var i=0; i<values.length; i++ ) {total += values[i].count;} "
                              + "return { count : total }; }";

        getDs().mapReduce(new MapReduceOptions<ResultEntity>()
                              .resultType(ResultEntity.class)
                              .outputType(OutputType.REPLACE)
                              .query(getAds().find(Shape.class))
                              .map(map)
                              .reduce(reduce));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testOldMapReduce() {
        final Random rnd = new Random();

        //create 100 circles and rectangles
        for (int i = 0; i < 100; i++) {
            getAds().insert("shapes", new Circle(rnd.nextDouble()));
            getAds().insert("shapes", new Rectangle(rnd.nextDouble(), rnd.nextDouble()));
        }
        final String map = "function () { if(this['radius']) { emit('circle', {count:1}); return; } emit('rect', {count:1}); }";
        final String reduce = "function (key, values) { var total = 0; for ( var i=0; i<values.length; i++ ) {total += values[i].count;} "
                              + "return { count : total }; }";

        final MapreduceResults<ResultEntity> mrRes =
            getDs().mapReduce(MapreduceType.REPLACE, getAds().find(Shape.class), map, reduce, null, null, ResultEntity.class);
        Assert.assertEquals(2, mrRes.createQuery().countAll());
        Assert.assertEquals(100, mrRes.createQuery()
                                      .find(new FindOptions().limit(1))
                                      .tryNext()
                                      .getValue().count, 0);


        final MapreduceResults<ResultEntity> inline =
            getDs().mapReduce(MapreduceType.INLINE, getAds().find(Shape.class), map, reduce, null, null, ResultEntity.class);
        final Iterator<ResultEntity> iterator = inline.iterator();
        Assert.assertEquals(2, count(iterator));
        Assert.assertEquals(100, inline.iterator().next().getValue().count, 0);
    }

    @Test
    public void testMapReduce() {
        final Random rnd = new Random();

        //create 100 circles and rectangles
        for (int i = 0; i < 100; i++) {
            getAds().insert("shapes", new Circle(rnd.nextDouble()));
            getAds().insert("shapes", new Rectangle(rnd.nextDouble(), rnd.nextDouble()));
        }
        final String map = "function () { if(this['radius']) { emit('circle', {count:1}); return; } emit('rect', {count:1}); }";
        final String reduce = "function (key, values) { var total = 0; for ( var i=0; i<values.length; i++ ) {total += values[i].count;} "
                              + "return { count : total }; }";

        final MapreduceResults<ResultEntity> mrRes =
            getDs().mapReduce(new MapReduceOptions<ResultEntity>()
                                  .outputType(OutputType.REPLACE)
                                  .query(getAds().find(Shape.class))
                                  .map(map)
                                  .reduce(reduce)
                                  .resultType(ResultEntity.class));
        Assert.assertEquals(2, mrRes.createQuery().count());
        Assert.assertEquals(100, mrRes.createQuery()
                                      .find(new FindOptions().limit(1))
                                      .tryNext()
                                      .getValue().count, 0);


        final MapreduceResults<ResultEntity> inline =
            getDs().mapReduce(new MapReduceOptions<ResultEntity>()
                                  .outputType(OutputType.INLINE)
                                  .query(getAds().find(Shape.class)).map(map).reduce(reduce)
                                  .resultType(ResultEntity.class));
        final Iterator<ResultEntity> iterator = inline.iterator();
        Assert.assertEquals(2, count(iterator));
        Assert.assertEquals(100, inline.iterator().next().getValue().count, 0);
    }

    @Test
    public void testCollation() {
        assumeMinServerVersion(3.4);
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                            new Book("Divine Comedy", "Dante", 1),
                            new Book("Eclogues", "Dante", 2),
                            new Book("The Odyssey", "Homer", 10),
                            new Book("Iliad", "Homer", 10)));

        final String map = "function () { emit(this.author, 1); return; }";
        final String reduce = "function (key, values) { return values.length }";

        Query<Book> query = getAds().find(Book.class)
            .field("author").equal("dante");
        MapReduceOptions<CountResult> options = new MapReduceOptions<CountResult>()
            .resultType(CountResult.class)
            .outputType(OutputType.INLINE)
            .query(query)
            .map(map)
            .reduce(reduce);
        Iterator<CountResult> iterator = getDs().mapReduce(options).getInlineResults();

        Assert.assertEquals(0, count(iterator));

        options
            .inputCollection(getMorphia().getMapper().getCollectionName(Book.class))
            .collation(Collation.builder()
                         .locale("en")
                         .collationStrength(CollationStrength.SECONDARY)
                         .build());
        iterator = getDs().mapReduce(options).getInlineResults();
        CountResult result = iterator.next();
        Assert.assertEquals("Dante", result.getAuthor());
        Assert.assertEquals(3D, result.getCount(), 0);
    }

    @Test
    public void testBypassDocumentValidation() {
        assumeMinServerVersion(3.4);
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                            new Book("Divine Comedy", "Dante", 1),
                            new Book("Eclogues", "Dante", 2),
                            new Book("The Odyssey", "Homer", 10),
                            new Book("Iliad", "Homer", 10)));

        Document validator = Document.parse("{ count : { $gt : '10' } }");
        ValidationOptions validationOptions = new ValidationOptions()
            .validator(validator)
            .validationLevel(ValidationLevel.STRICT)
            .validationAction(ValidationAction.ERROR);
        MongoDatabase database = getMongoClient().getDatabase(TEST_DB_NAME);
        database.getCollection("counts").drop();
        database.createCollection("counts", new CreateCollectionOptions().validationOptions(validationOptions));


        final String map = "function () { emit(this.author, 1); return; }";
        final String reduce = "function (key, values) { return values.length }";

        MapReduceOptions<CountResult> options = new MapReduceOptions<CountResult>()
            .query(getDs().find(Book.class))
            .resultType(CountResult.class)
            .outputType(OutputType.REPLACE)
            .map(map)
            .reduce(reduce);
        try {
            getDs().mapReduce(options);
            fail("Document validation should have complained.");
        } catch (MongoCommandException e) {
            // expected
        }

        getDs().mapReduce(options.bypassDocumentValidation(true));
        Assert.assertEquals(2, count(getDs().find(CountResult.class).iterator()));
    }

    @Entity("mr_results")
    private static class ResultEntity extends ResultBase<String, HasCount> {
    }

    public static class ResultBase<T, V> {
        @Id
        private T type;
        @Embedded
        private V value;

        public T getType() {
            return type;
        }

        public void setType(final T type) {
            this.type = type;
        }

        public V getValue() {
            return value;
        }

        public void setValue(final V value) {
            this.value = value;
        }
    }

    private static class HasCount {
        private double count;
    }

    @Entity("mr-results")
    private static class ResultEntity2 {
        @Id
        private String type;
        private double count;

        @PreLoad
        void preLoad(final BasicDBObject dbObj) {
            //pull all the fields from value field into the parent.
            dbObj.putAll((DBObject) dbObj.get("value"));
        }
    }

}
