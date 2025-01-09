package dev.morphia.rewrite.recipes.internal;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodDeclaration;

public class RemoveMethodDeclaration extends Recipe {
    @Option(displayName = "Method pattern", description = "A pattern to match method declarations for removal.", example = "java.lang.StringBuilder append(java.lang.String)")
    private String methodPattern;

    @Override
    public String getDisplayName() {
        return "Remove method declarations";
    }

    @Override
    public String getDescription() {
        return "Remove method declarations.";
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveMethodDeclarationVisitor(methodPattern);
    }

    private class RemoveMethodDeclarationVisitor extends JavaVisitor<ExecutionContext> {
        private final MethodMatcher matcher;

        public RemoveMethodDeclarationVisitor(MethodMatcher matcher) {
            this.matcher = matcher;
        }

        public RemoveMethodDeclarationVisitor(String pattern) {
            this(new MethodMatcher(pattern));
        }

        @Override
        public J visitMethodDeclaration(MethodDeclaration method, ExecutionContext executionContext) {
            return matcher.matches(method.getMethodType()) ? null : super.visitMethodDeclaration(method, executionContext);
        }
    }
}
