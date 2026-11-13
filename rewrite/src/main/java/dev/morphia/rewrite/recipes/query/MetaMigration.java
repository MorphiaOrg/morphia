package dev.morphia.rewrite.recipes.query;

import java.util.List;
import java.util.Set;

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
import org.openrewrite.java.tree.J.FieldAccess;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.Literal;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.J.NewClass;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;

/**
 * An OpenRewrite recipe that migrates the {@code Meta} constructors and the no-argument {@code Meta.textScore()}, all removed in 3.0,
 * to the {@code Meta} factory methods.
 */
public class MetaMigration extends Recipe {
    /** Creates a new instance. */
    public MetaMigration() {
    }

    private static final String META = "dev.morphia.query.Meta";

    private static final String KEYWORD = META + "$MetaDataKeyword";

    private static final MethodMatcher CONSTRUCTOR = new MethodMatcher(META + " <init>(..)");

    private static final MethodMatcher TEXT_SCORE = new MethodMatcher(META + " textScore()");

    private static final Set<String> FACTORIES = Set.of("indexKey", "searchHighlights", "searchScore", "textScore");

    @Override
    public @DisplayName String getDisplayName() {
        return "Meta refactorings";
    }

    @Override
    public @Description String getDescription() {
        return "Migrates usages of the Meta constructors and the no-argument textScore() to the Meta factory methods.";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public J visitNewClass(NewClass newClass, ExecutionContext executionContext) {
                J visited = super.visitNewClass(newClass, executionContext);
                if (!(visited instanceof NewClass ctor) || !CONSTRUCTOR.matches(ctor)) {
                    return visited;
                }
                List<Expression> arguments = ctor.getArguments();
                String factory = factory(arguments.get(0));
                if (factory == null) {
                    // a computed keyword can't be mapped to a factory method so leave it for the user to fix
                    return visited;
                }
                if (arguments.size() == 1) {
                    // new Meta(MetaDataKeyword) used the keyword as the field name, too
                    return replace(ctor, "Meta.textScore(\"textScore\")");
                }
                return replace(ctor, "Meta.%s(#{any(java.lang.String)})".formatted(factory), arguments.get(1));
            }

            @Override
            public J visitMethodInvocation(MethodInvocation method, ExecutionContext executionContext) {
                J visited = super.visitMethodInvocation(method, executionContext);
                // a static call made through an instance expression is left alone so the expression is still evaluated
                if (visited instanceof MethodInvocation invocation && TEXT_SCORE.matches(invocation)
                        && (invocation.getSelect() == null || isTypeReference(invocation.getSelect()))) {
                    return replace(invocation, "Meta.textScore(\"textScore\")");
                }
                return visited;
            }

            private J replace(Expression tree, String code, Object... arguments) {
                maybeAddImport(META);
                maybeRemoveImport(KEYWORD);
                maybeRemoveImport(META + ".MetaDataKeyword");
                return JavaTemplate.builder(code)
                        .javaParser(JavaParser.fromJavaVersion()
                                .classpath(findMorphiaDependencies()))
                        .imports(META)
                        .build()
                        .apply(updateCursor(tree), tree.getCoordinates().replace(), arguments);
            }
        };
    }

    /**
     * Finds the factory method to use for the keyword argument of a {@code Meta} constructor.
     *
     * @param keyword the keyword argument
     * @return the factory method name or null if the keyword can't be determined statically
     */
    private static String factory(Expression keyword) {
        if (keyword instanceof Literal literal && literal.getValue() instanceof String value) {
            return FACTORIES.contains(value) ? value : null;
        }
        // a constant reached through an instance expression is left alone so the expression is still evaluated
        Identifier name = keyword instanceof FieldAccess access ? (isTypeReference(access.getTarget()) ? access.getName() : null)
                : keyword instanceof Identifier identifier ? identifier : null;
        // only the enum constant itself, not some other variable or field of the enum type that happens to share its name
        if (name != null && name.getFieldType() != null && TypeUtils.isOfClassType(name.getFieldType().getOwner(), KEYWORD)
                && "textScore".equals(name.getSimpleName())) {
            return "textScore";
        }
        return null;
    }

    /**
     * @param expression the qualifier of a static member access
     * @return true if the qualifier names a type rather than computing a value
     */
    private static boolean isTypeReference(Expression expression) {
        if (expression instanceof Identifier identifier) {
            return identifier.getFieldType() == null && identifier.getType() instanceof JavaType.FullyQualified;
        }
        if (expression instanceof FieldAccess access) {
            return access.getName().getFieldType() == null && access.getType() instanceof JavaType.FullyQualified;
        }
        return false;
    }
}
