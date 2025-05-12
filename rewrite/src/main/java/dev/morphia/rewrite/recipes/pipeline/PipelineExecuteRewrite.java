package dev.morphia.rewrite.recipes.pipeline;

import dev.morphia.query.MorphiaCursor;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;

import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.AGGREGATION;

public class PipelineExecuteRewrite extends Recipe {
    public static final String CURSOR_CLASS = MorphiaCursor.class.getName();

    private static final MethodMatcher EXECUTE = new MethodMatcher(AGGREGATION + " execute(..)");
    private static final MethodMatcher CURSOR = new MethodMatcher(CURSOR_CLASS + " *(..)");

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
            public @NotNull MethodInvocation visitMethodInvocation(@NotNull MethodInvocation original,
                    @NotNull ExecutionContext context) {
                MethodInvocation invocation = super.visitMethodInvocation(original, context);
                if (CURSOR.matches(invocation)) {
                    Expression maybeMethod = invocation.getSelect();
                    if (maybeMethod instanceof MethodInvocation execute && EXECUTE.matches(maybeMethod)) {
                        invocation = removeExecute(invocation, execute);
                    }
                } else if (EXECUTE.matches(invocation)) {
                    invocation = removeExecute(null, invocation);
                }

                return invocation;
            }

            private MethodInvocation removeExecute(MethodInvocation cursorMethod, MethodInvocation execute) {
                var arguments = execute.getArguments();
                System.out.println("arguments = " + arguments);
                return null;
            }

        };
    }
}
