package dev.morphia.test.aggregation.stages;

import java.time.LocalDate;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.models.User;

import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

public class TestSample extends AggregationTest {
    @Test
    public void testSample() {
        getDs().save(asList(new User("John", LocalDate.now()),
                new User("Paul", LocalDate.now()),
                new User("George", LocalDate.now()),
                new User("Ringo", LocalDate.now())));
        Aggregation<User> pipeline = getDs()
                .aggregate(User.class)
                .sample(3);

        assertEquals(pipeline.execute(User.class).toList().size(), 3);
    }

}
