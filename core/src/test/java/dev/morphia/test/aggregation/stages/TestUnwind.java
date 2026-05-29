package dev.morphia.test.aggregation.stages;

import java.time.format.DateTimeFormatter;
import java.util.Iterator;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.models.User;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static java.time.LocalDate.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;

public class TestUnwind extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/unwind/example1
     * 
     */
    @Test
    @DisplayName("Unwind Array")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(unwind("sizes")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unwind/example2
     * 
     */
    @Test
    @DisplayName("Missing or Non-array Values")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(unwind("sizes")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unwind/example3
     * 
     */
    @Test
    @DisplayName("``preserveNullAndEmptyArrays`` and ``includeArrayIndex``")
    public void testExample3() {
        testPipeline((aggregation) -> aggregation.pipeline(unwind("sizes").preserveNullAndEmptyArrays(true)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unwind/example4
     * 
     */
    @Test
    @DisplayName("Group by Unwound Values")
    public void testExample4() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(unwind("sizes").preserveNullAndEmptyArrays(true),
                        group(id("$sizes")).field("averagePrice", avg("$price")), sort().descending("averagePrice")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unwind/example5
     * 
     */
    @Test
    @DisplayName("Unwind Embedded Arrays")
    public void testExample5() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation.pipeline(unwind("items"),
                unwind("items.tags"),
                group(id("$items.tags")).field("totalSalesAmount", sum(multiply("$items.price", "$items.quantity")))));
    }

    @Test
    public void testUnwind() {
        DateTimeFormatter format = ofPattern("yyyy-MM-dd");
        getDs().save(asList(new User("jane", parse("2011-03-02", format), "golf", "racquetball"),
                new User("joe", parse("2012-07-02", format), "tennis", "golf", "swimming"),
                new User("john", parse("2012-07-02", format))));

        Iterator<User> aggregate = getDs().aggregate(User.class)
                .pipeline(project().include("name").include("joined").include("likes"), unwind("likes")).iterator();
        int count = 0;
        while (aggregate.hasNext()) {
            User user = aggregate.next();
            switch (count) {
                case 0:
                    Assertions.assertEquals("jane", user.name);
                    Assertions.assertEquals("golf", user.likes.get(0));
                    break;
                case 1:
                    Assertions.assertEquals("jane", user.name);
                    Assertions.assertEquals("racquetball", user.likes.get(0));
                    break;
                case 2:
                    Assertions.assertEquals("joe", user.name);
                    Assertions.assertEquals("tennis", user.likes.get(0));
                    break;
                case 3:
                    Assertions.assertEquals("joe", user.name);
                    Assertions.assertEquals("golf", user.likes.get(0));
                    break;
                case 4:
                    Assertions.assertEquals("joe", user.name);
                    Assertions.assertEquals("swimming", user.likes.get(0));
                    break;
                default:
                    Assertions.fail("Should only find 5 elements");
            }
            count++;
        }

        aggregate = getDs().aggregate(User.class).pipeline(project().include("name").include("joined").include("likes"),
                unwind("likes").preserveNullAndEmptyArrays(true)).iterator();
        count = 0;
        while (aggregate.hasNext()) {
            User user = aggregate.next();
            switch (count) {
                case 0:
                    Assertions.assertEquals("jane", user.name);
                    Assertions.assertEquals("golf", user.likes.get(0));
                    break;
                case 1:
                    Assertions.assertEquals("jane", user.name);
                    Assertions.assertEquals("racquetball", user.likes.get(0));
                    break;
                case 2:
                    Assertions.assertEquals("joe", user.name);
                    Assertions.assertEquals("tennis", user.likes.get(0));
                    break;
                case 3:
                    Assertions.assertEquals("joe", user.name);
                    Assertions.assertEquals("golf", user.likes.get(0));
                    break;
                case 4:
                    Assertions.assertEquals("joe", user.name);
                    Assertions.assertEquals("swimming", user.likes.get(0));
                    break;
                case 5:
                    Assertions.assertEquals("john", user.name);
                    Assertions.assertNull(user.likes);
                    break;
                default:
                    Assertions.fail("Should only find 6 elements");
            }
            count++;
        }
    }

}
