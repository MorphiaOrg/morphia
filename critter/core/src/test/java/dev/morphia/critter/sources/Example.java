package dev.morphia.critter.sources;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.client.model.CollationCaseFirst;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.mapping.lifecycle.EntityListenerAdapter;

import org.bson.types.ObjectId;

@Entity("examples")
@EntityListeners(EntityListenerAdapter.class)
@Indexes(@Index(fields = @Field(value = "name", weight = 42), options = @IndexOptions(partialFilter = "partial filter", collation = @Collation(caseFirst = CollationCaseFirst.LOWER))))
public class Example {
    @Id
    private ObjectId id;
    @Property(value = "myName")
    @AlsoLoad({ "name1", "name2" })
    private String name;

    @Transient
    @Reference
    private final int[] temp = new int[0];

    private Map<String, Example> map;

    private List<Map<String, Example>> list;

    private Set<Map<String, Example>> set;

    @Reference(idOnly = true)
    private int age = 21;

    @Property
    private Long salary = 2L;

    public String __readNameTemplate() {
        return name;
    }

    public void __writeNameTemplate(final String name) {
        this.name = name;
    }

    public int __readAgeTemplate() {
        return age;
    }

    public void __writeAgeTemplate(final int age) {
        this.age = age;
    }

    public Long __readSalaryTemplate() {
        return salary;
    }

    public void __writeSalaryTemplate(final Long salary) {
        this.salary = salary;
    }

    @Override
    public String toString() {
        return "Example{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", salary=" + salary +
                '}';
    }
}
