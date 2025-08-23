package dev.morphia.rewrite.recipes.pipeline;

import dev.morphia.aggregation.stages.Out;

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

import static dev.morphia.rewrite.recipes.RewriteUtils.methodMatcher;
import static dev.morphia.rewrite.recipes.pipeline.PipelineRewriteRecipes.AGGREGATION;
import static dev.morphia.rewrite.recipes.pipeline.PipelineRewriteRecipes.addStage;

public class PipelineOutRewrite extends Recipe {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineOutRewrite.class);

    private static final MethodMatcher OUT = methodMatcher(AGGREGATION, "out(..)");
    private static final MethodMatcher DATABASE = methodMatcher(Out.class.getName(), "database(..)");

    @Override
    public @NotNull String getDisplayName() {
        return "Aggregation pipeline out() rewrite";
    }

    @Override
    public @NotNull String getDescription() {
        return "Rewrites uses of out() moving the parameters in to pipeline().";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public @NotNull MethodInvocation visitMethodInvocation(@NotNull MethodInvocation original, @NotNull ExecutionContext context) {

                if (OUT.matches(original)) {
                    MethodInvocation invocation = super.visitMethodInvocation(original, context);
                    LOG.debug("PipelineOutRewrite matches");
                    LOG.debug("invocation = {}", invocation);
                    var arguments = invocation.getArguments();
                    var outInfo = extractTarget((MethodInvocation) arguments.get(0));
                    invocation = maybeAutoFormat(original, (MethodInvocation) addStage(invocation, outInfo), context);
                    LOG.debug("now invocation = {}", invocation);
                    return invocation;
                }

                return original;
            }

            private MethodInvocation extractTarget(MethodInvocation argument) {

                MethodInvocation to;
                MethodInvocation database = null;
                if (DATABASE.matches(argument)) {
                    database = argument;
                    to = (MethodInvocation) argument.getSelect();
                } else {
                    to = argument;
                }
                maybeRemoveImport("dev.morphia.aggregation.stages.Out.to");
                maybeAddImport("dev.morphia.aggregation.stages.Out");
                maybeAddImport("dev.morphia.aggregation.stages.Out", "out");

                Method type = to.getMethodType()
                        .withName("out");
                to = to.withMethodType(type)
                        .withName(to.getName()
                                .withSimpleName("out")
                                .withType(type));
                return database == null
                        ? to
                        : database.withSelect(to);
            }
        };
    }

}
