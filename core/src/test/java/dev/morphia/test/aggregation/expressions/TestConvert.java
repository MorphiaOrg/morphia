package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.impls.ConvertType;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TypeExpressions.convert;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.in;

public class TestConvert extends TemplatedTestBase {

    @Test
    @DisplayName("Type Conversion Example")
    public void testTypeConversionExample() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("title", List.of("The Danish Girl", "The Godfather", "Gertie the Dinosaur"))),
                project().suppressId().include("title")
                        .include("rating", convert("$imdb.rating", ConvertType.DOUBLE).onError("Error").onNull(0))
                        .include("metacritic", convert("$metacritic", ConvertType.INT).onError("Error").onNull(0)),
                sort().ascending("title")));
    }
}
