package dev.morphia.rewrite.recipes.pipeline;

import java.util.List;

import dev.morphia.aggregation.Aggregation;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.FullyQualified;
import org.openrewrite.java.tree.JavaType.Method;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;
import static java.util.Arrays.stream;

public class ArgumentCollector {
    @NotNull
    private final Class<?> type;

    private final String method;

    private final MethodMatcher matcher;

    private final JavaTemplate template;

    public ArgumentCollector(Class<?> type, String method, Class<?>... imports) {
        this.type = type;
        this.method = method;
        matcher = new MethodMatcher("%s %s(..)".formatted(PipelineRewriteRecipes.AGGREGATION, method));

        String code = "%s.%s()".formatted(type.getSimpleName(), method);
        template = (JavaTemplate.builder(code))
                .javaParser(JavaParser.fromJavaVersion()
                        .classpath(findMorphiaDependencies()))
                .staticImports("%s.%s".formatted(type.getName(), method))
                .imports(type.getName())
                .imports(stream(imports)
                        .filter(t -> !t.isPrimitive())
                        .map(t -> t.isArray() ? t.getComponentType().getName() : t.getName())
                        .toArray(String[]::new))
                .build();
    }

    public boolean matches(JavaIsoVisitor<ExecutionContext> visitor, List<Expression> args, MethodInvocation invocation) {
        try {
            if (!matcher.matches(invocation)) {
                return false;
            }
            visitor.maybeAddImport(type.getName(), false);
            visitor.maybeAddImport(type.getName(), method, false);
            MethodInvocation applied = template.apply(new Cursor(visitor.getCursor(), invocation),
                    invocation.getCoordinates().replace());
            applied = applied.withArguments(invocation.getArguments()).withSelect(null);
            if (applied.getMethodType() == null) {
                applied = applied.withMethodType(new Method(null, 1, (FullyQualified) JavaType.buildType(type.getName()),
                        method, JavaType.buildType(Aggregation.class.getName()), List.of("T"),
                        List.of(JavaType.buildType(Object.class.getName())),
                        null, null, null, null));
            }
            args.add(applied);
            return true;
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException(template.getCode(), e);
        }
    }

}
