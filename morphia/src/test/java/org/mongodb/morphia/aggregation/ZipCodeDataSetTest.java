package org.mongodb.morphia.aggregation;

import com.mongodb.DBCollection;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.aggregation.zipcode.City;
import org.mongodb.morphia.aggregation.zipcode.Population;
import org.mongodb.morphia.aggregation.zipcode.State;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static org.mongodb.morphia.aggregation.Group.average;
import static org.mongodb.morphia.aggregation.Group.first;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Group.id;
import static org.mongodb.morphia.aggregation.Group.last;
import static org.mongodb.morphia.aggregation.Group.sum;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static org.mongodb.morphia.aggregation.Sort.ascending;

/**
 * These tests recreate the example zip code data set aggregations as found in the official documentation.
 *
 * @mongodb.driver.manual tutorial/aggregation-zip-code-data-set/ Aggregation with the Zip Code Data Set
 */
public class ZipCodeDataSetTest extends TestBase {
    public static final String MONGO_IMPORT;
    private static final Logger LOG = MorphiaLoggerFactory.get(ZipCodeDataSetTest.class);

    static {
        String property = System.getProperty("mongodb_server");
        String serverType = property != null ? property.replaceAll("-release", "") : "UNKNOWN";
        String path = format("/mnt/jenkins/mongodb/%s/%s/bin/mongoimport", serverType, property);
        if (new File(path).exists()) {
            MONGO_IMPORT = path;
        } else {
            MONGO_IMPORT = "/usr/local/bin/mongoimport";
        }
    }

    @Test
    public void averageCitySizeByState() throws InterruptedException, TimeoutException, IOException {
        Assume.assumeTrue(new File(MONGO_IMPORT).exists());
        installSampleData();
        AggregationPipeline pipeline = getDs().createAggregation(City.class)
                                              .group(id(grouping("state"), grouping("city")), grouping("pop", sum("pop")))
                                              .group("_id.state", grouping("avgCityPop", average("pop")));
        validate(pipeline.aggregate(Population.class), "MN", 5372);
    }

    public void installSampleData() throws IOException, TimeoutException, InterruptedException {
        File file = new File("zips.json");
        if (!file.exists()) {
            file = new File(System.getProperty("java.io.tmpdir"), "zips.json");
            if (!file.exists()) {
                download(new URL("http://media.mongodb.org/zips.json"), file);
            }
        }
        DBCollection zips = getDb().getCollection("zips");
        if (zips.count() == 0) {
            new ProcessExecutor().command(MONGO_IMPORT,
                                          "--db", getDb().getName(),
                                          "--collection", "zipcodes",
                                          "--file", file.getAbsolutePath())
                                 .redirectError(System.err)
                                 .execute();
        }
    }

    @Test
    public void populationsAbove10M() throws IOException, TimeoutException, InterruptedException {
        Assume.assumeTrue(new File(MONGO_IMPORT).exists());
        installSampleData();
        Query<Object> query = getDs().getQueryFactory().createQuery(getDs());

        AggregationPipeline pipeline
            = getDs().createAggregation(City.class)
                     .group("state", grouping("totalPop", sum("pop")))
                     .match(query.field("totalPop").greaterThanOrEq(10000000));


        validate(pipeline.aggregate(Population.class), "CA", 29754890);
        validate(pipeline.aggregate(Population.class), "OH", 10846517);
    }

    @Test
    public void smallestAndLargestCities() throws InterruptedException, TimeoutException, IOException {
        Assume.assumeTrue(new File(MONGO_IMPORT).exists());
        installSampleData();
        getMorphia().mapPackage(getClass().getPackage().getName());
        AggregationPipeline pipeline = getDs().createAggregation(City.class)

                                              .group(id(grouping("state"), grouping("city")), grouping("pop", sum("pop")))

                                              .sort(ascending("pop"))

                                              .group("_id.state",
                                                     grouping("biggestCity", last("_id.city")),
                                                     grouping("biggestPop", last("pop")),
                                                     grouping("smallestCity", first("_id.city")),
                                                     grouping("smallestPop", first("pop")))

                                              .project(projection("_id").suppress(),
                                                       projection("state", "_id"),
                                                       projection("biggestCity",
                                                                  projection("name", "biggestCity"),
                                                                  projection("pop", "biggestPop")),
                                                       projection("smallestCity",
                                                                  projection("name", "smallestCity"),
                                                                  projection("pop", "smallestPop")));

        Iterator<State> iterator = pipeline.aggregate(State.class);
        try {
            Map<String, State> states = new HashMap<String, State>();
            while (iterator.hasNext()) {
                State state = iterator.next();
                states.put(state.getState(), state);
            }

            State state = states.get("SD");

            Assert.assertEquals("SIOUX FALLS", state.getBiggest().getName());
            Assert.assertEquals(102046, state.getBiggest().getPopulation().longValue());

            Assert.assertEquals("ZEONA", state.getSmallest().getName());
            Assert.assertEquals(8, state.getSmallest().getPopulation().longValue());
        } finally {

            ((MorphiaIterator) iterator).close();
        }
    }

    private void download(final URL url, final File file) throws IOException {
        LOG.info("Downloading zip data set to " + file);
        InputStream inputStream = url.openStream();
        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            byte[] read = new byte[49152];
            int count;
            while ((count = inputStream.read(read)) != -1) {
                outputStream.write(read, 0, count);
            }
        } finally {
            inputStream.close();
            outputStream.close();
        }
    }

    private void validate(final Iterator<Population> iterator, final String state, final long value) {
        boolean found = false;
        try {
            while (iterator.hasNext()) {
                Population population = iterator.next();

                if (population.getState().equals(state)) {
                    found = true;
                    Assert.assertEquals(new Long(value), population.getPopulation());
                }
                LOG.debug("population = " + population);
            }
            Assert.assertTrue("Should have found " + state, found);
        } finally {
            ((MorphiaIterator) iterator).close();
        }
    }

}
