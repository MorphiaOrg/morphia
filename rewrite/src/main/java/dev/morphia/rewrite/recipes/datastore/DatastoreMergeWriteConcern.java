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
 * An OpenRewrite recipe that replaces the removed {@code Datastore.merge(T, WriteConcern)} with
 * {@code merge(T, new InsertOneOptions().writeConcern(wc))}.
 */
public class DatastoreMergeWriteConcern extends Recipe {
    /** Creates a new instance. */
    public DatastoreMergeWriteConcern() {
    }

    private static final String INSERT_ONE_OPTIONS = "dev.morphia.InsertOneOptions";

    private static final MethodMatcher MERGE = datastoreMethod("merge(..)");

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Replace Datastore.merge(T, WriteConcern)";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "Wraps the WriteConcern passed to the removed Datastore.merge(T, WriteConcern) in InsertOneOptions.";
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public MethodInvocation visitMethodInvocation(MethodInvocation method, ExecutionContext executionContext) {
                MethodInvocation invocation = super.visitMethodInvocation(method, executionContext);
                Method type = invocation.getMethodType();
                if (!MERGE.matches(invocation) || type == null || type.getParameterTypes().size() != 2
                        || !isOfType(type.getParameterTypes().get(1), "com.mongodb.WriteConcern")) {
                    return invocation;
                }
                MethodInvocation updated = template(getCursor(),
                        "#{any()}, InsertOneOptions().writeConcern(#{any()})",
                        "#{any()}, new InsertOneOptions().writeConcern(#{any()})",
                        INSERT_ONE_OPTIONS)
                        .apply(getCursor(), invocation.getCoordinates().replaceArguments(),
                                invocation.getArguments().get(0), invocation.getArguments().get(1));
                Method newType = type.withParameterTypes(
                        List.of(type.getParameterTypes().get(0), JavaType.buildType(INSERT_ONE_OPTIONS)))
                        .withParameterNames(List.of("entity", "options"));
                maybeAddImport(INSERT_ONE_OPTIONS, false);
                return invocation
                        .withArguments(updated.getArguments())
                        .withName(invocation.getName().withType(newType))
                        .withMethodType(newType);
            }
        };
    }
}
