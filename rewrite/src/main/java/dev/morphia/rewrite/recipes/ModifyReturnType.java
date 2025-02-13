package dev.morphia.rewrite.recipes;

import java.util.Collections;
import java.util.UUID;

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
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Variable;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;

public class ModifyReturnType extends Recipe {
    private static final TypeMatcher TYPE_MATCHER = new TypeMatcher("dev.morphia.query.Modify");

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Updates the assignment of the results of modify()";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "Since modify() now returns the entity, the assignment needs to be updated to not expect the now-missing Modify object.";
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public J visitVariableDeclarations(VariableDeclarations multiVariable, ExecutionContext executionContext) {
                if (multiVariable.getTypeExpression() instanceof ParameterizedType typeExpression && TYPE_MATCHER.matches(typeExpression)) {
                    var parameterType = typeExpression.getTypeParameters().get(0).toString();
                    var variable = multiVariable.getVariables().get(0);
                    Variable variableType = variable.getVariableType()
                            .withType(JavaType.buildType(parameterType));
                    variable = new NamedVariable(UUID.randomUUID(), variable.getPrefix(), variable.getMarkers(), variable.getName(),
                            variable.getDimensionsAfterName(),
                            new JLeftPadded<>(Space.SINGLE_SPACE, variable.getInitializer(), Markers.EMPTY),
                            variableType);
                    VariableDeclarations declaration = multiVariable
                            .withTypeExpression(TypeTree.build(parameterType))
                            .withVariables(Collections.singletonList(variable));

                    maybeRemoveImport("dev.morphia.query.Modify");
                    return maybeAutoFormat(multiVariable, declaration, executionContext);
                } else {
                    return super.visitVariableDeclarations(multiVariable, executionContext);
                }
            }
        };
    }
}
