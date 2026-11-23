package dev.morphia.test.aggregation.stages;

import java.time.format.DateTimeFormatter;
import java.util.Iterator;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.models.User;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static java.time.LocalDate.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;

public class TestUnwind extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/unwind/unwindArray
     * 
     */
    @Test
    @DisplayName("Unwind Array")
    public void testUnwindArray() {
        testPipeline((aggregation) -> aggregation.pipeline(unwind("sizes")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unwind/missingOrNonArrayValues
     * 
     */
    @Test
    @DisplayName("Missing or Non-array Values")
    public void testMissingOrNonArrayValues() {
        testPipeline((aggregation) -> aggregation.pipeline(unwind("sizes")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unwind/preserveNullAndEmptyArraysAndIncludeArrayIndex
     * 
     */
    @Test
    @DisplayName("``preserveNullAndEmptyArrays`` and ``includeArrayIndex``")
    public void testPreserveNullAndEmptyArraysAndIncludeArrayIndex() {
        testPipeline((aggregation) -> aggregation.pipeline(unwind("sizes").preserveNullAndEmptyArrays(true)));
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
