package dev.morphia.test.aggregation.stages;

import dev.morphia.test.aggregation.AggregationTest;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.of;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Documents.documents;
import static dev.morphia.aggregation.stages.Lookup.lookup;

public class TestDocuments extends AggregationTest {
    @Override
    public String prefix() {
        return "documents";
    }

    @Test
    public void testLookup() {
        testCase(5.1, "lookup", "locations", (collection) -> {
            return getDs().aggregate(collection)
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

        });
    }
}
