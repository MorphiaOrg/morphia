package dev.morphia.rewrite.recipes.internal;

import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodDeclaration;

/**
 * An OpenRewrite recipe that removes method declarations matching specified patterns.
 */
public class RemoveMethodDeclaration extends Recipe {
    /** Creates a new instance. */
    public RemoveMethodDeclaration() {
    }

    @Option(displayName = "Method pattern", description = "A pattern to match method declarations for removal.", example = "java.lang.StringBuilder append(java.lang.String)")
    private List<String> methodPatterns;

    @Override
    public String getDisplayName() {
        return "Remove method declarations";
    }

    @Override
    public String getDescription() {
        return "Remove method declarations.";
    }

    /**
     * Returns the method patterns used to identify declarations for removal.
     *
     * @return the list of method patterns
     */
    public List<String> getMethodPatterns() {
        return methodPatterns;
    }

    /**
     * Sets the method patterns used to identify declarations for removal.
     *
     * @param methodPatterns the list of method patterns
     */
    public void setMethodPatterns(List<String> methodPatterns) {
        this.methodPatterns = methodPatterns;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveMethodDeclarationVisitor(methodPatterns.stream().map(MethodMatcher::new).toList());
    }

    private class RemoveMethodDeclarationVisitor extends JavaVisitor<ExecutionContext> {
        private final List<MethodMatcher> matchers;

        public RemoveMethodDeclarationVisitor(List<MethodMatcher> matchers) {
            this.matchers = matchers;
        }

        @Override
        public J visitMethodDeclaration(MethodDeclaration method, ExecutionContext executionContext) {
            return matchers.stream().anyMatch(matcher -> matcher.matches(method.getMethodType()))
                    ? null
                    : super.visitMethodDeclaration(method, executionContext);
        }
    }
}
