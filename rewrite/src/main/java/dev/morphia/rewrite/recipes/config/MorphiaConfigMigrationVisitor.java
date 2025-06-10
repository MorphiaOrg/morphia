package dev.morphia.rewrite.recipes.config;

import java.util.List;

import dev.morphia.config.MorphiaConfig;
import dev.morphia.rewrite.recipes.MultiMethodMatcher;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.Space;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaCore;
import static dev.morphia.rewrite.recipes.RewriteUtils.methodMatcher;

public class MorphiaConfigMigrationVisitor extends JavaIsoVisitor<ExecutionContext> {
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaConfigMigrationVisitor.class);

    private static final String MAPPER_OPTIONS_BUILDER = "dev.morphia.mapping.MapperOptions.Builder";

    private static final MethodMatcher MAPPER_OPTIONS_MATCHER = methodMatcher("dev.morphia.mapping.MapperOptions", "builder()");
    private static final MethodMatcher MORPHIA_CONFIG_MATCHER = methodMatcher("dev.morphia.config.MorphiaConfig", "builder()");
    private static final MethodMatcher BUILDERS = new MultiMethodMatcher(MAPPER_OPTIONS_MATCHER, MORPHIA_CONFIG_MATCHER);

    private static MethodMatcher DEPRECATIONS = new MultiMethodMatcher(
            methodMatcher(MAPPER_OPTIONS_BUILDER, "isCacheClassLookups()"),
            methodMatcher(MAPPER_OPTIONS_BUILDER, "cacheClassLookups(..)"),
            methodMatcher(MAPPER_OPTIONS_BUILDER, "disableEmbeddedIndexes(..)"),
            methodMatcher(MAPPER_OPTIONS_BUILDER, "build()"));

    @Override
    public MethodInvocation visitMethodInvocation(@NotNull MethodInvocation methodInvocation, @NotNull ExecutionContext context) {
        if (DEPRECATIONS.matches(methodInvocation)) {
            MethodInvocation invocation = autoFormat(((MethodInvocation) methodInvocation.getSelect())
                    .withPrefix(Space.SINGLE_SPACE), context);
            LOG.debug("deprecations matched: \n\t{}\nupdated to: \n\t{}", methodInvocation, invocation);
            return invocation;
        } else if (BUILDERS.matches(methodInvocation)) {
            var template = JavaTemplate.builder("MorphiaConfig.load()")
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath(List.of(findMorphiaCore())))
                    .imports(MorphiaConfig.class.getName())
                    .build();

            MethodInvocation replacement = template.apply(getCursor(), methodInvocation.getCoordinates().replace());
            LOG.debug("builders matched: \n\t{}\nupdated to: \n\t{}", methodInvocation, replacement);
            return replacement;
        } else {
            LOG.debug("did not match: \n\t{}", methodInvocation);
            return super.visitMethodInvocation(methodInvocation, context);
        }
    }
}
