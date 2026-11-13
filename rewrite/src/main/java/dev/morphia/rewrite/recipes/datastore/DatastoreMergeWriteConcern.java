package dev.morphia.rewrite.recipes.datastore;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Method;
import org.openrewrite.kotlin.marker.SingleExpressionBlock;
import org.openrewrite.kotlin.tree.K;

import static dev.morphia.rewrite.recipes.RewriteUtils.datastoreMethod;
import static dev.morphia.rewrite.recipes.RewriteUtils.isKotlin;
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
                        || !isOfType(type.getParameterTypes().get(1), "com.mongodb.WriteConcern")
                        || isKotlinExpressionBody(getCursor())) {
                    return invocation;
                }
                MethodInvocation updated = template(getCursor(),
                        "#{any()}, InsertOneOptions().writeConcern(#{any()})",
                        "#{any()}, new InsertOneOptions().writeConcern(#{any()})",
                        INSERT_ONE_OPTIONS)
                        .apply(new Cursor(getCursor().getParentOrThrow(), invocation),
                                invocation.getCoordinates().replaceArguments(),
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

    /**
     * The new merge returns the entity, so a Kotlin {@code fun f() = ds.merge(e, wc)} would change its inferred return type from
     * {@code Unit}, or stop compiling if {@code Unit} is declared. Those calls are left for the user to migrate.
     */
    private static boolean isKotlinExpressionBody(Cursor cursor) {
        if (!isKotlin(cursor)) {
            return false;
        }
        Cursor block = cursor.getParentTreeCursor();
        if (!(block.getValue() instanceof J.Return)) {
            return false;
        }
        // Kotlin wraps the implicit J.Return in a K.Return
        while (block.getValue() instanceof J.Return || block.getValue() instanceof K.Return) {
            block = block.getParentTreeCursor();
        }
        return block.getValue() instanceof J.Block body
                && body.getMarkers().findFirst(SingleExpressionBlock.class).isPresent()
                && block.getParentTreeCursor().getValue() instanceof J.MethodDeclaration;
    }
}
