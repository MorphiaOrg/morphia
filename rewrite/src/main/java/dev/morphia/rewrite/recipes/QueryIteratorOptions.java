package dev.morphia.rewrite.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.Empty;
import org.openrewrite.java.tree.J.MethodInvocation;

import static java.util.Collections.emptyList;

public class QueryIteratorOptions extends Recipe {
    private static final MethodMatcher QUERY_METHOD = new MethodMatcher("dev.morphia.query.Query *(..)");
    private static final MethodMatcher MORPHIA_CURSOR_METHOD = new MethodMatcher("dev.morphia.query.MorphiaCursor *(..)");

    @NotNull
    @Override
    @DisplayName
    public String getDisplayName() {
        return "Rewrite query find options";
    }

    @NotNull
    @Override
    @Description
    public String getDescription() {
        return "Moves the usage of FindOptions from iterator() to Datastore#find().";
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            String className = null;

            @NotNull
            @Override
            public J visitClassDeclaration(@NotNull ClassDeclaration classDecl, @NotNull ExecutionContext executionContext) {
                className = classDecl.getName().getSimpleName();
                return super.visitClassDeclaration(classDecl, executionContext);
            }

            @NotNull
            @Override
            public J visitMethodInvocation(@NotNull J.MethodInvocation invocation,
                    @NotNull ExecutionContext executionContext) {
                if ((QUERY_METHOD.matches(invocation) || MORPHIA_CURSOR_METHOD.matches(invocation)) && containsCall(invocation)) {
                    List<Expression> arguments = new ArrayList<>();
                    List<MethodInvocation> newCall = new ArrayList<>();
                    Expression select = invocation;
                    while (select instanceof MethodInvocation method && !method.getSimpleName().equals("find")) {
                        if (method.getSimpleName().equals("iterator")) {
                            arguments.addAll(method.getArguments());
                            method = method.withArguments(emptyList())
                                    .withMethodType(method.getMethodType()
                                            .withParameterTypes(emptyList())
                                            .withParameterNames(emptyList()));
                        }
                        newCall.add(method);
                        select = method.getSelect();
                    }
                    if (select instanceof MethodInvocation find && find.getSimpleName().equals("find")) {
                        arguments.add(0, find.getArguments().get(0));
                        newCall.add(find.withArguments(arguments));

                        ListIterator<MethodInvocation> iterator = newCall.listIterator(newCall.size());
                        MethodInvocation updated = null;
                        while (iterator.hasPrevious()) {
                            if (updated == null) {
                                updated = iterator.previous();
                            } else {
                                updated = iterator.previous().withSelect(updated);
                            }

                        }
                        return maybeAutoFormat(invocation, updated, executionContext);
                    } else {
                        return super.visitMethodInvocation(invocation, executionContext);
                    }
                }

                return super.visitMethodInvocation(invocation, executionContext);
            }

            private boolean containsCall(@NotNull MethodInvocation invocation) {
                var candidate = invocation;
                while (candidate.getSelect() instanceof MethodInvocation && !candidate.getSimpleName().equals("iterator")) {
                    candidate = (MethodInvocation) candidate.getSelect();
                }
                List<Expression> arguments = candidate.getArguments();
                var empty = arguments.isEmpty() || arguments.size() == 1 && arguments.get(0) instanceof Empty;
                return candidate.getSimpleName().equals("iterator") && !empty;
            }
        };

    }

}