package dev.morphia.example;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.result.UpdateResult;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.query.Query;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;

import static com.mongodb.MongoClientSettings.builder;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.lte;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static org.bson.UuidRepresentation.STANDARD;

/**
 * This class is used in the Quick Tour documentation and is used to demonstrate various Morphia features.
 */
public class QuickTourTest {
    private static MongoDBContainer mongoDBContainer;

    private static String connectionString;

    private static MongoClient mongoClient;

    @BeforeAll
    public static void start() {
        mongoDBContainer = new MongoDBContainer("mongo:7");
        mongoDBContainer.start();
        connectionString = mongoDBContainer.getReplicaSetUrl("morphia-demo");
        mongoClient = MongoClients.create(builder()
                .uuidRepresentation(STANDARD)
                .applyConnectionString(new ConnectionString(connectionString))
                .build());
    }

    @AfterAll
    public static void stop() {
        mongoClient.close();
        mongoDBContainer.stop();
    }

    @Test
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

        Assertions.assertEquals(3, employees);

        long underpaid = datastore.find(Employee.class)
                .filter(lte("salary", 30000))
                .count();
        Assertions.assertEquals(1, underpaid);

        final Query<Employee> underPaidQuery = datastore.find(Employee.class)
                .filter(lte("salary", 30000));
        final UpdateResult results = underPaidQuery.update(inc("salary", 10000));

        Assertions.assertEquals(1, results.getModifiedCount());

        datastore.find(Employee.class)
                .filter(gt("salary", 100000))
                .findAndDelete();
    }
}
