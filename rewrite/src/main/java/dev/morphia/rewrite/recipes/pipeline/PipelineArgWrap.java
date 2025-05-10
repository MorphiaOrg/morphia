package dev.morphia.rewrite.recipes.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dev.morphia.aggregation.stages.Stage;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaCore;
import static dev.morphia.rewrite.recipes.pipeline.PipelineRollup.AGGREGATION;

public class PipelineArgWrap extends Recipe {

    private final MethodMatcher matcher;

    private final @NotNull JavaTemplate template;

    private final Class<? extends Stage> type;

    private final String method;

    private final String argTypes;

    private final Class<?>[] imports;

    public PipelineArgWrap(Class<? extends Stage> type, String method, String argTypes, Class<?>... imports) {
        this.type = type;
        this.method = method;
        this.argTypes = argTypes;
        this.imports = imports;
        template = template(type, method, imports);
        matcher = new MethodMatcher("%s %s(%s)".formatted(AGGREGATION, method, argTypes));
    }

    @Override
    public @DisplayName String getDisplayName() {
        return "Wraps the 'bare' stage arguments in their Stage methods";
    }

    @Override
    public @Description String getDescription() {
        return "Wraps the 'bare' stage arguments in their Stage methods.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public MethodInvocation visitMethodInvocation(MethodInvocation invocation, ExecutionContext executionContext) {
                //                System.out.printf("[%s.%s] matcher = %s%n, invocation = %s%n", type.getSimpleName(), method, matcher, invocation);
                if (matcher.matches(invocation)) {
                    maybeAddImport(type.getName(), method, false);
                    for (Class<?> anImport : imports) {
                        maybeAddImport(anImport.getName(), false);
                    }
                    Expression candidate = invocation;
                    List<Expression> methods = new ArrayList<>();
                    MethodInvocation method;
                    while (matcher.matches(candidate)) {
                        method = (MethodInvocation) candidate;
                        MethodInvocation applied = template.apply(new Cursor(getCursor(), method),
                                method.getCoordinates().replaceArguments());
                        applied = applied.withSelect(null)
                                .withArguments(method.getArguments());

                        methods.add(method.withArguments(Collections.singletonList(applied)));
                        candidate = method.getSelect();
                    }
                    Expression reduced = methods.stream().reduce(candidate, (parent, mi) -> ((MethodInvocation) mi).withSelect(parent));
                    return (MethodInvocation) reduced;

                } else {
                    return super.visitMethodInvocation(invocation, executionContext);
                }
            }
        };
    }

    private static @NotNull JavaTemplate template(Class<?> type, String method, Class<?>... imports) {
        return (JavaTemplate.builder("%s.%s()".formatted(type.getName(), method)))
                .javaParser(JavaParser.fromJavaVersion()
                        .classpath(List.of(findMorphiaCore().toPath())))
                .imports(type.getName())
                .imports(Arrays.stream(imports).map(Class::getName).toList().toArray(new String[0]))
                .contextSensitive()
                .build();
    }

}
