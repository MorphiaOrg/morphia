package dev.morphia.aggregation;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import dev.morphia.TestBase;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.aggregation.zipcode.City;
import dev.morphia.aggregation.zipcode.Population;
import dev.morphia.aggregation.zipcode.State;
import dev.morphia.query.internal.MorphiaCursor;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
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
import static java.lang.String.format;

/**
 * These tests recreate the example zip code data set aggregations as found in the official documentation.
 *
 * @mongodb.driver.manual tutorial/aggregation-zip-code-data-set/ Aggregation with the Zip Code Data Set
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ZipCodeDataSetTest extends TestBase {
    public static final File MONGO_IMPORT;
    private static final Logger LOG = LoggerFactory.getLogger(ZipCodeDataSetTest.class);

    static {
        String property = System.getProperty("mongodb_server");
        String serverType = property != null ? property.replaceAll("-release", "") : "UNKNOWN";
        File path = new File(format("/mnt/jenkins/mongodb/%s/%s/bin/mongoimport", serverType, property));
        if (path.exists()) {
            MONGO_IMPORT = path;
        } else {
            MONGO_IMPORT = Arrays.stream(System.getenv("PATH").split(File.pathSeparator))
                                 .map(p -> new File(p, "mongoimport"))
                                 .filter(File::exists)
                                 .findFirst()
                                 .orElseGet(() -> new File("/notreally here"));
        }
    }

    @Test
    public void averageCitySizeByState() {
        Assume.assumeTrue(MONGO_IMPORT.exists());
        installSampleData();

        Aggregation pipeline = getDs().aggregate(City.class)
                                      .group(of(id().field("state")
                                                    .field("city"))
                                                 .field("pop", sum(field("pop"))))
                                      .group(of(
                                          id("_id.state"))
                                                 .field("avgCityPop", avg(field("pop"))));
        validate(pipeline.execute(Population.class), "MN", 5372);
    }

    public void installSampleData() {
        File file = new File("zips.json");
        try {
            if (!file.exists()) {
                file = new File(System.getProperty("java.io.tmpdir"), "zips.json");
                if (!file.exists()) {
                    download(new URL("https://media.mongodb.org/zips.json"), file);
                }
            }
            MongoCollection<Document> zips = getDatabase().getCollection("zips");
            if (zips.countDocuments() == 0) {
                new ProcessExecutor().command(MONGO_IMPORT.getAbsolutePath(),
                    "--db", getDatabase().getName(),
                    "--collection", "zipcodes",
                    "--file", file.getAbsolutePath())
                                     .redirectError(System.err)
                                     .execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assume.assumeTrue("Failed to process media files", file.exists());
    }

    @Test
    public void populationsAbove10M() {
        Assume.assumeTrue(MONGO_IMPORT.exists());
        installSampleData();

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
        Assume.assumeTrue(MONGO_IMPORT.exists());
        installSampleData();
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

            Assert.assertEquals("SIOUX FALLS", state.getBiggest().getName());
            Assert.assertEquals(102046, state.getBiggest().getPopulation().longValue());

            Assert.assertEquals("ZEONA", state.getSmallest().getName());
            Assert.assertEquals(8, state.getSmallest().getPopulation().longValue());
        }
    }

    private void download(final URL url, final File file) throws IOException {
        LOG.info("Downloading zip data set to " + file);
        try (InputStream inputStream = url.openStream(); FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] read = new byte[49152];
            int count;
            while ((count = inputStream.read(read)) != -1) {
                outputStream.write(read, 0, count);
            }
        }
    }

    private void validate(final MorphiaCursor<Population> cursor, final String state, final long value) {
        boolean found = false;
        try (cursor) {
            while (cursor.hasNext()) {
                Population population = cursor.next();

                if (population.getState().equals(state)) {
                    found = true;
                    Assert.assertEquals(Long.valueOf(value), population.getPopulation());
                }
                LOG.debug("population = " + population);
            }
            Assert.assertTrue("Should have found " + state, found);
        }
    }

}
