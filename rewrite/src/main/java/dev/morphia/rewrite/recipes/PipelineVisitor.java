package dev.morphia.rewrite.recipes;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;

class PipelineVisitor extends JavaIsoVisitor<ExecutionContext> {

    @Override
    public MethodInvocation visitMethodInvocation(MethodInvocation methodInvocation, @NotNull ExecutionContext context) {
        if (PipelineRewriteStage1.pipeline.matches(methodInvocation)) {
            Expression select = methodInvocation.getSelect();
            System.out.println("\n\nselect = " + select);
            System.out.println("select.getSideEffects() = " + select.getSideEffects());
            if (select instanceof MethodInvocation invocation) {
                System.out.println("invocation.getArguments() = " + invocation.getArguments());
            } else {
                System.out.println("select.getType() = " + select.getType());
            }
            return super.visitMethodInvocation(methodInvocation, context);
        }
        return super.visitMethodInvocation(methodInvocation, context);
    }
}
