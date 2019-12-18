package dev.morphia.example;

import com.mongodb.client.MongoClients;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.query.Query;
import org.bson.types.ObjectId;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used in the Quick Tour documentation and is used to demonstrate various Morphia features.
 */
public final class QuickTour {
    private QuickTour() {
    }

    public static void main(final String[] args) {
        final Datastore datastore = Morphia.createDatastore(MongoClients.create(), "morphia_example");

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

        Assert.assertEquals(3, employees);

        long underpaid = datastore.find(Employee.class)
                                  .filter("salary <=", 30000)
                                  .count();
        Assert.assertEquals(1, underpaid);

        underpaid = datastore.find(Employee.class)
                             .field("salary").lessThanOrEq(30000)
                             .count();
        Assert.assertEquals(1, underpaid);

        final Query<Employee> underPaidQuery = datastore.find(Employee.class)
                                                        .filter("salary <=", 30000);
        final UpdateResult results = underPaidQuery.update()
                                                   .inc("salary", 10000)
                                                   .execute();

        Assert.assertEquals(1, results.getModifiedCount());

        datastore.find(Employee.class)
                 .filter("salary >", 100000)
                 .delete();
    }
}

@Entity("employees")
@Indexes(@Index(options = @IndexOptions(name = "salary"), fields = @Field("salary")))
class Employee {
    @Id
    private ObjectId id;
    private String name;
    private Integer age;
    @Reference
    private Employee manager;
    @Reference
    private List<Employee> directReports = new ArrayList<>();
    @Property("wage")
    private Double salary;

    Employee() {
    }

    Employee(final String name, final Double salary) {
        this.name = name;
        this.salary = salary;
    }

    public List<Employee> getDirectReports() {
        return directReports;
    }

    public void setDirectReports(final List<Employee> directReports) {
        this.directReports = directReports;
    }

    public ObjectId getId() {
        return id;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(final Employee manager) {
        this.manager = manager;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(final Double salary) {
        this.salary = salary;
    }
}
