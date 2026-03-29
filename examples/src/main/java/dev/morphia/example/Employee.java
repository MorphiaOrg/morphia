package dev.morphia.example;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;

import org.bson.types.ObjectId;

/**
 * Example entity representing an employee with manager/report relationships and salary.
 */
@Entity("employees")
@Indexes(@Index(options = @IndexOptions(name = "salary"), fields = @Field("salary")))
public class Employee {
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

    /**
     * Creates a new Employee with no initial values.
     */
    public Employee() {
    }

    /**
     * Creates a new Employee with the given name and salary.
     *
     * @param name   the employee's name
     * @param salary the employee's salary
     */
    public Employee(String name, Double salary) {
        this.name = name;
        this.salary = salary;
    }

    /**
     * Returns the list of employees who report directly to this employee.
     *
     * @return the direct reports
     */
    public List<Employee> getDirectReports() {
        return directReports;
    }

    /**
     * Sets the list of employees who report directly to this employee.
     *
     * @param directReports the direct reports
     */
    public void setDirectReports(List<Employee> directReports) {
        this.directReports = directReports;
    }

    /**
     * Returns the unique identifier for this employee.
     *
     * @return the employee ID
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Returns the manager of this employee.
     *
     * @return the manager
     */
    public Employee getManager() {
        return manager;
    }

    /**
     * Sets the manager of this employee.
     *
     * @param manager the manager
     */
    public void setManager(Employee manager) {
        this.manager = manager;
    }

    /**
     * Returns the name of this employee.
     *
     * @return the employee's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this employee.
     *
     * @param name the employee's name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the salary of this employee.
     *
     * @return the employee's salary
     */
    public Double getSalary() {
        return salary;
    }

    /**
     * Sets the salary of this employee.
     *
     * @param salary the employee's salary
     */
    public void setSalary(Double salary) {
        this.salary = salary;
    }
}
