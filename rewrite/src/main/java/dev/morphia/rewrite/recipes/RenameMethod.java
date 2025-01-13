package dev.morphia.rewrite.recipes;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;

public class RenameMethod extends Recipe {

    @Option(displayName = "Method pattern", description = "A pattern to match method declarations for removal.", example = "java.lang.StringBuilder append(java.lang.String)")
    private List<String> methodPatterns;

    @Override
    public @DisplayName String getDisplayName() {
        return "Renames a method";
    }

    @Override
    public @Description String getDescription() {
        return "Renames a method usually due to deprecation.";
    }

    public void setMethodPatterns(List<String> methodPatterns) {
        this.methodPatterns = methodPatterns;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RenameMethodVisitor(methodPatterns);
    }

    private static class RenameMethodVisitor extends JavaIsoVisitor<ExecutionContext> {
        private Map<MethodMatcher, String> replacements;

        public RenameMethodVisitor(List<String> patterns) {
            replacements = patterns.stream()
                    .collect(Collectors.toMap(pattern -> new MethodMatcher(pattern.substring(0, pattern.indexOf(')') + 1)),
                            pattern -> pattern.substring(pattern.indexOf(')') + 2)));
        }

        @Override
        public MethodInvocation visitMethodInvocation(MethodInvocation method, ExecutionContext executionContext) {
            for (Entry<MethodMatcher, String> entry : replacements.entrySet()) {
                if (entry.getKey().matches(method)) {
                    JavaType type = method.getType();
                    maybeAddImport(method.getMethodType().getDeclaringType().getFullyQualifiedName(), entry.getValue());
                    return method.withName(method.getName().withSimpleName(entry.getValue()));
                }
            }
            return super.visitMethodInvocation(method, executionContext);
        }
    }
}
