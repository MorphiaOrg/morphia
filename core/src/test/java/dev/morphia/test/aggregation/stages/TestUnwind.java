package dev.morphia.test.aggregation.stages;

import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.models.User;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.in;
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

    @Test
    @DisplayName("Group by Unwound Values")
    public void testGroupByUnwoundValues() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("title", List.of("The Dark Knight", "Inception", "Interstellar", "Brave"))),
                project().suppressId().include("title").include("genres"),
                unwind("$genres"),
                group(id("$genres")).field("movieCount", sum(1)),
                sort().descending("movieCount")));
    }

    @Test
    @DisplayName("includeArrayIndex")
    public void testIncludeArrayIndex() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(eq("title", "Inception")),
                project().suppressId().include("title").include("genres"),
                unwind("$genres").includeArrayIndex("genreIndex")));
    }

    @Test
    @DisplayName("preserveNullAndEmptyArrays")
    public void testPreserveNullAndEmptyArrays() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("title", List.of("Inception", "Brave", "La porta del cielo", "Neecha Nagar"))),
                project().suppressId().include("title").include("genres"),
                unwind("$genres").preserveNullAndEmptyArrays(true)));
    }

    @Test
    @DisplayName("Unwind Multiple Array Fields")
    public void testUnwindMultipleArrayFields() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("title", List.of("Inception", "The Dark Knight", "Interstellar"))),
                project().suppressId().include("title").include("genres").include("cast"),
                unwind("$genres"),
                unwind("$cast"),
                group(id("$genres")).field("castAppearances", sum(1))));
    }
}
