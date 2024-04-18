package dev.morphia.rewrite.recipes.test.originals;

import dev.morphia.aggregation.Aggregation;

import org.bson.Document;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Sort.sort;

public class PipelineRewriteOriginal {
    public void update(Aggregation<?> aggregation) {
        aggregation
                .group(group(id("author")).field("count", sum(value(1))))
                .sort(sort().ascending("_id"))
                .execute(Document.class);
    }
}
