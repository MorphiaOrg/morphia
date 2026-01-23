package dev.morphia.rewrite.recipes.pipeline;

import dev.morphia.aggregation.AggregationOptions;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.NewClass;
import org.openrewrite.java.tree.JavaType;

import static dev.morphia.rewrite.recipes.RewriteUtils.runtimeClasspathPaths;

/**
 * Converts {@code new AggregationOptions()} to {@code AggregationOptions.aggregationOptions()}.
 * <p>
 * In Morphia 3.x, the AggregationOptions constructor is private and the static factory method
 * must be used instead.
 */
public class AggregationOptionsConstructorToFactory extends Recipe {
    private static final String AGGREGATION_OPTIONS_TYPE = AggregationOptions.class.getName();

    @Override
    public @NotNull String getDisplayName() {
        return "Convert AggregationOptions constructor to factory method";
    }

    @Override
    public @NotNull String getDescription() {
        return "Converts new AggregationOptions() to AggregationOptions.aggregationOptions() for Morphia 3.x compatibility.";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public J visitNewClass(NewClass newClass, ExecutionContext context) {
                J result = super.visitNewClass(newClass, context);
                if (!(result instanceof NewClass nc)) {
                    return result;
                }

                if (nc.getType() != null &&
                        nc.getType() instanceof JavaType.Class classType &&
                        AGGREGATION_OPTIONS_TYPE.equals(classType.getFullyQualifiedName()) &&
                        (nc.getArguments() == null || nc.getArguments().isEmpty() ||
                                (nc.getArguments().size() == 1
                                        && nc.getArguments().get(0) instanceof J.Empty))) {

                    maybeAddImport(AGGREGATION_OPTIONS_TYPE, false);

                    JavaTemplate template = JavaTemplate.builder("AggregationOptions.aggregationOptions()")
                            .javaParser(JavaParser.fromJavaVersion()
                                    .classpath(runtimeClasspathPaths()))
                            .imports(AGGREGATION_OPTIONS_TYPE)
                            .build();

                    return template.apply(getCursor(), nc.getCoordinates().replace());
                }

                return result;
            }
        };
    }
}
