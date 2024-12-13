package dev.morphia.rewrite.recipes;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.J.TypeCast;
import org.openrewrite.java.tree.JavaType.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            new MorphiaConfigMigrationVisitor());
    }

    private static class MorphiaConfigMigrationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher METHOD_MATCHER = new MethodMatcher("dev.morphia.Morphia createDatastore(..)");

        private final JavaTemplate databaseCall = JavaTemplate.builder("MorphiaConfig.load().database(#{})")
                                                              .build();

        @Override
        public MethodInvocation visitMethodInvocation(@NotNull MethodInvocation methodInvocation, @NotNull ExecutionContext context) {
            if (METHOD_MATCHER.matches(methodInvocation)) {
                maybeAddImport(NEW_TYPE);
                maybeRemoveImport(OLD_TYPE);

                List<Expression> arguments = methodInvocation.getArguments();
                if (arguments.size() != 3) {
                    return methodInvocation;
                }
                var databaseName = arguments.get(1);
                MethodInvocation load = findLoad(arguments.get(2));
                Cursor scope = new Cursor(getCursor(), load);
                MethodInvocation applied = databaseCall.apply(scope, load.getCoordinates().replace(), databaseName);

                TypeCast typeCast = (TypeCast) arguments.get(2);
                var expressions = flatten(typeCast);
                Method methodType = ((MethodInvocation) typeCast.getExpression()).getMethodType()
                                                                                 .withName("database")
                                                                                 .withParameterTypes(
                                                                                     of(buildType(String.class.getName())));
                expressions.add(2, applied.withMethodType(methodType));
                var argument = expressions.subList(1, expressions.size()).stream().reduce(expressions.get(0),
                    (current, next) -> ((MethodInvocation) next).withSelect(current));

                MethodInvocation after = methodInvocation
                                             .withMethodType(methodInvocation
                                                                 .getMethodType()
                                                                 .withParameterTypes(of(buildType(String.class.getName()),
                                                                     buildType(NEW_TYPE))))
                                             .withArguments(of(arguments.get(0), argument));
                return maybeAutoFormat(methodInvocation, after, context);
            } else {
                return super.visitMethodInvocation(methodInvocation, context);
            }
        }

        private static @NotNull ArrayList<Expression> flatten(TypeCast typeCast) {
            var expressions = new ArrayList<Expression>();
            var expression = typeCast.getExpression();
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

        private MethodInvocation findLoad(Expression expression) {
            if (expression instanceof MethodInvocation methodInvocation) {
                if (methodInvocation.getSimpleName().equals("load")) {
                    return methodInvocation;
                }
                Expression select = methodInvocation.getSelect();
                if (select instanceof MethodInvocation selectInvoke) {
                    if (selectInvoke.getSimpleName().equals("load")) {
                        return methodInvocation;
                    }
                    return findLoad(select);
                }
            } else if (expression instanceof TypeCast typeCast) {
                return findLoad(typeCast.getExpression());
            }
            throw new IllegalStateException("Could not find load()");
        }
    }
}