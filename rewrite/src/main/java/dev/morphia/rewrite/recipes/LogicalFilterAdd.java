package dev.morphia.rewrite.recipes;

import java.util.ArrayList;
import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;

public class LogicalFilterAdd extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("dev.morphia.query.filters.LogicalFilter add(..)");

    private static final List<String> LOGICAL = List.of("and", "nor", "or");

    @Override
    public @DisplayName String getDisplayName() {
        return "Collapses .add() calls to their logical filter";
    }

    @Override
    public @Description String getDescription() {
        return "Moves the parameters of chained LogicalFilter.add() calls to their logical filter method as varargs.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public MethodInvocation visitMethodInvocation(MethodInvocation method, ExecutionContext executionContext) {
                if (MATCHER.matches(method)) {
                    Expression select = method;
                    var arguments = new ArrayList<Expression>();
                    while (MATCHER.matches(select)) {
                        var invocation = (MethodInvocation) select;
                        arguments.add(0, invocation.getArguments().get(0));
                        select = invocation.getSelect();
                    }
                    if (LOGICAL.contains(((MethodInvocation) select).getSimpleName())) {
                        return maybeAutoFormat(method, ((MethodInvocation) select).withArguments(arguments), executionContext);
                    }
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }
}
