package dev.morphia.test.aggregation.stages;

import dev.morphia.test.aggregation.AggregationTest;
import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.size;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.expressions.SystemVariables.DESCEND;
import static dev.morphia.aggregation.expressions.SystemVariables.PRUNE;
import static dev.morphia.aggregation.stages.Redact.redact;
import static dev.morphia.query.filters.Filters.eq;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;

public class TestRedact extends AggregationTest {
    @Test
    public void testRedact() {
        Document document = parse(
                "{ _id: 1, title: '123 Department Report', tags: [ 'G', 'STLW' ],year: 2014, subsections: [{ subtitle: 'Section 1: Overview',"
                        + " tags: [ 'SI', 'G' ],content:  'Section 1: This is the content of section 1.' },{ subtitle: 'Section 2: Analysis', tags: "
                        + "[ 'STLW' ], content: 'Section 2: This is the content of section 2.' },{ subtitle: 'Section 3: Budgeting', tags: [ 'TK' ],"
                        + "content: { text: 'Section 3: This is the content of section3.', tags: [ 'HCS' ]} }]}");

        getDatabase().getCollection("forecasts").insertOne(document);

        Document actual = getDs().aggregate("forecasts")
                .match(eq("year", 2014))
                .redact(redact(
                        condition(
                                gt(size(setIntersection(field("tags"), array(value("STLW"), value("G")))),
                                        value(0)),
                                DESCEND, PRUNE)))
                .execute(Document.class)
                .next();
        Document expected = parse("{ '_id' : 1, 'title' : '123 Department Report', 'tags' : [ 'G', 'STLW' ],'year' : 2014, 'subsections' :"
                + " [{ 'subtitle' : 'Section 1: Overview', 'tags' : [ 'SI', 'G' ],'content' : 'Section 1: This is the "
                + "content of section 1.' },{ 'subtitle' : 'Section 2: Analysis', 'tags' : [ 'STLW' ],'content' : "
                + "'Section 2: This is the content of section 2.' }]}");

        assertEquals(expected, actual);
    }

}
