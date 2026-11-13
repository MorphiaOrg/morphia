package dev.morphia.rewrite.recipes.datastore;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Literal;
import org.openrewrite.java.tree.J.MethodInvocation;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;

/**
 * An OpenRewrite recipe that migrates the removed {@code FindAndModifyOptions} to its parent type, {@code ModifyOptions}. The only
 * method {@code FindAndModifyOptions} added, {@code returnNew(boolean)}, is rewritten to the equivalent {@code returnDocument(..)} call
 * before the type itself is changed.
 */
public class FindAndModifyOptionsMigration extends Recipe {
    /** Creates a new instance. */
    public FindAndModifyOptionsMigration() {
    }

    private static final String FIND_AND_MODIFY_OPTIONS = "dev.morphia.FindAndModifyOptions";

    private static final String RETURN_DOCUMENT = "com.mongodb.client.model.ReturnDocument";

    private static final MethodMatcher RETURN_NEW = new MethodMatcher(FIND_AND_MODIFY_OPTIONS + " returnNew(boolean)");

    @Override
    public @DisplayName String getDisplayName() {
        return "FindAndModifyOptions refactorings";
    }

    @Override
    public @Description String getDescription() {
        return "Migrates usages of FindAndModifyOptions to ModifyOptions, replacing returnNew(boolean) with returnDocument(ReturnDocument).";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public J visitMethodInvocation(MethodInvocation method, ExecutionContext executionContext) {
                J visited = super.visitMethodInvocation(method, executionContext);
                if (!(visited instanceof MethodInvocation invocation) || !RETURN_NEW.matches(invocation)
                        || invocation.getSelect() == null) {
                    return visited;
                }
                Expression returnNew = invocation.getArguments().get(0);
                String code;
                Object[] arguments;
                if (returnNew instanceof Literal literal && literal.getValue() instanceof Boolean value) {
                    code = "#{any(%s)}.returnDocument(ReturnDocument.%s)".formatted(FIND_AND_MODIFY_OPTIONS,
                            value ? "AFTER" : "BEFORE");
                    arguments = new Object[] { invocation.getSelect() };
                } else {
                    code = "#{any(%s)}.returnDocument(#{any(boolean)} ? ReturnDocument.AFTER : ReturnDocument.BEFORE)"
                            .formatted(FIND_AND_MODIFY_OPTIONS);
                    arguments = new Object[] { invocation.getSelect(), returnNew };
                }
                maybeAddImport(RETURN_DOCUMENT);
                MethodInvocation applied = JavaTemplate.builder(code)
                        .javaParser(JavaParser.fromJavaVersion()
                                .classpath(findMorphiaDependencies()))
                        .imports(RETURN_DOCUMENT)
                        .build()
                        .apply(updateCursor(invocation), invocation.getCoordinates().replace(), arguments);
                // keep any line break before the original .returnNew(..) in a chained call
                return applied.getPadding().withSelect(invocation.getPadding().getSelect().withElement(applied.getSelect()));
            }
        };
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new ChangeType(FIND_AND_MODIFY_OPTIONS, "dev.morphia.ModifyOptions", false));
    }
}
