package dev.morphia.rewrite.recipes.datastore;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Method;

import static dev.morphia.rewrite.recipes.RewriteUtils.datastoreMethod;
import static dev.morphia.rewrite.recipes.RewriteUtils.isOfType;
import static dev.morphia.rewrite.recipes.RewriteUtils.template;

/**
 * An OpenRewrite recipe that replaces the removed {@code Datastore.find(String collection, Class type)} with
 * {@code find(type, new FindOptions().collection(collection))}. This needs to run before
 * {@link dev.morphia.rewrite.recipes.query.QueryFindOptions} so that any {@code FindOptions} passed to a terminal
 * query method can be merged with the collection name.
 */
public class DatastoreFindCollection extends Recipe {
    /** Creates a new instance. */
    public DatastoreFindCollection() {
    }

    private static final String FIND_OPTIONS = "dev.morphia.query.FindOptions";

    private static final MethodMatcher FIND = datastoreMethod("find(..)");

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Replace Datastore.find(String, Class)";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "Moves the collection name passed to the removed Datastore.find(String, Class) to FindOptions.";
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public MethodInvocation visitMethodInvocation(MethodInvocation method, ExecutionContext executionContext) {
                MethodInvocation invocation = super.visitMethodInvocation(method, executionContext);
                Method type = invocation.getMethodType();
                if (!FIND.matches(invocation) || type == null || type.getParameterTypes().size() != 2
                        || !isOfType(type.getParameterTypes().get(0), "java.lang.String", "kotlin.String")
                        || !isOfType(type.getParameterTypes().get(1), "java.lang.Class")) {
                    return invocation;
                }
                MethodInvocation updated = template(getCursor(),
                        "#{any()}, FindOptions().collection(#{any()})",
                        "#{any()}, new FindOptions().collection(#{any()})",
                        FIND_OPTIONS)
                        .apply(getCursor(), invocation.getCoordinates().replaceArguments(),
                                invocation.getArguments().get(1), invocation.getArguments().get(0));
                Method newType = type.withParameterTypes(
                        List.of(type.getParameterTypes().get(1), JavaType.buildType(FIND_OPTIONS)))
                        .withParameterNames(List.of("type", "options"));
                maybeAddImport(FIND_OPTIONS, false);
                return invocation
                        .withArguments(updated.getArguments())
                        .withName(invocation.getName().withType(newType))
                        .withMethodType(newType);
            }
        };
    }
}
