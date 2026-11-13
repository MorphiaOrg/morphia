package dev.morphia.rewrite.recipes.query;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType.Method;

/**
 * An OpenRewrite recipe that renames the removed {@code Query.execute()} and {@code Query.execute(FindOptions)} to
 * {@code iterator()} and {@code iterator(FindOptions)}, including method references such as
 * {@code query::execute}. This needs to run before {@link QueryFindOptions} so that any
 * {@code FindOptions} argument is then moved to {@code Datastore#find()}.
 */
public class QueryExecute extends Recipe {
    /** Creates a new instance. */
    public QueryExecute() {
    }

    private static final MethodMatcher EXECUTE = new MethodMatcher("dev.morphia.query.Query execute(..)");

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Replace Query.execute() with Query.iterator()";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "Renames the removed Query.execute() and Query.execute(FindOptions) to iterator() and iterator(FindOptions).";
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public MethodInvocation visitMethodInvocation(MethodInvocation method, ExecutionContext executionContext) {
                MethodInvocation invocation = super.visitMethodInvocation(method, executionContext);
                if (EXECUTE.matches(invocation)) {
                    Method type = invocation.getMethodType();
                    if (type != null) {
                        type = type.withName("iterator");
                    }
                    invocation = invocation
                            .withName(invocation.getName().withSimpleName("iterator").withType(type))
                            .withMethodType(type);
                }
                return invocation;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext executionContext) {
                J.MemberReference reference = super.visitMemberReference(memberRef, executionContext);
                Method type = reference.getMethodType();
                if (type != null && EXECUTE.matches(type)) {
                    Method newType = type.withName("iterator");
                    reference = reference
                            .withReference(reference.getReference().withSimpleName("iterator").withType(newType))
                            .withMethodType(newType);
                }
                return reference;
            }
        };
    }
}
