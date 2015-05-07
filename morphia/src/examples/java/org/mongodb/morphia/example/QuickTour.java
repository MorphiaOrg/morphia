package org.mongodb.morphia.example;

import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;

import java.net.UnknownHostException;
import java.util.List;

public final class QuickTour {
    private QuickTour() {
    }

    public static void main(final String[] args) throws UnknownHostException {
        final Morphia morphia = new Morphia();

        // tell morphia where to find your classes
        // can be called multiple times with different packages or classes
        morphia.mapPackage("org.mongodb.morphia.example");

        // create the Datastore connecting to the database running on the default port on the local host
        final Datastore datastore = morphia.createDatastore(new MongoClient(), "morphia_example");
        datastore.ensureIndexes();

        final Employee employee = new Employee("Elmer Fudd", 50000.0);
        datastore.save(employee);
    }
}

@Entity("employees")
@Indexes(
            @Index(value = "salary", fields = @Field("salary"))
)
class Employee {
    @Id
    private ObjectId id;
    private String name;
    @Reference
    private Employee manager;
    @Reference
    private List<Employee> directReports;
    @Property("wage")
    private Double salary;

    public Employee() {
    }

    public Employee(final String name, final Double salary) {
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