package dev.morphia.rewrite.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.MethodDeclaration;
import org.openrewrite.java.tree.J.MethodInvocation;

import java.util.stream.Collectors;

public class UnwrapFieldExpressions extends Recipe {
    private static final MethodMatcher matcher = new MethodMatcher("dev.morphia.aggregation.expressions.Expressions field(String)");
    @Override
    public String getDisplayName() {
        return "Unwrap field expressions that use Expressions#field()";
    }

    @Override
    public String getDescription() {
        return "Field names can be used directly now so this unwraps any uses of the field() method to just the raw String.";
    }

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public MethodInvocation visitMethodInvocation(MethodInvocation method, ExecutionContext executionContext) {
                method.getArguments().stream()
                      .map(e -> e.unwrap())
                      .filter(e -> e instanceof MethodInvocation)
                      .collect(Collectors.toList())
                               .forEach(e -> {
                                   visitMethodInvocation((MethodInvocation) e, executionContext);
                               });
                if(!matcher.matches(method.getMethodType())) {
                    return method;
                }
                throw new UnsupportedOperationException("Looks we found it! " + method);
//                return super.visitMethodInvocation(method, executionContext);
            }

        };
    }
}
