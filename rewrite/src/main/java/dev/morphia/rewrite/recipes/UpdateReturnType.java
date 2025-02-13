package dev.morphia.rewrite.recipes;

import java.util.Collections;

import com.mongodb.client.result.UpdateResult;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.ParameterizedType;
import org.openrewrite.java.tree.J.VariableDeclarations;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Variable;
import org.openrewrite.java.tree.TypeTree;

public class UpdateReturnType extends Recipe {
    private static final TypeMatcher TYPE_MATCHER = new TypeMatcher("dev.morphia.query.Update");
    private static final String UPDATE_RESULT = UpdateResult.class.getName();

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Updates the assignment of the results of update()";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "Since update() now returns the entity, the assignment needs to be updated to not expect the now-missing Update object.";
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public J visitVariableDeclarations(VariableDeclarations multiVariable, ExecutionContext executionContext) {
                if (multiVariable.getTypeExpression() instanceof ParameterizedType typeExpression && TYPE_MATCHER.matches(typeExpression)) {
                    maybeAddImport(UPDATE_RESULT, false);
                    var variable = multiVariable.getVariables().get(0);
                    JavaType updateResult = JavaType.buildType(UPDATE_RESULT);
                    Variable variableType = variable.getVariableType()
                            .withType(updateResult);

                    VariableDeclarations declaration = multiVariable
                            //                            .withTypeExpression(multiVariable.getTypeExpression().withType(updateResult))
                            .withTypeExpression(TypeTree.build("UpdateResult").withType(updateResult))
                            .withVariables(Collections.singletonList(variable
                                    .withVariableType(variableType)));

                    maybeRemoveImport("dev.morphia.query.Update");
                    return maybeAutoFormat(multiVariable, declaration, executionContext);
                } else {
                    return super.visitVariableDeclarations(multiVariable, executionContext);
                }
            }
        };
    }
}
