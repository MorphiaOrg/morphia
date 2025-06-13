package dev.morphia.rewrite.recipes.pipeline;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.mongodb.lang.Nullable;

import dev.morphia.query.MorphiaCursor;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.AGGREGATE_ANYTHING;
import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.AGGREGATION;
import static dev.morphia.rewrite.recipes.RewriteUtils.methodMatcher;
import static java.util.List.of;

public class PipelineExecuteRewrite extends Recipe {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineExecuteRewrite.class);

    private static final MethodMatcher EXECUTE = methodMatcher(AGGREGATION, "execute(..)");
    private static final JavaType MORPHIA_CURSOR = JavaType.buildType(MorphiaCursor.class.getName());

    @Override
    public @NotNull String getDisplayName() {
        return "Aggregation pipeline execute() rewrite";
    }

    @Override
    public @NotNull String getDescription() {
        return "Rewrites uses of execute() moving an options to Datastore.aggregate().";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {

        return new JavaIsoVisitor<>() {
            private Expression targetType;
            private MethodInvocation options;

            @Override
            public @NotNull MethodInvocation visitMethodInvocation(@NotNull MethodInvocation original, @NotNull ExecutionContext context) {
                MethodInvocation invocation = super.visitMethodInvocation(original, context);

                if (EXECUTE.matches(invocation)) {
                    LOG.debug("matches invocation = {}", invocation);
                    var arguments = invocation.getArguments();
                    targetType = arguments.size() != 0 ? arguments.get(0) : null;
                    options = arguments.size() > 1 ? (MethodInvocation) arguments.get(1) : null;
                    invocation = (MethodInvocation) propagate(
                            invocation
                                    .withArguments(of())
                                    .withName(invocation.getName().withSimpleName("iterator"))
                                    .withMethodType(invocation.getMethodType()
                                            .withName("iterator")
                                            .withParameterNames(of())
                                            .withParameterTypes(of())
                                            .withReturnType(MORPHIA_CURSOR)));
                    invocation = invocation.withName(invocation.getName()
                            .withType(MORPHIA_CURSOR));
                    LOG.debug("now invocation = {}", invocation);
                }

                return invocation;
            }

            private @NotNull List<Expression> mergeArguments(List<Expression> arguments) {
                Expression sourceType = null;
                MethodInvocation options = this.options;
                for (final Expression expression : arguments) {
                    if (expression instanceof MethodInvocation methodInvocation) {
                        options = mergeOptions(methodInvocation);
                    } else {
                        sourceType = expression;
                    }
                }
                return Stream.of(sourceType, targetType, options)
                        .filter(Objects::nonNull)
                        .toList();
            }

            private MethodInvocation mergeOptions(@Nullable MethodInvocation argumentOptions) {
                if (options == null && argumentOptions == null) {
                    return null;
                } else if (options != null && argumentOptions == null) {
                    return options;
                } else if (options == null) {
                    return argumentOptions;
                }
                return attach(argumentOptions, options);
            }

            private MethodInvocation attach(MethodInvocation target, MethodInvocation option) {
                if (option.getSelect() instanceof MethodInvocation select) {
                    return option.withSelect(attach(target, select));
                } else {
                    return option.withSelect(target);
                }
            }

            private Expression propagate(Expression expression) {
                if (expression instanceof MethodInvocation invocation) {
                    if (AGGREGATE_ANYTHING.matches(invocation)) {
                        return invocation.withArguments(mergeArguments(invocation.getArguments()));
                    }
                    return invocation.withSelect(propagate(invocation.getSelect()));
                } else {
                    return expression;
                }
            }
        };
    }

}
