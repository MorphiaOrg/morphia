package dev.morphia.rewrite.recipes;

import dev.morphia.rewrite.recipes.CreateDatastoreMigration.CreateDatastoreMigrationVisitor;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J.MethodInvocation;

public class MorphiaConfigMigration extends Recipe {
    private static final String OLD_TYPE = "dev.morphia.mapping.MapperOptions";

    @Override
    public String getDisplayName() {
        return "Migrate MapperOptions to MorphiaConfig";
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
        private static final MethodMatcher BUILDER_MATCHER = new MethodMatcher("dev.morphia.mapping.MapperOptions.Builder build()");

        @Override
        public MethodInvocation visitMethodInvocation(@NotNull MethodInvocation methodInvocation, @NotNull ExecutionContext context) {
            if (BUILDER_MATCHER.matches(methodInvocation)) {
                MethodInvocation invocation = (MethodInvocation) CreateDatastoreMigrationVisitor.convertToMorphiaConfig(getCursor(),
                        methodInvocation, null);
                return autoFormat(invocation, context);
            } else {
                return super.visitMethodInvocation(methodInvocation, context);
            }
        }
    }
}