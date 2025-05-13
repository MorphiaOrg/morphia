package dev.morphia.test.aggregation;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.AggregationOptions;
import dev.morphia.test.TestBase;
import dev.morphia.test.aggregation.model.Author;
import dev.morphia.test.aggregation.model.Book;

import org.testng.annotations.Test;

import static com.mongodb.client.model.MergeOptions.WhenMatched.REPLACE;
import static com.mongodb.client.model.MergeOptions.WhenNotMatched.INSERT;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Merge.merge;
import static dev.morphia.aggregation.stages.Out.out;
import static dev.morphia.aggregation.stages.Sort.sort;

/**
 * This file gets copied in to the docs directory for inclusion in the examples.
 */
public class TestDocsExamples extends TestBase {
    @Test
    public void testBasic() {
        MorphiaDatastore datastore = getDs();
// @formatter:off
// tag::basic[]
datastore.aggregate(Book.class, Author.class).pipeline(
    group(id("author"))
      .field("books", push("$title")),
    sort()
       .ascending("name"))
    .iterator();
// end::basic[]
// @formatter:on
    }

    @Test
    public void testMerge() {
        var datastore = getDs();
// @formatter:off
// tag::merge[]
var aggregation = datastore.aggregate(new AggregationOptions().collection("some collection"));
aggregation.pipeline(
    group(id()
        .field("fiscal_year", "$fiscal_year")
        .field("dept", "$dept"))
        .field("salaries", sum("$salary")),
    merge("reporting", "budgets")
        .on("_id")
        .whenMatched(REPLACE)
        .whenNotMatched(INSERT))
    .iterator();
// end::merge[]
// @formatter:on
    }

    @Test
    public void testOut() {
        MorphiaDatastore datastore = getDs();
// @formatter:off
// tag::out[]
datastore.aggregate(Book.class, Author.class)
         .pipeline(
             group(id("$author"))
                 .field("books", push().single("$title")),
             out(Author.class))
    .iterator();
// end::out[]
// @formatter:on
    }

}
