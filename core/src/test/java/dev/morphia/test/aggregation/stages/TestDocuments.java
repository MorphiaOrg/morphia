package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.test.aggregation.AggregationTest;
import org.bson.Document;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static dev.morphia.aggregation.expressions.Expressions.of;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Documents.documents;
import static dev.morphia.aggregation.stages.Lookup.lookup;
import static org.testng.Assert.assertEquals;

public class TestDocuments extends AggregationTest {

    @Test
    public void testLookup() throws IOException {
        checkMinServerVersion(5.1);
        insert("locations", parseDocs(
            "{ zip: 94301, name: 'Palo Alto' }",
            "{ zip: 10019, name: 'New York' }"));

        AggregationImpl<Document> aggregation =
            (AggregationImpl<Document>) getDs().aggregate("locations")
                                               .match()
                                               .lookup(lookup()
                                                           .localField("zip")
                                                           .foreignField("zip_id")
                                                           .as("city_state")
                                                           .pipeline(
                                                               documents(
                                                                   of().field("zip_id", value(94301))
                                                                       .field("name",
                                                                           value("Palo Alto, CA")),
                                                                   of().field("zip_id", value(10019))
                                                                       .field("name",
                                                                           value("New York, NY")))));

        validatePipeline(aggregation, "documents-lookup.json");

        List<Document> actual = removeIds(aggregation.execute(Document.class).toList());
        List<Document> expected = parseDocs(
            "{zip: 94301, name: 'Palo Alto', city_state: [ { zip_id: 94301, name: 'Palo Alto, CA' } ]}",
            "{ zip: 10019, name: 'New York', city_state: [ { zip_id: 10019, name: 'New York, NY' } ]}");

        assertEquals(actual, expected);
    }
}
