package dev.morphia.rewrite.recipes.query;

import java.util.List;

import dev.morphia.query.ArraySlice;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.J.NewClass;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Method;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;

public class ArraySliceMigration extends Recipe {

    private static final MethodMatcher CONSTRUCTOR = new MethodMatcher(ArraySlice.class.getName() + " <init>(..)");

    @Override
    public @DisplayName String getDisplayName() {
        return "ArraySlice refactorings";
    }

    @Override
    public @Description String getDescription() {
        return "Migrates usages of the ArraySlice constructors to the factory methods.";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public J visitNewClass(NewClass newClass, ExecutionContext executionContext) {
                if (CONSTRUCTOR.matches(newClass)) {
                    List<Expression> arguments = newClass.getArguments();
                    var template = (switch (arguments.size()) {
                        case 1 -> JavaTemplate.builder("ArraySlice.limit(#{any()})");
                        case 2 -> JavaTemplate.builder("ArraySlice.limit(#{any()}).skip(#{any()})");
                        default -> throw new UnsupportedOperationException();
                    })
                            .javaParser(JavaParser.fromJavaVersion()
                                    .classpath(findMorphiaDependencies()))
                            .imports(ArraySlice.class.getName())
                            .build();

                    MethodInvocation applied = template.apply(getCursor(), newClass.getCoordinates().replace(),
                            arguments.toArray(new Object[0]));

                    String name;
                    List<JavaType> parameterTypes;
                    List<String> parameterNames;
                    if (arguments.size() == 1) {
                        name = "limit";
                        parameterTypes = newClass.getMethodType().getParameterTypes().subList(0, 1);
                        parameterNames = newClass.getMethodType().getParameterNames().subList(0, 1);
                    } else {
                        name = "skip";
                        parameterTypes = newClass.getMethodType().getParameterTypes().subList(1, 2);
                        parameterNames = newClass.getMethodType().getParameterNames().subList(1, 2);
                    }
                    Method method = newClass.getMethodType()
                            .withName(name)
                            .withParameterTypes(parameterTypes)
                            .withParameterNames(parameterNames);
                    applied = applied.withMethodType(method)
                            .withName(applied.getName().withType(method));
                    if (applied.getSimpleName().equals("skip")) {
                        name = "limit";
                        parameterTypes = newClass.getMethodType().getParameterTypes().subList(0, 1);
                        parameterNames = newClass.getMethodType().getParameterNames().subList(0, 1);

                        method = newClass.getMethodType()
                                .withName(name)
                                .withParameterTypes(parameterTypes)
                                .withParameterNames(parameterNames);

                        MethodInvocation limit = (MethodInvocation) applied.getSelect();
                        limit = limit.withName(limit.getName()
                                .withType(method))
                                .withMethodType(method)
                                .withSelect(limit.getSelect());
                        applied = applied.withSelect(limit);
                    }
                    return applied;
                } else {
                    return super.visitNewClass(newClass, executionContext);
                }
            }
        };
    }
}
