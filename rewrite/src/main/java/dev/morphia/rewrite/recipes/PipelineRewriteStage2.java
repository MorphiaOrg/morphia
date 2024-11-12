package dev.morphia.rewrite.recipes;

import java.util.ArrayList;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J.MethodInvocation;

public class PipelineRewriteStage2 extends Recipe {

    private static final String AGGREGATION = "dev.morphia.aggregation.Aggregation";

    private static final MethodMatcher PIPELINE = new MethodMatcher(PipelineRewriteStage2.AGGREGATION + " pipeline(..)");

    @Override
    public String getDisplayName() {
        return "Aggregation pipeline rewrite";
    }

    @Override
    public String getDescription() {
        return "Rewrites an aggregation from using stage-named methods to using pipeline(Stage...).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(PIPELINE), new JavaIsoVisitor<>() {

            public MethodInvocation visitMethodInvocation(MethodInvocation method, ExecutionContext p) {
                if (!PIPELINE.matches(method.getSelect())) {
                    return method;
                }

                var updated = method;
                while (PIPELINE.matches(updated.getSelect()) && PIPELINE.matches(((MethodInvocation) updated.getSelect()).getSelect())) {
                    var select = updated.getSelect();
                    MethodInvocation invocation = (MethodInvocation) select;
                    if (PIPELINE.matches(invocation.getSelect())) {
                        MethodInvocation parent = (MethodInvocation) invocation.getSelect();
                        var args = new ArrayList<>(parent.getArguments());
                        args.addAll(invocation.getArguments());
                        updated = updated.withSelect(((MethodInvocation) invocation.getSelect()).withArguments(args));
                    }
                }
                return updated;
            }

        });
    }
}
