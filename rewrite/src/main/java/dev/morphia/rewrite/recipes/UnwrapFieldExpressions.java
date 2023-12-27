package dev.morphia.rewrite.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.MethodDeclaration;
import org.openrewrite.java.tree.J.MethodInvocation;

public class UnwrapFieldExpressions extends Recipe {
    private static final MethodMatcher matcher = new MethodMatcher("dev.morphia.aggregation.expressions.Expression field(String)");
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
                if(!matcher.matches(method.getMethodType())) {
                    return method;
                }
                throw new UnsupportedOperationException();
//                return super.visitMethodInvocation(method, executionContext);
            }

        };
    }
}
