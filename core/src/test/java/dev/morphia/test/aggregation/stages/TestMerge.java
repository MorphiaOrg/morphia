package dev.morphia.test.aggregation.stages;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.morphia.InsertOneOptions;
import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Salary;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import static com.mongodb.client.model.MergeOptions.WhenMatched.FAIL;
import static com.mongodb.client.model.MergeOptions.WhenMatched.MERGE;
import static com.mongodb.client.model.MergeOptions.WhenMatched.REPLACE;
import static com.mongodb.client.model.MergeOptions.WhenNotMatched.INSERT;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Merge.into;
import static dev.morphia.aggregation.stages.Merge.merge;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lt;

public class TestMerge extends AggregationTest {
    public TestMerge() {
        skipDataCheck();
    }

    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                group(id()
                        .field("fiscal_year", field("fiscal_year"))
                        .field("dept", field("dept")))
                        .field("salaries", sum(field("salary"))),
                merge("reporting", "budgets")
                        .on("_id")
                        .whenMatched(REPLACE)
                        .whenNotMatched(INSERT)));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(gte("fiscal_year", 2019)),
                group(id()
                        .field("fiscal_year", field("fiscal_year"))
                        .field("dept", field("dept")))
                        .field("salaries", sum(field("salary"))),
                merge("reporting", "budgets")
                        .on("_id")
                        .whenMatched(REPLACE)
                        .whenNotMatched(INSERT)));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(eq("fiscal_year", 2019)),
                group(id()
                        .field("fiscal_year", field("fiscal_year"))
                        .field("dept", field("dept")))
                        .field("employees", push(field("employee"))),
                project()
                        .suppressId()
                        .include("dept", field("_id.dept"))
                        .include("fiscal_year", field("_id.fiscal_year"))
                        .include("employees"),
                merge("reporting", "orgArchive")
                        .on("dept", "fiscal_year")
                        .whenMatched(FAIL)));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                group()
                        .field("_id", field("quarter"))
                        .field("purchased", sum(field("qty"))),
                merge("quarterlyreport")
                        .on("_id")
                        .whenMatched(MERGE)
                        .whenNotMatched(INSERT)));
    }

    @Test
    public void testExample5() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(
                        gte("date", LocalDate.of(2019, Month.MAY, 7)),
                        lt("date", LocalDate.of(2019, Month.MAY, 8))),
                project()
                        .include("_id", dateToString()
                                .date(field("date"))
                                .format("%Y-%m"))
                        .include("thumbsup")
                        .include("thumbsdown"),
                merge("monthlytotals")
                        .on("_id")
                        .whenMatched(
                                addFields()
                                        .field("thumbsup", add(field("thumbsup"), value("$$new.thumbsup")))
                                        .field("thumbsdown", add(field("thumbsdown"), value("$$new.thumbsdown"))))
                        .whenNotMatched(INSERT)));
    }

    @Test
    public void testExample6() {
        // a bit of arcane example Morphia's not well-suited for
    }

    @Test
    public void testMerge() {
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

    @Test
    public void testMergeWithUnsetMissing() {
        GenericEntity entity = new GenericEntity();
        entity.strings = Arrays.asList("Test1", null, "Test2");
        MorphiaDatastore ds = getDs();
        ds.save(entity);

        entity.strings = Arrays.asList("Test1", null, "Test2");
        ds.merge(entity, new InsertOneOptions().unsetMissing(true));

    }

    @Entity
    private static class GenericEntity {
        @Id
        private ObjectId id;

        private List<String> strings = new ArrayList<>();
    }
}
