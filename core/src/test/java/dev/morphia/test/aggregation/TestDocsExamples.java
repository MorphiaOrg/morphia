package dev.morphia.test.aggregation;

import dev.morphia.MorphiaDatastore;
import dev.morphia.test.TestBase;
import dev.morphia.test.aggregation.model.Author;
import dev.morphia.test.aggregation.model.Book;

import org.bson.Document;
import org.testng.annotations.Test;

import static com.mongodb.client.model.MergeOptions.WhenMatched.REPLACE;
import static com.mongodb.client.model.MergeOptions.WhenNotMatched.INSERT;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Merge.merge;
import static dev.morphia.aggregation.stages.Out.out;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestDocsExamples extends TestBase {
    @Test
    public void testBasic() {
        MorphiaDatastore datastore = getDs();
// @formatter:off
// tag::basic[]
var aggregate = datastore.aggregate(Book.class).pipeline(
    group(id("author"))
      .field("books", push(field("title"))),
    sort()
       .ascending("name"))
    .execute(Author.class);
// end::basic[]
// @formatter:on
    }

    @Test
    public void testMerge() {
        var datastore = getDs();
        var aggregation = datastore.aggregate(Document.class);
// @formatter:off
// tag::merge[]
aggregation.pipeline(
    group(id()
        .field("fiscal_year", "$fiscal_year")
        .field("dept", "$dept"))
        .field("salaries", sum("$salary")),
    merge("reporting", "budgets")
        .on("_id")
        .whenMatched(REPLACE)
        .whenNotMatched(INSERT))
    .execute();
// end::merge[]
// @formatter:on
    }

    @Test
    public void testOut() {
        MorphiaDatastore datastore = getDs();
// @formatter:off
// tag::out[]
datastore.aggregate(Book.class).pipeline(
    group(id("$author"))
        .field("books",
            push().single("$title")),
    out(Author.class))
    .execute();
// end::out[]
// @formatter:on
    }

}
