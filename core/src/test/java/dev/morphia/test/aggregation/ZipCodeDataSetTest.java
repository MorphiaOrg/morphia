package dev.morphia.test.aggregation;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.City;
import dev.morphia.test.models.Population;
import dev.morphia.test.models.State;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.first;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.last;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.aggregation.experimental.stages.Group.of;
import static dev.morphia.query.experimental.filters.Filters.gte;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * These tests recreate the example zip code data set aggregations as found in the official documentation.
 *
 * @mongodb.driver.manual tutorial/aggregation-zip-code-data-set/ Aggregation with the Zip Code Data Set
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Ignore
public class ZipCodeDataSetTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(ZipCodeDataSetTest.class);

    @BeforeMethod
    public void installData() {
        File file = new File("zips.json");
        try {
            if (!file.exists()) {
                file = new File("target/zips.json");
                if (!file.exists()) {
                    download(new URL("https://media.mongodb.org/zips.json"), file);
                }
            }
            MongoCollection<Document> zips = getDatabase().getCollection("zipcodes");
            if (zips.countDocuments() == 0) {
                LOG.info("Count is 0.  (Re)installing sample data");
                MongoCollection<Document> zipcodes = getDatabase().getCollection("zipcodes");
                zipcodes.drop();
                Files.lines(file.toPath())
                     .forEach(l -> zipcodes.insertOne(Document.parse(l)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assumeTrue(file.exists(), "Failed to process media files");
    }

    @Test
    public void averageCitySizeByState() {
        Aggregation pipeline = getDs().aggregate(City.class)
                                      .group(of(id().field("state")
                                                    .field("city"))
                                                 .field("pop", sum(field("pop"))))
                                      .group(of(
                                          id("_id.state"))
                                                 .field("avgCityPop", avg(field("pop"))));
        validate(pipeline.execute(Population.class), "MN", 5372);
    }


    @Test
    public void populationsAbove10M() {
        Aggregation pipeline
            = getDs().aggregate(City.class)
                     .group(of(id("state"))
                                .field("totalPop", sum(field("pop"))))
                     .match(gte("totalPop", 10000000));

        validate(pipeline.execute(Population.class), "CA", 29754890);
        validate(pipeline.execute(Population.class), "OH", 10846517);
    }

    @Test
    public void smallestAndLargestCities() {
        getMapper().mapPackage(getClass().getPackage().getName());

        Aggregation pipeline = getDs().aggregate(City.class)

                                      .group(of(id().field("state")
                                                    .field("city"))
                                                 .field("pop", sum(field("pop"))))

                                      .sort(Sort.on().ascending("pop"))

                                      .group(of(
                                          id("_id.state"))
                                                 .field("biggestCity", last(field("_id.city")))
                                                 .field("biggestPop", last(field("pop")))
                                                 .field("smallestCity", first(field("_id.city")))
                                                 .field("smallestPop", first(field("pop"))))

                                      .project(
                                          Projection.of()
                                                    .exclude("_id")
                                                    .include("state", field("_id"))
                                                    .include("biggestCity",
                                                        Expressions.of()
                                                                   .field("name", field("biggestCity"))
                                                                   .field("pop", field("biggestPop")))
                                                    .include("smallestCity",
                                                        Expressions.of()
                                                                   .field("name", field("smallestCity"))
                                                                   .field("pop", field("smallestPop"))));

        try (MongoCursor<State> cursor = (MongoCursor<State>) pipeline.execute(State.class)) {
            Map<String, State> states = new HashMap<>();
            while (cursor.hasNext()) {
                State state = cursor.next();
                states.put(state.getState(), state);
            }

            State state = states.get("SD");

            assertEquals(state.getBiggest().getName(), "SIOUX FALLS");
            assertEquals(state.getBiggest().getPopulation().longValue(), 102046);

            assertEquals(state.getSmallest().getName(), "ZEONA");
            assertEquals(state.getSmallest().getPopulation().longValue(), 8);
        }
    }

    private void validate(MorphiaCursor<Population> cursor, String state, long value) {
        boolean found = false;
        try (cursor) {
            while (cursor.hasNext()) {
                Population population = cursor.next();

                if (population.getState().equals(state)) {
                    found = true;
                    assertEquals(population.getPopulation(), Long.valueOf(value));
                }
                LOG.debug("population = " + population);
            }
            assertTrue(found, "Should have found " + state);
        }
    }

}
