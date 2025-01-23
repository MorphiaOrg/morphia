package dev.morphia.rewrite.recipes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.Empty;
import org.openrewrite.java.tree.J.MethodInvocation;

public class ModifyExecute extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("dev.morphia.query.Modify execute(..)");

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Remove calls to Modify.execute()";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "Removes calls to Modify.execute().";
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Nullable
            @Override
            public MethodInvocation visitMethodInvocation(@NotNull MethodInvocation invocation,
                    @NotNull ExecutionContext executionContext) {
                if (MATCHER.matches(invocation)) {
                    var select = invocation.getSelect();
                    List<Expression> arguments = invocation.getArguments();
                    boolean defaultOptions = !arguments.isEmpty() && arguments.get(0).toString().equals("new ModifyOptions()");
                    if (!(select instanceof MethodInvocation modify)) {
                        return invocation;
                    }

                    var updateArgs = new ArrayList<>(modify.getArguments());
                    Expression expression = arguments.get(0);
                    if (!(expression instanceof Empty)) {
                        updateArgs.add(0, expression);
                        modify = modify.withArguments(updateArgs);
                    }
                    return maybeAutoFormat(invocation, modify, executionContext);
                }

                return super.visitMethodInvocation(invocation, executionContext);
            }
        };
    }
}
