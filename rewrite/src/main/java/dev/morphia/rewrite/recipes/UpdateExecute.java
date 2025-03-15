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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Empty;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Method;
import org.openrewrite.java.tree.JavaType.Parameterized;

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
        return new JavaVisitor<>() {

            @Nullable
            @Override
            public J visitMethodInvocation(@NotNull MethodInvocation invocation,
                    @NotNull ExecutionContext executionContext) {
                if (MATCHER.matches(invocation)) {
                    var select = invocation.getSelect();
                    List<Expression> arguments = invocation.getArguments();
                    if (select instanceof MethodInvocation update) {
                        var updateArgs = new ArrayList<>(update.getArguments());
                        Expression expression = arguments.get(0);
                        if (!(expression instanceof Empty)) {
                            updateArgs.add(0, expression);
                            update = update.withArguments(updateArgs);
                        }
                        Method methodType = update.getMethodType();

                        Parameterized returnType = (Parameterized) methodType.getReturnType();
                        JavaType typeParameter = returnType.getTypeParameters().get(0);

                        update = update.withMethodType(methodType.withReturnType(typeParameter));

                        maybeRemoveImport("dev.morphia.query.Update");
                        return maybeAutoFormat(invocation, update, executionContext);
                    } else {
                        return maybeAutoFormat(invocation, select, executionContext);
                    }

                }

                return super.visitMethodInvocation(invocation, executionContext);
            }
        };
    }
}
