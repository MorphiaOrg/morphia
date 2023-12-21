package dev.morphia.test.aggregation;

import java.time.LocalDate;

import com.mongodb.client.model.Collation;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationOptions;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.User;

import org.testng.annotations.Test;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static dev.morphia.query.filters.Filters.eq;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

public class TestCollation extends TestBase {
    @Test
    public void testCollation() {
        getDs().save(asList(new User("john doe", LocalDate.now()), new User("John Doe", LocalDate.now())));

        Aggregation<User> pipeline = getDs()
                .aggregate(User.class)
                .match(eq("name", "john doe"));
        assertEquals(count(pipeline.execute(User.class)), 1);

        assertEquals(count(pipeline.execute(User.class,
                new AggregationOptions()
                        .collation(Collation.builder()
                                .locale("en")
                                .collationStrength(SECONDARY)
                                .build()))),
                2);
    }

}
