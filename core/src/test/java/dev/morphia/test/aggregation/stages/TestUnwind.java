package dev.morphia.test.aggregation.stages;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.models.User;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.format.DateTimeFormatter;
import java.util.Iterator;

import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static java.time.LocalDate.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

public class TestUnwind extends AggregationTest {
    @Test
    public void testUnwind() {
        DateTimeFormatter format = ofPattern("yyyy-MM-dd");
        getDs().save(asList(new User("jane", parse("2011-03-02", format), "golf", "racquetball"),
            new User("joe", parse("2012-07-02", format), "tennis", "golf", "swimming"),
            new User("john", parse("2012-07-02", format))));

        Iterator<User> aggregate = getDs().aggregate(User.class)
                                          .project(project()
                                                       .include("name")
                                                       .include("joined")
                                                       .include("likes"))
                                          .unwind(unwind("likes"))
                                          .execute(User.class);
        int count = 0;
        while (aggregate.hasNext()) {
            User user = aggregate.next();
            switch (count) {
                case 0:
                    assertEquals(user.name, "jane");
                    assertEquals(user.likes.get(0), "golf");
                    break;
                case 1:
                    assertEquals(user.name, "jane");
                    assertEquals(user.likes.get(0), "racquetball");
                    break;
                case 2:
                    assertEquals(user.name, "joe");
                    assertEquals(user.likes.get(0), "tennis");
                    break;
                case 3:
                    assertEquals(user.name, "joe");
                    assertEquals(user.likes.get(0), "golf");
                    break;
                case 4:
                    assertEquals(user.name, "joe");
                    assertEquals(user.likes.get(0), "swimming");
                    break;
                default:
                    Assert.fail("Should only find 5 elements");
            }
            count++;
        }

        aggregate = getDs().aggregate(User.class)
                           .project(project()
                                        .include("name")
                                        .include("joined")
                                        .include("likes"))
                           .unwind(unwind("likes")
                                       .preserveNullAndEmptyArrays(true))
                           .execute(User.class);
        count = 0;
        while (aggregate.hasNext()) {
            User user = aggregate.next();
            switch (count) {
                case 0:
                    assertEquals(user.name, "jane");
                    assertEquals(user.likes.get(0), "golf");
                    break;
                case 1:
                    assertEquals(user.name, "jane");
                    assertEquals(user.likes.get(0), "racquetball");
                    break;
                case 2:
                    assertEquals(user.name, "joe");
                    assertEquals(user.likes.get(0), "tennis");
                    break;
                case 3:
                    assertEquals(user.name, "joe");
                    assertEquals(user.likes.get(0), "golf");
                    break;
                case 4:
                    assertEquals(user.name, "joe");
                    assertEquals(user.likes.get(0), "swimming");
                    break;
                case 5:
                    assertEquals(user.name, "john");
                    Assert.assertNull(user.likes);
                    break;
                default:
                    Assert.fail("Should only find 6 elements");
            }
            count++;
        }
    }

}
