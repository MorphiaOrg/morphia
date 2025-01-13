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

public class UpdateExecute extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("dev.morphia.query.Update execute(..)");

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Remove calls to Update.execute()";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "Removes calls to Update.execute().";
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
                    boolean defaultOptions = !arguments.isEmpty() && arguments.get(0).toString().equals("new UpdateOptions()");
                    if (!(select instanceof MethodInvocation update)) {
                        return invocation;
                    }

                    var updateArgs = new ArrayList<>(update.getArguments());
                    Expression expression = arguments.get(0);
                    if (!(expression instanceof Empty)) {
                        updateArgs.add(0, expression);
                        update = update.withArguments(updateArgs);
                    }
                    return maybeAutoFormat(invocation, update, executionContext);
                }

                return super.visitMethodInvocation(invocation, executionContext);
            }
        };
    }
}
