package dev.morphia.rewrite.recipes;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;

public class MorphiaConfigMigration extends Recipe {
    private static final String OLD_TYPE = "dev.morphia.mapping.MapperOptions";

    private static final String NEW_TYPE = "dev.morphia.config.MorphiaConfig";

    @Override
    public String getDisplayName() {
        return "Migrate Morphia MapperOptions to MorphiaConfig";
    }

    @Override
    public String getDescription() {
        return "Converts uses of dev.morphia.mapping.MapperOptions to dev.morphia.config.MorphiaConfig.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(OLD_TYPE, true),
                new MorphiaConfigMigrationVisitor());
    }

    private static class MorphiaConfigMigrationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher BUILDER_MATCHER = new MethodMatcher("dev.morphia.mapping.MapperOptions builder()");

        @Override
        public MethodInvocation visitMethodInvocation(@NotNull MethodInvocation methodInvocation, @NotNull ExecutionContext context) {
            if (BUILDER_MATCHER.matches(methodInvocation)) {
                return methodInvocation.withName(methodInvocation.getName().withSimpleName("load"))
                        .withSelect(((Identifier) methodInvocation.getSelect())
                                .withSimpleName("MorphiaConfig")
                                .withType(JavaType.buildType(NEW_TYPE)));
            } else {
                return super.visitMethodInvocation(methodInvocation, context);
            }
        }
    }
}