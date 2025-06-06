package dev.morphia.test.aggregation;

import java.time.LocalDate;

import com.mongodb.client.model.Collation;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationOptions;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.User;

import org.testng.annotations.Test;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

public class TestCollation extends TestBase {
    @Test
    public void testCollation() {
        getDs().save(asList(new User("john doe", LocalDate.now()), new User("John Doe", LocalDate.now())));

        Aggregation<User> pipeline = getDs()
                .aggregate(User.class)
                .pipeline(match(eq("name", "john doe")));
        assertEquals(count(pipeline.iterator()), 1);

        AggregationOptions options = new AggregationOptions()
                .collation(Collation.builder()
                        .locale("en")
                        .collationStrength(SECONDARY)
                        .build());
        pipeline = getDs()
                .aggregate(User.class, options)
                .pipeline(match(eq("name", "john doe")));
        assertEquals(count(pipeline.iterator()),
                2);
    }

}
