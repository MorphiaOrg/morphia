package dev.morphia.rewrite.recipes.datastore;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Method;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.marker.IsNullSafe;
import org.openrewrite.marker.Markers;

import static dev.morphia.rewrite.recipes.RewriteUtils.datastoreMethod;
import static dev.morphia.rewrite.recipes.RewriteUtils.isOfType;
import static org.openrewrite.Tree.randomId;

/**
 * An OpenRewrite recipe that replaces the removed {@code Datastore.delete(Query)} and
 * {@code Datastore.delete(Query, DeleteOptions)} with {@code query.delete()} and {@code query.delete(options)}.
 */
public class DatastoreDeleteQuery extends Recipe {
    /** Creates a new instance. */
    public DatastoreDeleteQuery() {
    }

    private static final String QUERY = "dev.morphia.query.Query";

    private static final MethodMatcher DELETE = datastoreMethod("delete(..)");

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Replace Datastore.delete(Query) with Query.delete()";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "Moves the query passed to the removed Datastore.delete(Query) overloads to be the receiver of Query.delete().";
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public MethodInvocation visitMethodInvocation(MethodInvocation method, ExecutionContext executionContext) {
                MethodInvocation invocation = super.visitMethodInvocation(method, executionContext);
                Method type = invocation.getMethodType();
                if (!DELETE.matches(invocation) || type == null || type.getParameterTypes().isEmpty()
                        || !isOfType(type.getParameterTypes().get(0), QUERY)
                // `ds?.delete(query)` would lose its null check on the datastore
                        || invocation.getMarkers().findFirst(IsNullSafe.class).isPresent()) {
                    return invocation;
                }
                List<Expression> arguments = invocation.getArguments();
                Expression query = arguments.get(0);
                List<Expression> remaining = new ArrayList<>(arguments.subList(1, arguments.size()));
                if (remaining.isEmpty()) {
                    remaining.add(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY));
                } else {
                    remaining.set(0, remaining.get(0).withPrefix(Space.EMPTY));
                }
                List<JavaType> parameterTypes = type.getParameterTypes();
                List<String> parameterNames = type.getParameterNames();
                JavaType.FullyQualified queryType = TypeUtils.asFullyQualified(parameterTypes.get(0));
                Method newType = type
                        .withDeclaringType(queryType instanceof JavaType.Parameterized parameterized
                                ? parameterized.getType()
                                : queryType)
                        .withParameterTypes(parameterTypes.subList(1, parameterTypes.size()))
                        .withParameterNames(parameterNames.subList(1, parameterNames.size()));

                Space prefix = invocation.getSelect() != null ? invocation.getSelect().getPrefix() : Space.EMPTY;
                Expression select = needsParentheses(query)
                        ? new J.Parentheses<>(randomId(), prefix, Markers.EMPTY,
                                JRightPadded.build(query.withPrefix(Space.EMPTY)))
                        : query.withPrefix(prefix);
                return invocation
                        .withSelect(select)
                        .withArguments(remaining)
                        .withName(invocation.getName().withType(newType))
                        .withMethodType(newType);
            }
        };
    }

    private static boolean needsParentheses(Expression expression) {
        return !(expression instanceof J.Identifier
                || expression instanceof J.FieldAccess
                || expression instanceof J.MethodInvocation
                || expression instanceof J.NewClass
                || expression instanceof J.Parentheses);
    }
}
