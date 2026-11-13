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
                if (visited instanceof MethodInvocation invocation && TEXT_SCORE.matches(invocation)) {
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
        if (TypeUtils.isOfClassType(keyword.getType(), KEYWORD)) {
            String name = keyword instanceof FieldAccess access
                    ? access.getSimpleName()
                    : keyword instanceof Identifier identifier ? identifier.getSimpleName() : null;
            return "textScore".equals(name) ? name : null;
        }
        return null;
    }
}
