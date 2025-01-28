package dev.morphia.rewrite.recipes;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.ClassDeclaration;

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
            public J visitClassDeclaration(ClassDeclaration classDecl, ExecutionContext executionContext) {
                System.out.println("classDecl.getSimpleName() = " + classDecl.getSimpleName());
                return super.visitClassDeclaration(classDecl, executionContext);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, @NotNull ExecutionContext context) {
                if (fieldMatcher.matches(method) || valueMatcher.matches(method)) {
                    System.out.println("method = " + method);
                    maybeRemoveImport("dev.morphia.aggregation.expressions.Expressions.field");
                    maybeRemoveImport("dev.morphia.aggregation.expressions.Expressions.value");
                    Expression expression = method.getArguments().get(0);
                    System.out.println("expression = " + expression);
                    return expression;
                } else {
                    return super.visitMethodInvocation(method, context);
                }
            }
        };
    }

}
