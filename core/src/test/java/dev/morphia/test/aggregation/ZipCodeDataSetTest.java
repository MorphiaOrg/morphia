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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * These tests recreate the example zip code data set aggregations as found in the official documentation.
 *
 * @mongodb.driver.manual tutorial/aggregation-zip-code-data-set/ Aggregation with the Zip Code Data Set
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Ignore
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

            assertEquals(state.getBiggest().getName(), "SIOUX FALLS");
            assertEquals(state.getBiggest().getPopulation().longValue(), 102046);

            assertEquals(state.getSmallest().getName(), "ZEONA");
            assertEquals(state.getSmallest().getPopulation().longValue(), 8);
        }
    }

    private void validate(Aggregation<Population> cursor, String state, long value) {
        boolean found = false;
        for (Population population : cursor) {
            if (population.getState().equals(state)) {
                found = true;
                assertEquals(population.getPopulation(), Long.valueOf(value));
            }
            LOG.debug("population = " + population);
        }
        assertTrue(found, "Should have found " + state);
    }

}
