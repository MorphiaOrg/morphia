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

public class UpdateExecute extends Recipe {
    private static final MethodMatcher UPDATE = new MethodMatcher("dev.morphia.query.Update execute(..)");
    private static final MethodMatcher MODIFY = new MethodMatcher("dev.morphia.query.Modify execute(..)");

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
                if (UPDATE.matches(invocation) || MODIFY.matches(invocation)) {
                    var select = invocation.getSelect();
                    if (select instanceof MethodInvocation update) {
                        var updateArgs = new ArrayList<>(update.getArguments());
                        Expression expression = invocation.getArguments().get(0);
                        Method methodType = update.getMethodType();
                        if (!(expression instanceof Empty)) {
                            updateArgs.add(0, expression);
                            JavaType type = invocation.getName().getType();
                            if (type != null) {
                                List<String> names = ((Method) type).getParameterNames();
                                List<JavaType> types = ((Method) type).getParameterTypes();
                                List<String> extantNames = methodType.getParameterNames();
                                List<JavaType> extantTypes = methodType.getParameterTypes();
                                if (extantNames.size() > 1) {
                                    extantNames = List.of("updates");
                                    extantTypes = extantTypes.subList(1, extantTypes.size());
                                }
                                methodType = methodType
                                        .withParameterNames(combine(names, extantNames))
                                        .withParameterTypes(combine(types, extantTypes));
                            }
                            update = update.withArguments(updateArgs);
                        }

                        JavaType typeParameter = ((Parameterized) methodType.getReturnType())
                                .getTypeParameters()
                                .get(0);
                        methodType = methodType.withReturnType(typeParameter);

                        update = update
                                .withName(update.getName().withType(methodType))
                                .withMethodType(methodType);

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

    private <T> List<T> combine(List<T> first, List<T> second) {
        var combined = new ArrayList<T>();
        combined.addAll(first);
        combined.addAll(second);

        return combined;
    }
}
