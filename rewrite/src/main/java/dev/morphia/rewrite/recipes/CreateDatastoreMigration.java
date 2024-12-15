package dev.morphia.rewrite.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodInvocation;

import static java.util.List.of;
import static org.openrewrite.java.tree.JavaType.buildType;

public class CreateDatastoreMigration extends Recipe {
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
                new CreateDatastoreMigrationVisitor());
    }

    public static class CreateDatastoreMigrationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher METHOD_MATCHER = new MethodMatcher(
                "dev.morphia.Morphia createDatastore(com.mongodb.client.MongoClient, String, dev.morphia.mapping" +
                        ".MapperOptions)");

        @Override
        public MethodInvocation visitMethodInvocation(@NotNull MethodInvocation methodInvocation, @NotNull ExecutionContext context) {
            if (METHOD_MATCHER.matches(methodInvocation)) {
                maybeAddImport(NEW_TYPE, null, false);
                maybeRemoveImport(OLD_TYPE);

                List<Expression> arguments = methodInvocation.getArguments();
                var databaseName = arguments.get(1);

                MethodInvocation after = methodInvocation
                        .withMethodType(methodInvocation
                                .getMethodType()
                                .withParameterTypes(of(buildType(String.class.getName()),
                                        buildType(NEW_TYPE))))
                        .withArguments(of(arguments.get(0), convertToMorphiaConfig(getCursor(), arguments.get(2), databaseName)));
                return maybeAutoFormat(methodInvocation, after, context);
            } else {
                return super.visitMethodInvocation(methodInvocation, context);
            }
        }

        public static Expression convertToMorphiaConfig(Cursor cursor, Expression builder, @Nullable Expression databaseName) {
            JavaTemplate databaseCall = (databaseName != null
                    ? JavaTemplate.builder("MorphiaConfig.load().database(#{})")
                    : JavaTemplate.builder("MorphiaConfig.load()"))
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath("morphia-core"))
                    .imports(NEW_TYPE)
                    .build();

            Cursor scope = new Cursor(cursor, builder);
            MethodInvocation applied;
            if (databaseName != null) {
                applied = databaseCall.apply(scope, builder.getCoordinates().replace(), databaseName);
            } else {
                applied = databaseCall.apply(scope, builder.getCoordinates().replace());
            }

            var expressions = flatten(builder);

            expressions.set(0, applied);
            expressions.remove(1);
            expressions.removeIf(e -> e instanceof MethodInvocation mi && mi.getSimpleName().equals("build"));
            return expressions.subList(1, expressions.size()).stream().reduce(expressions.get(0),
                    (current, next) -> ((MethodInvocation) next).withSelect(current));
        }

        private static @NotNull ArrayList<Expression> flatten(Expression start) {
            var expressions = new ArrayList<Expression>();
            var expression = start;
            while (expression != null) {
                if (expression instanceof MethodInvocation invocation) {
                    expressions.add(invocation);
                    expression = invocation.getSelect();
                } else if (expression instanceof Identifier identifier) {
                    expressions.add(identifier);
                    expression = null;
                }
            }
            Collections.reverse(expressions);
            return expressions;
        }
    }
}