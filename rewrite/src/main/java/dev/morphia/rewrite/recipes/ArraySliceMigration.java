package dev.morphia.rewrite.recipes;

import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.NewClass;
import org.openrewrite.java.tree.JavaType;

public class ArraySliceMigration extends Recipe {
    private static final MethodMatcher ARRAY_SLICE_CONSTRUCTOR = new MethodMatcher("dev.morphia.query.ArraySlice ArraySlice(int)");

    @Override
    public @DisplayName String getDisplayName() {
        return "ArraySlice constructor migration";
    }

    @Override
    public @Description String getDescription() {
        return "Migrates usages of the ArraySlice constructor to the ArraySlice.limit().";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public J visitNewClass(NewClass newClass, ExecutionContext executionContext) {
                System.out.println("newClass = " + newClass + ", executionContext = " + executionContext);
                JavaTemplate template = JavaTemplate.builder("ArraySlice.limit(#{any(int)})")
                        .imports("dev.morphia.query.ArraySlice")
                        .build();
                List<Expression> arguments = newClass.getArguments();
                JavaType javaType = visitType(JavaType.buildType("dev.morphia.query.ArraySlice"), executionContext);
                return null;
                //                template.<MethodInvocation> apply(
                //                        updateCursor(newClass),
                //                        newClass.getCoordinates().replace(),
                //                        (Object[]) arguments.toArray(new Expression[0]))
                //                        .withMethodType(javaType);
            }

        };
    }
}
