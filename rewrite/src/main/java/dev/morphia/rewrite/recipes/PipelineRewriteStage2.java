package dev.morphia.rewrite.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Block;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.List.of;
import static org.openrewrite.java.tree.JRightPadded.build;

public class PipelineRewriteStage2 extends Recipe {

    static final String AGGREGATION = "dev.morphia.aggregation.Aggregation";

    static final MethodMatcher PIPELINE = new MethodMatcher(PipelineRewriteStage2.AGGREGATION + " pipeline(..)");

    private final JavaTemplate pipelineTemplate = null; //JavaTemplate.builder("pipeline(..)").contextSensitive().build();

    @Override
    public String getDisplayName() {
        return "Aggregation pipeline rewrite";
    }

    @Override
    public String getDescription() {
        return "Rewrites an aggregation from using stage-named methods to using pipeline(Stage...).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(PIPELINE), new JavaIsoVisitor<>() {

            public MethodInvocation visitMethodInvocation(MethodInvocation method, ExecutionContext p) {
                // exit if method doesn't match isEqualTo(..)
                if (!PIPELINE.matches(method.getSelect()/*.getMethodType()*/)) {
                    return method;
                }

                var arguments = new ArrayList<JRightPadded<Expression>>();
                var select = method.getSelect();
                while (PIPELINE.matches(select)) {
//                    System.out.println("select = " + select);
                    J.MethodInvocation invocation = (J.MethodInvocation) select;
                    arguments.add(build(invocation.getArguments().get(0)));
                    select = invocation.getSelect();
                }
                System.out.println("done:  select = " + select);
                Collections.reverse(arguments);
                Markers markers = new Markers(UUID.randomUUID(), of());
                Identifier identifier = (Identifier) select;
                Space prefix = method.getPrefix();
                var newInvocation = new MethodInvocation(
                    UUID.randomUUID(), prefix, markers, build(method.getSelect()), null, identifier,
                    JContainer.build(arguments),
                    method.getMethodType());
                return newInvocation;
            }
/*
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block bl = super.visitBlock(block, ctx);
                return bl.withStatements(rewritePipelineStatements(bl));
            }
*/

/*
            private MethodInvocation rewritePipelineStatements(Block bl) {
                List<Statement> statements = new ArrayList<>();
                for (var statement : bl.getStatements()) {
                    if (statement instanceof J.MethodInvocation && isPipeline(statement)) {
                        List<Expression> arguments = new ArrayList<>();
                        J.MethodInvocation pipeline = (J.MethodInvocation) statement;
                        var select = pipeline.getSelect();
                        while (PIPELINE.matches(select)) {
                            System.out.println("select = " + select);
                            J.MethodInvocation invocation = (J.MethodInvocation) select;
                            arguments.add(invocation.getArguments().get(0));
                            select = invocation.getSelect();
                        }
                        System.out.println("done:  select = " + select);
                        Collections.reverse(arguments);
//                        Markers markers = new Markers(UUID.randomUUID(), of());
//                        Identifier identifier = (Identifier) select;
//                        Space prefix = statement.getPrefix();
//                        var newInvocation = new MethodInvocation(
//                            UUID.randomUUID(), prefix, markers, null, null, identifier, arguments,
//                            ((MethodInvocation) statement).getMethodType() );
                        MethodInvocation m = pipelineTemplate.apply(getCursor(), statement.getCoordinates().replace());
                        return m;
                        statements.add((newInvocation));
                    } else {
                        statements.add(statement);
                    }
                }
                return statements;
            }
*/

            private boolean isPipeline(Statement statement) {
                J.MethodInvocation methodInvocation = (J.MethodInvocation) statement;
                // Only match method invocations where the select is an assertThat, containing a non-method call argument
                if (PIPELINE.matches(methodInvocation.getSelect())) {
                    J.MethodInvocation invocation = (J.MethodInvocation) methodInvocation.getSelect();
                    if (invocation != null && PIPELINE.matches(invocation.getSelect())) {
                        return true;
                    }
                }
                return false;
            }

            private J.MethodInvocation getCollapsedAssertThat(List<Statement> consecutiveAssertThatStatement) {
                assert !consecutiveAssertThatStatement.isEmpty();
                Space originalPrefix = consecutiveAssertThatStatement.get(0).getPrefix();
                String continuationIndent = originalPrefix.getIndent().contains("\t") ? "\t\t" : "        ";
                Space indentedNewline = Space.format(originalPrefix.getLastWhitespace().replaceAll("^\\s+\n", "\n") +
                                                     continuationIndent);
                J.MethodInvocation collapsed = null;
                for (Statement st : consecutiveAssertThatStatement) {
                    J.MethodInvocation assertion = (J.MethodInvocation) st;
                    J.MethodInvocation assertThat = (J.MethodInvocation) assertion.getSelect();
                    assert assertThat != null;
                    J.MethodInvocation newSelect = collapsed == null ? assertThat : collapsed;
                    collapsed = assertion.getPadding().withSelect(build((Expression) newSelect.withPrefix(Space.EMPTY))
                                                                      .withAfter(indentedNewline));
                }
                return collapsed.withPrefix(originalPrefix);
            }
        });
    }
}
