package org.mongodb.morphia.aggregation;

import com.mongodb.DBCollection;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
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

import static org.mongodb.morphia.aggregation.Group.field;
import static org.mongodb.morphia.aggregation.Group.sum;

public class ZipCodeDataSetTest extends TestBase {
    private static final Logr LOG = MorphiaLoggerFactory.get(ZipCodeDataSetTest.class);

    private static final File download = new File(System.getProperty("java.io.tmpdir"), "zips.json");

    public void installSampleData() throws IOException, TimeoutException, InterruptedException {
        LOG.info("ZipCodeDataSetTest.installSampleData");
        if (!download.exists()) {
            download();
        }
        DBCollection zips = getDb().getCollection("zips");
        if (zips.count() == 0) {
            new ProcessExecutor().command("/usr/local/bin/mongoimport",
                                          "--db", getDb().getName(),
                                          "--collection", "zipcodes",
                                          "--file", download.getAbsolutePath())
                .redirectError(System.err)
                     //                                 .redirectOutput(System.out)
                .execute();
        }
    }

    private void download() throws IOException {
        URL url = new URL("http://media.mongodb.org/zips.json");
        InputStream inputStream = url.openStream();
        FileOutputStream outputStream = new FileOutputStream(download);
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
                                                   .match("state", field("totalPop", sum("pop")))
                                                   .aggregate();
        for (Population population : pipeline) {
            System.out.println("population = " + population);
        }
        pipeline.close();
    }

    @Entity(value = "zipcodes", noClassnameStored = true)
    private static class City {
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
        String state;
        Long population;

        public Long getPopulation() {
            return population;
        }

        public void setPopulation(final Long population) {
            this.population = population;
        }
    }
}
