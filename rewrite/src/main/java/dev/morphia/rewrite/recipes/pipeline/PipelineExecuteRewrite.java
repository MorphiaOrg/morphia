package dev.morphia.rewrite.recipes.pipeline;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.query.MorphiaCursor;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;

import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.AGGREGATION;
import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.DATASTORE;

public class PipelineExecuteRewrite extends Recipe {
    public static final String CURSOR_CLASS = MorphiaCursor.class.getName();

    private static final MethodMatcher EXECUTE = new MethodMatcher(AGGREGATION + " execute(..)");
    private static final MethodMatcher AGGREGATE = new MethodMatcher(DATASTORE + " aggregate(..)");

    @Override
    public @NotNull String getDisplayName() {
        return "Aggregation pipeline execute() rewrite";
    }

    @Override
    public @NotNull String getDescription() {
        return "Rewrites uses of execute() moving an options to Datastore.aggregate().";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {

        return new JavaIsoVisitor<>() {
            @Override
            public @NotNull MethodInvocation visitMethodInvocation(@NotNull MethodInvocation original, @NotNull ExecutionContext context) {
                MethodInvocation invocation = super.visitMethodInvocation(original, context);

                if (EXECUTE.matches(invocation)) {
                    var arguments = invocation.getArguments();
                    invocation = (MethodInvocation) propagate(arguments,
                            invocation
                                    .withArguments(List.of())
                                    .withName(invocation.getName().withSimpleName("iterator"))
                                    .withMethodType(invocation.getMethodType()
                                            .withName("iterator")
                                            .withParameterNames(List.of())
                                            .withParameterTypes(List.of())
                                            .withReturnType(JavaType.buildType(MorphiaCursor.class.getName()))));
                    Identifier name = invocation.getName()
                            .withType(JavaType.buildType(MorphiaCursor.class.getName()));
                    invocation = invocation.withName(name);
                }

                return invocation;
            }

            private Expression propagate(List<Expression> arguments, Expression expression) {
                if (expression instanceof MethodInvocation invocation) {
                    if (AGGREGATE.matches(invocation)) {
                        List<Expression> aggregationArguments = new ArrayList<>(invocation.getArguments());
                        aggregationArguments.addAll(arguments);
                        return invocation.withArguments(aggregationArguments);
                    }
                    Expression propagate = propagate(arguments, invocation.getSelect());
                    return invocation.withSelect(propagate);
                } else if (expression instanceof Identifier identifier) {
                    return expression;
                } else {
                    throw new UnsupportedOperationException();
                }
            }

        };
    }
}
