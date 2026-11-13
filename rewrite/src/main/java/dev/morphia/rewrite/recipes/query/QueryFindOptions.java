package dev.morphia.rewrite.recipes.query;

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
import org.openrewrite.java.tree.J.Empty;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType.Method;
import org.openrewrite.java.tree.Space;

import jakarta.annotation.Nullable;

import static dev.morphia.rewrite.recipes.RewriteUtils.isOfType;
import static dev.morphia.rewrite.recipes.RewriteUtils.template;
import static java.util.Collections.emptyList;

/**
 * An OpenRewrite recipe that moves {@code FindOptions} arguments from terminal query methods
 * ({@code first()}, {@code iterator()}, {@code stream()}) to {@code Datastore#find()}.
 */
public class QueryFindOptions extends Recipe {
    /** Creates a new instance. */
    public QueryFindOptions() {
    }

    private static final String FIND_OPTIONS = "dev.morphia.query.FindOptions";
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
        return "Moves the uses of FindOptions to Datastore#find().";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new MoveFindOptions("first"),
                new MoveFindOptions("iterator"),
                new MoveFindOptions("stream"));
    }

    /**
     * Returns the collection name if the expression is exactly {@code new FindOptions().collection(name)}.
     */
    private static @Nullable Expression collectionName(Expression expression) {
        if (expression instanceof MethodInvocation method && method.getSimpleName().equals("collection")
                && method.getArguments().size() == 1 && isNewFindOptions(method.getSelect())
                && method.getSelect() instanceof J.NewClass) {
            return method.getArguments().get(0);
        }
        return null;
    }

    /**
     * Checks whether the expression is a {@code new FindOptions()}, possibly followed by a chain of calls on it, so
     * that it can be modified without affecting any other use of the options.
     */
    private static boolean isNewFindOptions(@Nullable Expression expression) {
        while (expression instanceof MethodInvocation method) {
            expression = method.getSelect();
        }
        return expression instanceof J.NewClass newClass && isOfType(newClass.getType(), FIND_OPTIONS)
                && newClass.getArguments().stream().allMatch(argument -> argument instanceof Empty);
    }

    /**
     * Checks whether the options name a collection that replaces the one given to {@code find()}. In 2.x, a null
     * collection fell back to the query's collection, so only a non-null string literal provably overrides it. Returns
     * null when that can't be told.
     */
    private static @Nullable Boolean collectionOverride(Expression expression) {
        while (expression instanceof MethodInvocation method) {
            if (method.getSimpleName().equals("collection")) {
                return method.getArguments().get(0) instanceof J.Literal literal && literal.getValue() != null
                        ? Boolean.TRUE
                        : null;
            }
            expression = method.getSelect();
        }
        return false;
    }

    private static boolean isSideEffectFree(Expression expression) {
        return expression instanceof J.Literal || expression instanceof J.FieldAccess fieldAccess
                && isSideEffectFree(fieldAccess.getTarget())
                || expression instanceof J.Identifier;
    }

    private class MoveFindOptions extends Recipe {

        private final String methodName;

        @NotNull
        @Override
        @DisplayName
        public String getDisplayName() {
            return "Rewrite query find options in %s()".formatted(methodName);
        }

        @NotNull
        @Override
        @Description
        public String getDescription() {
            return "Moves the usage of FindOptions from %s() to Datastore#find().".formatted(methodName);
        }

        public MoveFindOptions(String methodName) {
            this.methodName = methodName;
        }

        @NotNull
        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaVisitor<>() {

                @NotNull
                @Override
                public J visitMethodInvocation(@NotNull J.MethodInvocation invocation,
                        @NotNull ExecutionContext executionContext) {
                    if ((QUERY_METHOD.matches(invocation) || MORPHIA_CURSOR_METHOD.matches(invocation)) && containsCall(invocation)) {
                        List<Expression> arguments = new ArrayList<>();
                        List<MethodInvocation> newCall = new ArrayList<>();
                        Expression select = invocation;
                        while (select instanceof MethodInvocation method && !method.getSimpleName().equals("find")) {
                            if (method.getSimpleName().equals(methodName)) {
                                arguments.addAll(method.getArguments());
                                Method newType = method.getMethodType()
                                        .withParameterTypes(emptyList())
                                        .withParameterNames(emptyList());
                                method = method.withArguments(emptyList())
                                        .withName(method.getName().withType(newType))
                                        .withMethodType(newType);
                            }
                            newCall.add(method);
                            select = method.getSelect();
                        }
                        if (select instanceof MethodInvocation find && find.getSimpleName().equals("find")) {
                            List<Expression> findArguments = mergeFindArguments(find, arguments);
                            if (findArguments == null) {
                                return super.visitMethodInvocation(invocation, executionContext);
                            }
                            newCall.add(find.withArguments(findArguments));

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

                /**
                 * Combines the arguments already passed to {@code find()} with the options being moved. A
                 * {@code FindOptions} already passed to {@code find()} can only be merged when it does nothing but
                 * name the collection (e.g., as rewritten from {@code find(String, Class)}) and the moved options
                 * are an inline {@code new FindOptions()} chain. A collection named in the moved options wins, as it
                 * did in 2.x. Otherwise, returns null so the invocation is left for a manual migration rather than
                 * silently dropping or changing either set of options.
                 */
                private @Nullable List<Expression> mergeFindArguments(MethodInvocation find, List<Expression> moved) {
                    List<Expression> findArguments = find.getArguments();
                    List<Expression> merged = new ArrayList<>();
                    merged.add(findArguments.get(0));
                    List<Expression> options = moved;
                    for (Expression argument : findArguments.subList(1, findArguments.size())) {
                        if (isOfType(argument.getType(), FIND_OPTIONS)) {
                            Expression collection = collectionName(argument);
                            // the collection is now evaluated after the moved options, or not at all if they override it
                            if (collection == null || !isSideEffectFree(collection) || moved.size() != 1
                                    || !isNewFindOptions(moved.get(0))) {
                                return null;
                            }
                            Expression movedOptions = moved.get(0);
                            Boolean override = collectionOverride(movedOptions);
                            if (override == null) {
                                return null;
                            } else if (!override) {
                                J replaced = template(getCursor(),
                                        "#{any()}.collection(#{any()})",
                                        "#{any(%s)}.collection(#{any(java.lang.String)})".formatted(FIND_OPTIONS),
                                        FIND_OPTIONS)
                                        .apply(getCursor(), ((MethodInvocation) getCursor().getValue()).getCoordinates().replace(),
                                                movedOptions.withPrefix(Space.EMPTY), collection);
                                options = List.of(((Expression) replaced).withPrefix(movedOptions.getPrefix()));
                            }
                        } else {
                            merged.add(argument);
                        }
                    }
                    merged.addAll(options);
                    return merged;
                }

                private boolean containsCall(@NotNull MethodInvocation invocation) {
                    var candidate = invocation;
                    while (candidate.getSelect() instanceof MethodInvocation && !candidate.getSimpleName().equals(methodName)) {
                        candidate = (MethodInvocation) candidate.getSelect();
                    }
                    List<Expression> arguments = candidate.getArguments();
                    var empty = arguments.isEmpty() || arguments.size() == 1 && arguments.get(0) instanceof Empty;
                    return candidate.getSimpleName().equals(methodName) && !empty;
                }
            };

        }

    }
}