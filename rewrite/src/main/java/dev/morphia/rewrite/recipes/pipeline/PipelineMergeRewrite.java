package dev.morphia.rewrite.recipes.pipeline;

import dev.morphia.aggregation.stages.Merge;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.AGGREGATION;
import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.addStage;
import static dev.morphia.rewrite.recipes.RewriteUtils.methodMatcher;

public class PipelineMergeRewrite extends Recipe {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineMergeRewrite.class);

    private static final MethodMatcher MERGE = new MethodMatcher(AGGREGATION + " merge(..)");
    private static final MethodMatcher INTO = methodMatcher(Merge.class.getName(), "into(..)");

    @Override
    public @NotNull String getDisplayName() {
        return "Aggregation pipeline merge() rewrite";
    }

    @Override
    public @NotNull String getDescription() {
        return "Rewrites uses of merge() moving the parameters in to pipeline().";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public @NotNull MethodInvocation visitMethodInvocation(@NotNull MethodInvocation original, @NotNull ExecutionContext context) {
                if (MERGE.matches(original)) {
                    MethodInvocation invocation = super.visitMethodInvocation(original, context);
                    LOG.debug("PipelineMergeRewrite matches");
                    LOG.debug("invocation = {}", invocation);
                    var arguments = invocation.getArguments();
                    var mergeInfo = extractArgument((MethodInvocation) arguments.get(0));
                    invocation = maybeAutoFormat(original, (MethodInvocation) addStage(invocation, mergeInfo), context);
                    LOG.debug("now invocation = {}", invocation);

                    maybeRemoveImport("dev.morphia.aggregation.stages.Merge.into");
                    maybeAddImport("dev.morphia.aggregation.stages.Merge");
                    maybeAddImport("dev.morphia.aggregation.stages.Merge", "merge");

                    return invocation;
                }

                return original;
            }

            private MethodInvocation extractArgument(MethodInvocation argument) {
                if (!INTO.matches(argument)) {
                    return argument.withSelect(extractArgument((MethodInvocation) argument.getSelect()));
                } else {
                    Method methodType = argument.getMethodType().withName("merge");
                    MethodInvocation invocation = argument
                            .withMethodType(methodType)
                            .withName(argument.getName()
                                    .withSimpleName("merge")
                                    .withType(methodType));
                    return invocation;
                }

            }
        };
    }

}
