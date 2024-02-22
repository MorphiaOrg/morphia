package dev.morphia.rewrite.recipes;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;

public class UnwrapFieldExpressions extends Recipe {
    private static final MethodMatcher fieldMatcher = new MethodMatcher("dev.morphia.aggregation.expressions.Expressions field(String)");
    private static final MethodMatcher valueMatcher = new MethodMatcher("dev.morphia.aggregation.expressions.Expressions value(Object)");

    @Override
    public String getDisplayName() {
        return "Unwrap field expressions that use Expressions#field()";
    }

    @Override
    public String getDescription() {
        return "Field names can be used directly now so this unwraps any uses of the field() method to just the raw String.";
    }

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {

            @Override
            public MethodInvocation visitMethodInvocation(MethodInvocation method, @NotNull ExecutionContext context) {
                var list = method.getArguments().stream()
                        .map(argument -> {
                            if (argument instanceof MethodInvocation invocation) {
                                if (fieldMatcher.matches(invocation) || valueMatcher.matches(invocation)) {
                                    return invocation.getArguments().get(0);
                                } else {
                                    return visitMethodInvocation(invocation, context);
                                }
                            } else {
                                return argument;
                            }
                        })
                        .collect(Collectors.toList());
                return method.withArguments(list);
            }

            private MethodInvocation visitMethodInvocation2(MethodInvocation method, @NotNull ExecutionContext context) {
                /*
                 * System.out.println("method = " + method);
                 * System.out.println("** method.getArguments() = \n\t" + method.getArguments());
                 * method.getArguments().stream()
                 * .filter(a -> a instanceof MethodInvocation)
                 * .forEach(a -> visitMethodInvocation((MethodInvocation) a, context));
                 * return method;
                 */
                List<Expression> arguments1 = method.getArguments();
                List<Expression> arguments = arguments1.stream().map(e -> e.unwrap()).map(argument -> {
                    if (argument instanceof MethodInvocation invocation) {
                        return updateArguments(method, context, invocation);
                    }
                    return argument;
                }).collect(Collectors.toList());
                return method.withArguments(arguments);

                /*
                 * what should happen here:
                 * 
                 * in cases where users are calling field("somevalue")
                 * 
                 * this invocation should be replaced with just "somevalue"
                 * 
                 * so
                 * gte(field("somevalue"), 42)
                 * becomes
                 * gte("somevalue", 42)
                 * 
                 * 
                 */
                //                return super.visitMethodInvocation(method, executionContext);
            }

            @NotNull
            private MethodInvocation updateArguments(MethodInvocation method,
                    ExecutionContext executionContext,
                    MethodInvocation invocation) {

                List<Expression> newArgs = method.getArguments().stream().map(a -> {
                    if (a instanceof MethodInvocation invoke) {
                        if (!fieldMatcher.matches(invoke.getMethodType())) {
                            return visitMethodInvocation(invoke, executionContext);
                        } else {
                            List<Expression> invocationArguments = invoke.getArguments();
                            System.out.println("invocationArguments = " + invocationArguments);
                            return invocationArguments.get(0);
                        }
                    }
                    return a;
                }).collect(Collectors.toList());
                /*
                 * if (!matcher.matches(invocation.getMethodType())) {
                 * return visitMethodInvocation(invocation, executionContext);
                 * } else {
                 * List<Expression> invocationArguments = invocation.getArguments();
                 * System.out.println("invocationArguments = " + invocationArguments);
                 * return method.withArguments(invocationArguments);
                 * }
                 */
                return invocation.withArguments(newArgs);

            }

        };
    }
}
