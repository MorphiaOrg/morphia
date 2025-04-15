package dev.morphia.rewrite.recipes;

import java.util.ArrayList;
import java.util.List;

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

import jakarta.annotation.Nullable;

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
        return new JavaVisitor<>() {

            @Nullable
            @Override
            public J visitMethodInvocation(@NotNull MethodInvocation invocation,
                    @NotNull ExecutionContext executionContext) {
                if (MATCHER.matches(invocation)) {
                    var select = invocation.getSelect();
                    List<Expression> arguments = invocation.getArguments();
                    if (select instanceof MethodInvocation modify) {
                        var modifyArgs = new ArrayList<>(modify.getArguments());
                        Expression expression = arguments.get(0);
                        if (!(expression instanceof Empty)) {
                            modifyArgs.add(0, expression);
                            modify = modify.withArguments(modifyArgs);
                        }
                        Method methodType = modify.getMethodType();

                        Parameterized returnType = (Parameterized) methodType.getReturnType();
                        JavaType typeParameter = returnType.getTypeParameters().get(0);

                        modify = modify.withMethodType(methodType.withReturnType(typeParameter));

                        maybeRemoveImport("dev.morphia.query.Modify");
                        return maybeAutoFormat(invocation, modify, executionContext);
                    } else {
                        return maybeAutoFormat(invocation, select, executionContext);
                    }

                }

                return super.visitMethodInvocation(invocation, executionContext);
            }
        };
    }
}
