package dev.morphia.example;

import com.antwerkz.bottlerocket.clusters.MongoCluster;
import com.antwerkz.bottlerocket.clusters.ReplicaSet;
import com.antwerkz.bottlerocket.clusters.SingleNode;
import com.antwerkz.bottlerocket.configuration.types.Verbosity;
import com.github.zafarkhaja.semver.Version;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.query.Query;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.lte;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static org.testng.Assert.assertEquals;

/**
 * This class is used in the Quick Tour documentation and is used to demonstrate various Morphia features.
 */
public class QuickTourTest {
    private static MongoClient mongoClient;
    private static MongoCluster cluster;

    @BeforeClass
    public static void setupDb() {
        String mongodb = System.getProperty("MONGODB");
        Builder builder = MongoClientSettings.builder();

        if (mongodb != null) {
            File mongodbRoot = new File("target/mongo");
            try {
                FileUtils.deleteDirectory(mongodbRoot);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            Version version = Version.valueOf(mongodb);
            cluster = version.lessThan(Version.valueOf("4.0.0"))
                    ? new SingleNode(version, "morphia_test", mongodbRoot)
                    : new ReplicaSet(version, "morphia_test", mongodbRoot);

            cluster.configure(c -> {
                c.systemLog(s -> {
                    s.setTraceAllExceptions(true);
                    s.setVerbosity(Verbosity.FIVE);
                    return null;
                });
                return null;
            });
            cluster.clean();
            cluster.start();
            mongoClient = cluster.getClient(builder);
        } else {
            mongoClient = MongoClients.create(builder.build());
        }
    }

    @AfterClass
    public static void stopDb() {
        if (cluster != null) {
            cluster.shutdown();
        }
    }

    @Test
    public void demo() {
        final Datastore datastore = Morphia.createDatastore(mongoClient, "morphia_example");

        // tell morphia where to find your classes
        // can be called multiple times with different packages or classes
        datastore.getMapper().mapPackage("dev.morphia.example");

        // create the Datastore connecting to the database running on the default port on the local host
        datastore.getDatabase().drop();
        datastore.ensureIndexes();

        final Employee elmer = new Employee("Elmer Fudd", 50000.0);
        datastore.save(elmer);

        final Employee daffy = new Employee("Daffy Duck", 40000.0);
        datastore.save(daffy);

        final Employee pepe = new Employee("Pepé Le Pew", 25000.0);
        datastore.save(pepe);

        elmer.getDirectReports().add(daffy);
        elmer.getDirectReports().add(pepe);

        datastore.save(elmer);

        Query<Employee> query = datastore.find(Employee.class);
        final long employees = query.count();

        assertEquals(employees, 3);

        long underpaid = datastore.find(Employee.class)
                .filter(lte("salary", 30000))
                .count();
        assertEquals(underpaid, 1);

        final Query<Employee> underPaidQuery = datastore.find(Employee.class)
                .filter(lte("salary", 30000));
        final UpdateResult results = underPaidQuery.update(inc("salary", 10000))
                .execute();

        assertEquals(results.getModifiedCount(), 1);

        datastore.find(Employee.class)
                .filter(gt("salary", 100000))
                .findAndDelete();
    }
}
