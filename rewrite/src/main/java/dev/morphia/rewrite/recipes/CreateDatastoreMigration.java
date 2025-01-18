package dev.morphia.rewrite.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Method;

import static java.util.List.of;

public class CreateDatastoreMigration extends Recipe {
    private static final String OLD_TYPE = "dev.morphia.mapping.MapperOptions";

    private static final String NEW_TYPE = "dev.morphia.config.MorphiaConfig";

    private static List<String> DEPRECATIONS = List.of("cacheClassLookups",
            "disableEmbeddedIndexes",
            "build");

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
        return new CreateDatastoreMigrationVisitor();
    }

    public static class CreateDatastoreMigrationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final List<MethodMatcher> METHOD_MATCHERS = List.of(
                new MethodMatcher("dev.morphia.Morphia createDatastore(com.mongodb.client.MongoClient, String)"),
                new MethodMatcher(
                        "dev.morphia.Morphia createDatastore(com.mongodb.client.MongoClient, String, dev.morphia.mapping.MapperOptions)"));
        //        private static final MethodMatcher STATIC_METHOD_MATCHER = new MethodMatcher("createDatastore(..)");

        @Override
        public MethodInvocation visitMethodInvocation(@NotNull MethodInvocation methodInvocation, @NotNull ExecutionContext context) {
            if (METHOD_MATCHERS.stream().anyMatch(matcher -> matcher.matches(methodInvocation))) {
                maybeAddImport(NEW_TYPE, null, false);
                maybeAddImport("dev.morphia.Morphia", "createDatastore");
                maybeRemoveImport(OLD_TYPE);

                List<Expression> arguments = methodInvocation.getArguments();
                var databaseName = arguments.get(1);
                Expression options;
                if (arguments.size() == 3) {
                    options = convertToMorphiaConfig(getCursor(), arguments.get(2), databaseName);
                } else {
                    options = synthesizeMorphiaConfig(getCursor(), databaseName);
                }

                List<Expression> newArguments = of(arguments.get(0), options);
                List<JavaType> types = newArguments.stream().map(Expression::getType)
                        .toList();
                Method methodType = methodInvocation.getMethodType().withParameterTypes(types);
                MethodInvocation after = methodInvocation
                        .withMethodType(methodType)
                        .withArguments(newArguments)
                        .withName(methodInvocation.getName().withType(methodType));
                return maybeAutoFormat(methodInvocation, after, context);
            } else {
                return super.visitMethodInvocation(methodInvocation, context);
            }
        }

        public static Expression synthesizeMorphiaConfig(Cursor cursor, Expression databaseName) {
            JavaTemplate databaseCall = JavaTemplate.builder("MorphiaConfig.load().database(#{any(java.lang.String)})")
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath("morphia-core"))
                    .imports(NEW_TYPE)
                    .build();

            return databaseCall.apply(new Cursor(cursor, databaseName), databaseName.getCoordinates().replace(), databaseName);
        }

        public static Expression convertToMorphiaConfig(Cursor cursor, Expression builder, @Nullable Expression databaseName) {
            if (builder instanceof Identifier identifier) {
                return reuseArgument(cursor, identifier, databaseName);
            }
            JavaTemplate databaseCall = (databaseName != null
                    ? JavaTemplate.builder("MorphiaConfig.load().database(#{any(java.lang.String)})")
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

            var expressions = new ArrayList<>(flatten(builder)
                    .stream().filter(e -> {
                        return !(e instanceof MethodInvocation mi) || !DEPRECATIONS.contains(mi.getSimpleName());
                    })
                    .toList());

            expressions.set(0, applied);
            expressions.remove(1);

            return rechain(expressions);
        }

        private static Expression rechain(List<Expression> expressions) {
            return expressions.subList(1, expressions.size()).stream().reduce(expressions.get(0),
                    (current, next) -> ((MethodInvocation) next).withSelect(current));
        }

        public static Expression reuseArgument(Cursor cursor, Identifier identifier, @Nullable Expression databaseName) {
            JavaTemplate.Builder dbBuilder = JavaTemplate.builder(identifier.getSimpleName() + ".database(#{any})");
            JavaTemplate databaseCall = dbBuilder
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath("morphia-core"))
                    .build();

            return databaseCall.apply(new Cursor(cursor, identifier),
                    identifier.getCoordinates().replace(), databaseName);
        }

        private static Expression updateIdentifierType(Expression expression) {
            var flattened = flatten(expression);
            flattened.set(0, flattened.get(0).withType(JavaType.buildType(NEW_TYPE)));
            return rechain(flattened);
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