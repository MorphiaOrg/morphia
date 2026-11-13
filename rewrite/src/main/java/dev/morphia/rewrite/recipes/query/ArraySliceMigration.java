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
import org.openrewrite.java.tree.JavaType.Method;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;
import static dev.morphia.rewrite.recipes.RewriteUtils.isSideEffectFree;

/**
 * An OpenRewrite recipe that migrates {@code new ArraySlice(...)} constructor calls to the equivalent
 * {@code ArraySlice.limit()} and {@code ArraySlice.limit().skip()} factory methods.
 */
public class ArraySliceMigration extends Recipe {
    /** Creates a new instance. */
    public ArraySliceMigration() {
    }

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
                if (!CONSTRUCTOR.matches(newClass)) {
                    return super.visitNewClass(newClass, executionContext);
                }
                List<Expression> arguments = newClass.getArguments();
                Method constructor = newClass.getMethodType();
                if (arguments.size() == 1) {
                    MethodInvocation limit = template("ArraySlice.limit(#{any()})")
                            .apply(getCursor(), newClass.getCoordinates().replace(), arguments.get(0));
                    return withType(limit, constructor, "limit", 0);
                } else if (arguments.size() == 2) {
                    // 2.x is ArraySlice(int skip, int limit) but the factory needs the limit first
                    Expression skip = arguments.get(0);
                    Expression limit = arguments.get(1);
                    if (!canReorder(skip, limit)) {
                        return newClass;
                    }
                    MethodInvocation applied = template("ArraySlice.limit(#{any()}).skip(#{any()})")
                            .apply(getCursor(), newClass.getCoordinates().replace(), limit, skip);
                    MethodInvocation limitCall = withType((MethodInvocation) applied.getSelect(), constructor, "limit", 1);
                    return withType(applied.withSelect(limitCall), constructor, "skip", 0);
                }
                return newClass;
            }
        };
    }

    /**
     * The limit is now evaluated before the skip, so the arguments can only be swapped when that can't change what
     * either of them evaluates to: when one is a literal, or when neither has side effects.
     */
    private static boolean canReorder(Expression skip, Expression limit) {
        return skip instanceof J.Literal || limit instanceof J.Literal
                || isSideEffectFree(skip) && isSideEffectFree(limit);
    }

    private static JavaTemplate template(String code) {
        return JavaTemplate.builder(code)
                .javaParser(JavaParser.fromJavaVersion()
                        .classpath(findMorphiaDependencies()))
                .imports(ArraySlice.class.getName())
                .build();
    }

    /**
     * Types the call as a one-parameter method named {@code name}, taking the constructor's parameter at {@code index}.
     */
    private static MethodInvocation withType(MethodInvocation invocation, Method constructor, String name, int index) {
        if (constructor == null) {
            return invocation;
        }
        Method method = constructor
                .withName(name)
                .withParameterTypes(constructor.getParameterTypes().subList(index, index + 1))
                .withParameterNames(constructor.getParameterNames().subList(index, index + 1));
        return invocation.withMethodType(method)
                .withName(invocation.getName().withType(method));
    }
}
