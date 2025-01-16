package dev.morphia.rewrite.recipes;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class UnwrapFieldExpressions extends Recipe {
    @Override
    public String getDisplayName() {
        return "Unwrap field expressions that use Expressions#field()";
    }

    @Override
    public String getDescription() {
        return "Field names can be used directly now so this unwraps any uses of the field() method to just the raw String.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        MethodMatcher fieldMatcher = new MethodMatcher("dev.morphia.aggregation.expressions.Expressions field(..)");
        MethodMatcher valueMatcher = new MethodMatcher("dev.morphia.aggregation.expressions.Expressions value(..)");

        return new JavaVisitor<>() {

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, @NotNull ExecutionContext context) {
                if (fieldMatcher.matches(method) || valueMatcher.matches(method)) {
                    return method.getArguments().get(0);
                } else {
                    return super.visitMethodInvocation(method, context);
                }
            }
        };
    }

}
