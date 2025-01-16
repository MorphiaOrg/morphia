package dev.morphia.rewrite.recipes;

import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;

public class RegexPatternMerge extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("dev.morphia.query.filters.RegexFilter *(..)");

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Merge regex filter expression and pattern calls";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "This merges the fluent-style API calls in to a single method call.";
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @NotNull
            @Override
            public MethodInvocation visitMethodInvocation(@NotNull MethodInvocation invocation,
                    @NotNull ExecutionContext executionContext) {
                if (MATCHER.matches(invocation)) {
                    MethodInvocation pattern = findInvocation(invocation, "pattern");
                    if (pattern != null) {
                        var arguments = pattern.getArguments();
                        Expression select = invocation;
                        var methods = new ArrayList<MethodInvocation>();
                        while (select instanceof MethodInvocation parentInvocation) {
                            if (parentInvocation.getSimpleName().equals("regex")) {
                                arguments.addAll(0, parentInvocation.getArguments());
                                methods.add(0, parentInvocation);
                            } else if (!parentInvocation.getSimpleName().equals("pattern")) {
                                methods.add(0, parentInvocation);
                            }
                            select = parentInvocation.getSelect();
                        }
                        if (methods.size() == 1) {
                            return maybeAutoFormat(invocation, methods.get(0).withArguments(arguments), executionContext);
                        }
                        MethodInvocation newMethod = methods.get(0)
                                .withArguments(arguments);
                        for (MethodInvocation method : methods.subList(1, methods.size())) {
                            newMethod = method.withSelect(newMethod);
                        }
                        return maybeAutoFormat(invocation, newMethod, executionContext);
                    }
                }

                return super.visitMethodInvocation(invocation, executionContext);
            }
        };
    }

    @Nullable
    public static MethodInvocation findInvocation(Expression expression, String methodName) {
        if (expression == null) {
            return null;
        }
        if (expression instanceof MethodInvocation invocation) {
            if (invocation.getSimpleName().equals(methodName)) {
                return invocation;
            } else {
                return findInvocation(invocation.getSelect(), methodName);
            }
        }
        return null;
    }
}
