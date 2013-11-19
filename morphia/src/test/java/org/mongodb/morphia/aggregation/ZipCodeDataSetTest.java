package org.mongodb.morphia.aggregation;

import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.AlsoLoad;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.logging.Logr;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.query.MorphiaIterator;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.mongodb.morphia.aggregation.Group.average;
import static org.mongodb.morphia.aggregation.Group.first;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Group.id;
import static org.mongodb.morphia.aggregation.Group.last;
import static org.mongodb.morphia.aggregation.Group.sum;
import static org.mongodb.morphia.aggregation.Matcher.match;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static org.mongodb.morphia.aggregation.Sort.ascending;

/**
 * These tests recreate the example zip code data set aggregations as found in the official documentation.
 * 
 * @see <a href="http://docs.mongodb.org/manual/tutorial/aggregation-zip-code-data-set/">Aggregation with the Zip Code Data Set</a>
 */
public class ZipCodeDataSetTest extends TestBase {
    private static final Logr LOG = MorphiaLoggerFactory.get(ZipCodeDataSetTest.class);

    public void installSampleData() throws IOException, TimeoutException, InterruptedException {
        File file = new File(System.getProperty("java.io.tmpdir"), "zips.json");
        if (!file.exists()) {
            download(new URL("http://media.mongodb.org/zips.json"), file);
        }
        DBCollection zips = getDb().getCollection("zips");
        if (zips.count() == 0) {
            new ProcessExecutor().command("/usr/local/bin/mongoimport",
                                          "--db", getDb().getName(),
                                          "--collection", "zipcodes",
                                          "--file", file.getAbsolutePath())
                                 .redirectError(System.err)
                                 .execute();
        }
    }

    private void download(final URL url, final File file) throws IOException {
        LOG.info("Downloading zip data set");
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
        installSampleData();
        AggregationPipeline<City, Population> pipeline
            = getDs().createAggregation(City.class, Population.class)
                 .group("state", grouping("totalPop", sum("pop")))
                 .match(match("totalPop").greaterThanEqual(10000000));
        validate(pipeline.aggregate(), "CA", 29760021);
        validate(pipeline.aggregate(), "OH", 10847115);
    }

    @Test
    public void averageCitySizeByState() throws InterruptedException, TimeoutException, IOException {
        installSampleData();
        AggregationPipeline<City, Population> pipeline = getDs().createAggregation(City.class, Population.class)
                                                             .group(id(grouping("state"), grouping("city")), grouping("pop", sum("pop")))
                                                             .group("_id.state", grouping("avgCityPop", average("pop")));
        validate(pipeline.aggregate(), "MN", 5335);
    }

    @Test
    public void smallestAndLargestCities() throws InterruptedException, TimeoutException, IOException {
        installSampleData();
        getMorphia().mapPackage(getClass().getPackage().getName());
        AggregationPipeline<City, State> pipeline = getDs().createAggregation(City.class, State.class)
                                                        .group(id(grouping("state"), grouping("city")), grouping("pop", sum("pop")))
                                                        .sort(ascending("pop"))
                                                        .group("_id.state",
                                                               grouping("biggestCity", last("_id.city")),
                                                               grouping("biggestPop", last("pop")),
                                                               grouping("smallestCity", first("_id.city")),
                                                               grouping("smallestPop", first("pop"))
                                                              )
                                                        .project(projection("_id").suppress(),
                                                                 projection("state", "_id"),
                                                                 projection("biggestCity",
                                                                            projection("name", "biggestCity"),
                                                                            projection("pop", "biggestPop")
                                                                           ),
                                                                 projection("smallestCity",
                                                                            projection("name", "smallestCity"),
                                                                            projection("pop", "smallestPop")
                                                                           )
                                                                );

        MorphiaIterator<State, State> iterator = pipeline.aggregate();
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
        iterator.close();
    }

    private void validate(final MorphiaIterator<Population, Population> pipeline, final String state, final long value) {
        boolean found = false;
        for (Population population : pipeline) {
            if (population.getState().equals(state)) {
                found = true;
                Assert.assertEquals(new Long(value), population.getPopulation());
            }
            LOG.debug("population = " + population);
        }
        Assert.assertTrue("Should have found " + state, found);
        pipeline.close();
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

        private String getId() {
            return id;
        }

        private void setId(final String id) {
            this.id = id;
        }

        private double[] getLocation() {
            return location;
        }

        private void setLocation(final double[] location) {
            this.location = location;
        }

        private String getName() {
            return name;
        }

        private void setName(final String name) {
            this.name = name;
        }

        private Double getPopulation() {
            return population;
        }

        private void setPopulation(final Double population) {
            this.population = population;
        }

        private String getState() {
            return state;
        }

        private void setState(final String state) {
            this.state = state;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("City{");
            sb.append("id='").append(id).append('\'');
            sb.append(", name='").append(name).append('\'');
            sb.append(", location=").append(Arrays.toString(location));
            sb.append(", population=").append(population);
            sb.append(", state='").append(state).append('\'');
            sb.append('}');
            return sb.toString();
        }

    }

    @Entity
    public static class Population {
        @Id
        private String state;
        @Property("totalPop")
        @AlsoLoad("avgCityPop")
        private Long population;

        public String getState() {
            return state;
        }

        public Long getPopulation() {
            return population;
        }

        public void setPopulation(final Long population) {
            this.population = population;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Population{");
            sb.append("population=").append(population);
            sb.append(", state='").append(state).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    @Embedded
    public static class CityPopulation {
        @Property("name")
        private String name;
        @Property("pop")
        private Long population;

        public String getName() {
            return name;
        }

        public Long getPopulation() {
            return population;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("CityPopulation{");
            sb.append("name='").append(name).append('\'');
            sb.append(", population=").append(population);
            sb.append('}');
            return sb.toString();
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
            final StringBuilder sb = new StringBuilder("State{");
            sb.append("state='").append(state).append('\'');
            sb.append(", biggest=").append(biggest);
            sb.append(", smallest=").append(smallest);
            sb.append('}');
            return sb.toString();
        }
    }
}
