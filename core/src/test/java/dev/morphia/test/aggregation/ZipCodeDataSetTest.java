package dev.morphia.test.aggregation;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.client.MongoCursor;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.City;
import dev.morphia.test.models.Population;
import dev.morphia.test.models.State;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.first;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.last;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.gte;

/**
 * These tests recreate the example zip code data set aggregations as found in the official documentation.
 *
 * @mongodb.driver.manual tutorial/aggregation-zip-code-data-set/ Aggregation with the Zip Code Data Set
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Disabled
public class ZipCodeDataSetTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(ZipCodeDataSetTest.class);

    @Test
    public void averageCitySizeByState() {
        Aggregation<Population> pipeline = getDs().aggregate(City.class, Population.class)
                .pipeline(
                        group(id().field("state")
                                .field("city"))
                                .field("pop", sum("$pop")),
                        group(id("_id.state"))
                                .field("avgCityPop", avg("$pop")));
        validate(pipeline, "MN", 5372);
    }

    @Test
    public void populationsAbove10M() {
        Aggregation<Population> pipeline = getDs().aggregate(City.class, Population.class)
                .pipeline(
                        group(id("state"))
                                .field("totalPop", sum("$pop")),
                        match(gte("totalPop", 10000000)));

        validate(pipeline, "CA", 29754890);
        validate(pipeline, "OH", 10846517);
    }

    @Test
    public void smallestAndLargestCities() {
        getMapper().mapPackage(getClass().getPackage().getName());

        Aggregation<State> pipeline = getDs().aggregate(City.class, State.class)

                .pipeline(group(id().field("state")
                        .field("city"))
                        .field("pop", sum("$pop")),
                        sort().ascending("pop"),
                        group(
                                id("_id.state"))
                                .field("biggestCity", last("$_id.city"))
                                .field("biggestPop", last("$pop"))
                                .field("smallestCity", first("$_id.city"))
                                .field("smallestPop", first("$pop")),
                        project()
                                .exclude("_id")
                                .include("state", "$_id")
                                .include("biggestCity",
                                        Expressions.document()
                                                .field("name", "$biggestCity")
                                                .field("pop", "$biggestPop"))
                                .include("smallestCity",
                                        Expressions.document()
                                                .field("name", "$smallestCity")
                                                .field("pop", "$smallestPop")));

        try (MongoCursor<State> cursor = pipeline.iterator()) {
            Map<String, State> states = new HashMap<>();
            while (cursor.hasNext()) {
                State state = cursor.next();
                states.put(state.getState(), state);
            }

            State state = states.get("SD");

            Assertions.assertEquals("SIOUX FALLS", state.getBiggest().getName());
            Assertions.assertEquals(102046, state.getBiggest().getPopulation().longValue());

            Assertions.assertEquals("ZEONA", state.getSmallest().getName());
            Assertions.assertEquals(8, state.getSmallest().getPopulation().longValue());
        }
    }

    private void validate(Aggregation<Population> cursor, String state, long value) {
        boolean found = false;
        for (Population population : cursor) {
            if (population.getState().equals(state)) {
                found = true;
                Assertions.assertEquals(Long.valueOf(value), population.getPopulation());
            }
            LOG.debug("population = " + population);
        }
        Assertions.assertTrue(found, "Should have found " + state);
    }

}
