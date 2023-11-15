package dev.morphia.example;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.result.UpdateResult;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.query.Query;

import org.testcontainers.containers.MongoDBContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.mongodb.MongoClientSettings.builder;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.lte;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static org.bson.UuidRepresentation.STANDARD;
import static org.testng.Assert.assertEquals;

/**
 * This class is used in the Quick Tour documentation and is used to demonstrate various Morphia features.
 */
@Test
public class QuickTourTest {
    private static MongoDBContainer mongoDBContainer;

    private static String connectionString;

    private static MongoClient mongoClient;

    @BeforeClass
    public static void start() {
        mongoDBContainer = new MongoDBContainer("mongo:7");
        mongoDBContainer.start();
        connectionString = mongoDBContainer.getReplicaSetUrl("morphia-demo");
        mongoClient = MongoClients.create(builder()
                .uuidRepresentation(STANDARD)
                .applyConnectionString(new ConnectionString(connectionString))
                .build());
    }

    @AfterClass
    public void stop() {
        mongoClient.close();
        mongoDBContainer.stop();
    }

    public void demo() {
        final Datastore datastore = Morphia.createDatastore(mongoClient);
        datastore.getDatabase().drop();

        final Employee elmer = datastore.save(new Employee("Elmer Fudd", 50000.0));
        final Employee daffy = datastore.save(new Employee("Daffy Duck", 40000.0));
        final Employee pepe = datastore.save(new Employee("Pepé Le Pew", 25000.0));

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
        final UpdateResult results = underPaidQuery.update(inc("salary", 10000));

        assertEquals(results.getModifiedCount(), 1);

        datastore.find(Employee.class)
                .filter(gt("salary", 100000))
                .findAndDelete();
    }
}
