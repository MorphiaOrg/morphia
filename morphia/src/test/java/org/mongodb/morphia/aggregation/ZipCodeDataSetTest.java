package org.mongodb.morphia.aggregation;

import com.mongodb.DBCollection;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.AlsoLoad;
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
import java.util.concurrent.TimeoutException;

import static org.mongodb.morphia.aggregation.Group.average;
import static org.mongodb.morphia.aggregation.Group.field;
import static org.mongodb.morphia.aggregation.Group.sum;
import static org.mongodb.morphia.aggregation.Matcher.match;

public class ZipCodeDataSetTest extends TestBase {
    private static final Logr LOG = MorphiaLoggerFactory.get(ZipCodeDataSetTest.class);

    public void installSampleData() throws IOException, TimeoutException, InterruptedException {
        LOG.info("ZipCodeDataSetTest.installSampleData");
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
                     //                                 .redirectOutput(System.out)
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
        MorphiaIterator<Population, Population> pipeline = getDs().createAggregation(City.class, Population.class)
                                                               .group("state", field("totalPop", sum("pop")))
                                                               .match(match("totalPop").greaterThanEqual(10000000))
                                                               .aggregate();
        boolean caFound = false;
        for (Population population : pipeline) {
            if (population.getState().equals("CA")) {
                caFound = true;
                Assert.assertEquals(new Long(29760021), population.getPopulation());
            }
            LOG.debug("population = " + population);
        }
        Assert.assertTrue("Should have found CA", caFound);

        pipeline.close();
    }

    @Test
    public void averageCitySizeByState() throws InterruptedException, TimeoutException, IOException {
        installSampleData();
        MorphiaIterator<Population, Population> pipeline = getDs().createAggregation(City.class, Population.class)
                                                               .group(Group.id(field("state"), field("city")), field("pop", sum("pop")))
                                                               .group("_id.state", field("avgCityPop", average("pop")))
                                                               .aggregate();
        boolean found = false;
        for (Population population : pipeline) {
            if (population.getState().equals("MN")) {
                found = true;
                Assert.assertEquals(new Long(5335), population.getPopulation());
            }
            LOG.info("population = " + population);
        }
        Assert.assertTrue("Should have found MN", found);

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
        private double population;
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

        private double getPopulation() {
            return population;
        }

        private void setPopulation(final double population) {
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
}
