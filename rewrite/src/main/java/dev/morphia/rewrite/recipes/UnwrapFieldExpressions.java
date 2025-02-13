package dev.morphia.rewrite.recipes;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.MethodInvocation;

public class UnwrapFieldExpressions extends Recipe {
    @Override
    public String getDisplayName() {
        return "Unwrap field() and value()";
    }

    @Override
    public String getDescription() {
        return "Field names can be used directly now so this unwraps any uses of Expressions#field() or Expressions#field() method to " +
                "just its argument.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        MethodMatcher fieldMatcher = new MethodMatcher("dev.morphia.aggregation.expressions.Expressions field(..)");
        MethodMatcher valueMatcher = new MethodMatcher("dev.morphia.aggregation.expressions.Expressions value(..)");

        return new JavaVisitor<>() {
            @Override
            public J visitClassDeclaration(ClassDeclaration classDecl, ExecutionContext executionContext) {
                return super.visitClassDeclaration(classDecl, executionContext);
            }

            @Override
            public J visitMethodInvocation(MethodInvocation method, @NotNull ExecutionContext context) {
                if (fieldMatcher.matches(method) || valueMatcher.matches(method)) {
                    maybeRemoveImport("dev.morphia.aggregation.expressions.Expressions.field");
                    maybeRemoveImport("dev.morphia.aggregation.expressions.Expressions.value");
                    maybeRemoveImport("dev.morphia.aggregation.expressions.impls.Expression");
                    return maybeAutoFormat(method, extractArgument(method), context);
                } else {
                    return super.visitMethodInvocation(method, context);
                }
            }

            private @NotNull Expression extractArgument(Expression argument) {
                if (fieldMatcher.matches(argument) || valueMatcher.matches(argument)) {
                    MethodInvocation method = (MethodInvocation) argument;
                    Expression expression = method.getArguments().get(0);
                    if (expression instanceof MethodInvocation invocation) {
                        List<Expression> arguments = invocation.getArguments();
                        arguments.replaceAll(this::extractArgument);
                        expression = invocation.withArguments(arguments);
                    }
                    return expression;
                } else {
                    return argument;
                }
            }
        };
    }

}
