package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.stages.Group;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Salary;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.List;

import static com.mongodb.client.model.MergeOptions.WhenMatched.REPLACE;
import static com.mongodb.client.model.MergeOptions.WhenNotMatched.INSERT;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Merge.into;

public class TestMerge extends AggregationTest {
    @Test
    public void testMerge() {
        checkMinServerVersion(4.2);

        insert("salaries", parseDocs(
                "{ '_id' : 1, employee: 'Ant', dept: 'A', salary: 100000, fiscal_year: 2017 }",
                "{ '_id' : 2, employee: 'Bee', dept: 'A', salary: 120000, fiscal_year: 2017 }",
                "{ '_id' : 3, employee: 'Cat', dept: 'Z', salary: 115000, fiscal_year: 2017 }",
                "{ '_id' : 4, employee: 'Ant', dept: 'A', salary: 115000, fiscal_year: 2018 }",
                "{ '_id' : 5, employee: 'Bee', dept: 'Z', salary: 145000, fiscal_year: 2018 }",
                "{ '_id' : 6, employee: 'Cat', dept: 'Z', salary: 135000, fiscal_year: 2018 }",
                "{ '_id' : 7, employee: 'Gecko', dept: 'A', salary: 100000, fiscal_year: 2018 }",
                "{ '_id' : 8, employee: 'Ant', dept: 'A', salary: 125000, fiscal_year: 2019 }",
                "{ '_id' : 9, employee: 'Bee', dept: 'Z', salary: 160000, fiscal_year: 2019 }",
                "{ '_id' : 10, employee: 'Cat', dept: 'Z', salary: 150000, fiscal_year: 2019 }"));

        getDs().aggregate(Salary.class)
                .group(Group.group(id()
                        .field("fiscal_year")
                        .field("dept"))
                        .field("salaries", sum(field("salary"))))
                .merge(into("budgets")
                        .on("_id")
                        .whenMatched(REPLACE)
                        .whenNotMatched(INSERT));
        List<Document> actual = getDs().find("budgets", Document.class).iterator().toList();

        List<Document> expected = parseDocs(
                "{ '_id' : { 'fiscal_year' : 2017, 'dept' : 'A' }, 'salaries' : 220000 }",
                "{ '_id' : { 'fiscal_year' : 2017, 'dept' : 'Z' }, 'salaries' : 115000 }",
                "{ '_id' : { 'fiscal_year' : 2018, 'dept' : 'A' }, 'salaries' : 215000 }",
                "{ '_id' : { 'fiscal_year' : 2018, 'dept' : 'Z' }, 'salaries' : 280000 }",
                "{ '_id' : { 'fiscal_year' : 2019, 'dept' : 'A' }, 'salaries' : 125000 }",
                "{ '_id' : { 'fiscal_year' : 2019, 'dept' : 'Z' }, 'salaries' : 310000 }");

        assertDocumentEquals(actual, expected);
    }

}
