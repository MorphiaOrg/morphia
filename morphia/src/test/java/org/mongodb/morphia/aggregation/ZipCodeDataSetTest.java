package org.mongodb.morphia.aggregation;

import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.AlsoLoad;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
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
import java.util.Arrays;
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
 * @see <a href="http://docs.mongodb.org/manual/tutorial/aggregation-zip-code-data-set/">Aggregation with the Zip Code Data Set</a>
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
    public void averageCitySizeByState() throws InterruptedException, TimeoutException, IOException {
        Assume.assumeTrue(new File(MONGO_IMPORT).exists());
        installSampleData();
        AggregationPipeline pipeline = getDs().createAggregation(City.class)
                                              .group(id(grouping("state"), grouping("city")), grouping("pop", sum("pop")))
                                              .group("_id.state", grouping("avgCityPop", average("pop")));
        validate(pipeline.aggregate(Population.class), "MN", 5372);
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

            Assert.assertEquals("SIOUX FALLS", state.getBiggest().name);
            Assert.assertEquals(102046, state.getBiggest().population.longValue());

            Assert.assertEquals("ZEONA", state.getSmallest().name);
            Assert.assertEquals(8, state.getSmallest().population.longValue());
        } finally {

            ((MorphiaIterator) iterator).close();
        }
    }

    private void validate(final Iterator<Population> iterator, final String state, final long value) {
        boolean found = false;
        try {
            while (iterator.hasNext()) {
                Population population = iterator.next();

                if (population.state.equals(state)) {
                    found = true;
                    Assert.assertEquals(new Long(value), population.population);
                }
                LOG.debug("population = " + population);
            }
            Assert.assertTrue("Should have found " + state, found);
        } finally {
            ((MorphiaIterator) iterator).close();
        }
    }

    @Entity(value = "zipcodes", noClassnameStored = true)
    public static final class City {
        @Id
        private String id;
        @Property("city")
        private String name;
        @Property("loc")
        private double[] location;
        @Property("pop")
        private Double population;
        @Property("state")
        private String state;

        private City() {
        }

        @Override
        public String toString() {
            return format("City{id='%s', name='%s', location=%s, population=%s, state='%s'}", id, name, Arrays.toString(location),
                          population, state);
        }
    }

    @Entity
    public static class Population {
        @Id
        private String state;
        @Property("totalPop")
        @AlsoLoad("avgCityPop")
        private Long population;

        @Override
        public String toString() {
            return String.format("Population{population=%d, state='%s'}", population, state);
        }
    }

    @Embedded
    public static class CityPopulation {
        @Property("name")
        private String name;
        @Property("pop")
        private Long population;

        @Override
        public String toString() {
            return String.format("CityPopulation{name='%s', population=%d}", name, population);
        }
    }

    @Entity
    public static class State {
        @Id
        private ObjectId id;
        @Property("state")
        private String state;
        @Embedded("biggestCity")
        private CityPopulation biggest;
        @Embedded("smallestCity")
        private CityPopulation smallest;

        public CityPopulation getBiggest() {
            return biggest;
        }

        public CityPopulation getSmallest() {
            return smallest;
        }

        public String getState() {
            return state;
        }

        @Override
        public String toString() {
            return String.format("State{state='%s', biggest=%s, smallest=%s}", state, biggest, smallest);
        }
    }
}
