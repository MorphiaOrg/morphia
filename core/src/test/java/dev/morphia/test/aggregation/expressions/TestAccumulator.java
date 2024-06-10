package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.stages.Group;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.accumulator;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAccumulator extends AggregationTest {
    @Test
    public void testExample1() {
        skipActionCheck();
        testPipeline(ANY, false, false, aggregation -> aggregation
                .pipeline(Group.group(Group.id("$author"))
                        .field("avgCopies", accumulator(
                                """
                                        function() {
                                          return { count: 0, sum: 0 }
                                        }
                                        """,
                                """
                                        function(state, numCopies) {
                                          return {
                                            count: state.count + 1,
                                            sum: state.sum + numCopies
                                          }
                                        }""",
                                List.of("$copies"),
                                """
                                        function(state1, state2) {
                                          return {
                                            count: state1.count + state2.count,
                                            sum: state1.sum + state2.sum
                                          }
                                        }""").finalizeFunction("""
                                        function(state) {
                                          return (state.sum / state.count)
                                        }"""))));
    }

    @Test
    public void testExample2() {
        skipActionCheck();
        testPipeline(ANY, false, false, aggregation -> aggregation
                .pipeline(Group.group(Group.id().field("city", "$city"))
                        .field("restaurants", accumulator(
                                """
                                        function(city, userProfileCity) {       \s
                                                  return {
                                                    max: city === userProfileCity ? 3 : 1,    \s
                                                    restaurants: []                           \s
                                                  }\s
                                                }""",
                                """
                                        function(state, restaurantName) { \s
                                                  if (state.restaurants.length < state.max) {
                                                    state.restaurants.push(restaurantName);
                                                  }
                                                  return state;
                                                }""",
                                List.of("$name"), """
                                        function(state1, state2) {             \s
                                                  return {
                                                    max: state1.max,
                                                    restaurants: state1.restaurants.concat(state2.restaurants).slice(0, state1.max)
                                                  }\s
                                                }""")
                                .initArgs(List.of("$city", "Bettles"))
                                .finalizeFunction("""
                                        function(state) {
                                                  return state.restaurants
                                                }"""))));
    }
}
