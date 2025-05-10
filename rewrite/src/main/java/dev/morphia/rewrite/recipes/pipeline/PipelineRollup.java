package dev.morphia.rewrite.recipes.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.morphia.aggregation.stages.Count;
import dev.morphia.aggregation.stages.Limit;
import dev.morphia.aggregation.stages.Match;
import dev.morphia.aggregation.stages.Skip;
import dev.morphia.aggregation.stages.SortByCount;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.aggregation.stages.UnionWith;
import dev.morphia.query.filters.Filter;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JavaType.FullyQualified;
import org.openrewrite.java.tree.JavaType.Method;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.java.tree.Space;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaCore;
import static java.util.Collections.emptyList;

public class PipelineRollup extends Recipe {
    static final String AGGREGATION = "dev.morphia.aggregation.Aggregation";

    static final List<MethodMatcher> matchers = List.of(
            new MethodMatcher(AGGREGATION + " addFields(dev.morphia.aggregation.stages.AddFields)"),
            new MethodMatcher(AGGREGATION + " autoBucket(dev.morphia.aggregation.stages.AutoBucket)"),
            new MethodMatcher(AGGREGATION + " bucket(dev.morphia.aggregation.stages.Bucket)"),
            new MethodMatcher(AGGREGATION + " changeStream(..)"),
            //            new MethodMatcher(AGGREGATION + " changeStream(dev.morphia.aggregation.stages.ChangeStream)"),
            new MethodMatcher(AGGREGATION + " collStats(dev.morphia.aggregation.stages.CollectionStats)"),
            new MethodMatcher(AGGREGATION + " count(..)"),
            //            new MethodMatcher(AGGREGATION + " count(java.lang.String)"),
            new MethodMatcher(AGGREGATION + " currentOp(dev.morphia.aggregation.stages.CountOp)"),
            new MethodMatcher(AGGREGATION + " densify(dev.morphia.aggregation.stages.Densify)"),
            new MethodMatcher(AGGREGATION + " documents(dev.morphia.aggregation.expressions.impls.DocumentExpression)"),
            new MethodMatcher(AGGREGATION + " facet(dev.morphia.aggregation.stages.Facet)"),
            new MethodMatcher(AGGREGATION + " fill(dev.morphia.aggregation.stages.Fill)"),
            new MethodMatcher(AGGREGATION + " geoNear(dev.morphia.aggregation.stages.GeoNear)"),
            new MethodMatcher(AGGREGATION + " graphLookup(dev.morphia.aggregation.stages.GraphLookup)"),
            new MethodMatcher(AGGREGATION + " group(dev.morphia.aggregation.stages.Group)"),
            new MethodMatcher(AGGREGATION + " indexStats(dev.morphia.aggregation.stages.IndexStats)"),
            new MethodMatcher(AGGREGATION + " limit(dev.morphia.aggregation.stages.Limit)"),
            new MethodMatcher(AGGREGATION + " lookup(dev.morphia.aggregation.stages.Lookup)"),
            new MethodMatcher(AGGREGATION + " match(dev.morphia.aggregation.stages.Match)"),
            new MethodMatcher(AGGREGATION + " planCacheStats()"),
            new MethodMatcher(AGGREGATION + " project(dev.morphia.aggregation.stages.Projection)"),
            new MethodMatcher(AGGREGATION + " redact(dev.morphia.aggregation.stages.Redact)"),
            new MethodMatcher(AGGREGATION + " replaceRoot(dev.morphia.aggregation.stages.ReplaceRoot)"),
            new MethodMatcher(AGGREGATION + " replaceWith(dev.morphia.aggregation.stages.ReplaceWith)"),
            new MethodMatcher(AGGREGATION + " sample(dev.morphia.aggregation.stages.Sample)"),
            //            new MethodMatcher(AGGREGATION + " set(..)"),
            new MethodMatcher(AGGREGATION + " set(dev.morphia.aggregation.stages.Set)"),
            new MethodMatcher(AGGREGATION + " setWindowFields(dev.morphia.aggregation.stages.SetWindowFields)"),
            new MethodMatcher(AGGREGATION + " skip(dev.morphia.aggregation.stages.Skip)"),
            new MethodMatcher(AGGREGATION + " sort(dev.morphia.aggregation.stages.Sort)"),
            new MethodMatcher(AGGREGATION + " sortByCount(dev.morphia.aggregation.stages.SortByCount)"),
            //            new MethodMatcher(AGGREGATION + " sortByCount(dev.morphia.aggregation.expressions.impls.Expression)"),
            new MethodMatcher(AGGREGATION + " unionWith(dev.morphia.aggregation.stages.UnionWith)"),
            //            new MethodMatcher(AGGREGATION + " unionWith(String,Stage...)"),
            //            new MethodMatcher(AGGREGATION + " unionWith(String,Stage,Stage...)"),
            new MethodMatcher(AGGREGATION + " unset(dev.morphia.aggregation.stages.Unset)"),
            new MethodMatcher(AGGREGATION + " unwind(dev.morphia.aggregation.stages.Unwind)"));

    private static final MethodMatcher MEGA_MATCHER = new MethodMatcher(
            AGGREGATION + " addFields(dev.morphia.aggregation.stages.AddFields)") {
        @Override
        public boolean matches(@Nullable MethodCall methodCall) {
            return matchers.stream().anyMatch(matcher -> matcher.matches(methodCall));
        }
    };

    private static final JavaTemplate COUNT = template(Count.class, "count");
    private static final JavaTemplate LIMIT = template(Limit.class, "limit");
    private static final JavaTemplate MATCH = template(Match.class, "match", Filter.class);
    private static final JavaTemplate SKIP = template(Skip.class, "skip");
    private static final JavaTemplate SORT_BY_COUNT = template(SortByCount.class, "sortByCount");
    private static final JavaTemplate UNION_WITH = template(UnionWith.class, "unionWith");

    private static @NotNull JavaTemplate template(Class<?> type, String method, Class<?>... imports) {
        return (JavaTemplate.builder("%s.%s()".formatted(type.getName(), method)))
                .javaParser(JavaParser.fromJavaVersion()
                        .classpath(List.of(findMorphiaCore().toPath())))
                .imports(type.getName())
                .imports(Arrays.stream(imports).map(Class::getName).toList().toArray(new String[0]))
                .build();
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Aggregation pipeline rewrite";
    }

    @Override
    public @NotNull String getDescription() {
        return "Rewrites an aggregation from using stage-named methods to using pipeline(Stage...).";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {

        return new JavaIsoVisitor<>() {
            @Override
            public @NotNull MethodInvocation visitMethodInvocation(@NotNull MethodInvocation methodInvocation,
                    @NotNull ExecutionContext context) {
                if (MEGA_MATCHER.matches(methodInvocation)) {
                    System.out.println("PipelineRollup.visitMethodInvocation: methodInvocation = " + methodInvocation);
                    Expression updated = methodInvocation;
                    MethodInvocation invocation = (MethodInvocation) updated;
                    List<Expression> arguments = new ArrayList<>();
                    while (MEGA_MATCHER.matches(updated)) {
                        invocation = (MethodInvocation) updated;
                        arguments.addAll(0, invocation.getArguments());
                        updated = invocation.getSelect();
                    }
                    Space after = methodInvocation.getPadding().getSelect().getAfter();
                    Space space = Space.build(getIndent(after), emptyList());
                    arguments = arguments.stream()
                            .map(a -> (Expression) a.withPrefix(space))
                            .toList();

                    invocation = invocation.withName(methodInvocation.getName().withSimpleName("pipeline"))
                            .withArguments(arguments);

                    MethodInvocation newInvocation = maybeAutoFormat(methodInvocation,
                            invocation.withPrefix(Space.build("\n", emptyList())), context);
                    System.out.println("newInvocation = " + newInvocation);
                    return newInvocation;
                } else {
                    return super.visitMethodInvocation(methodInvocation, context);
                }
            }

            private void collect(Class<? extends Stage> type,
                    String method,
                    JavaTemplate template,
                    MethodInvocation invocation,
                    List<Expression> args) {
                maybeAddImport(type.getName(), method, false);
                MethodInvocation applied = template.apply(new Cursor(getCursor(), invocation),
                        invocation.getCoordinates().replaceMethod());
                Method methodType = applied.getMethodType();
                FullyQualified declaringType = methodType.getDeclaringType()
                        .withFullyQualifiedName(type.getName());

                Method method1 = methodType.withDeclaringType(declaringType);
                Method type1 = method1
                        .withParameterNames(emptyList())
                        .withParameterTypes(emptyList());
                MethodInvocation methodInvocation = applied.withMethodType(type1);
                applied = methodInvocation
                        .withTypeParameters(JContainer.empty());
                args.add(0, applied.withArguments(invocation.getArguments()).withSelect(null));
            }

        };
    }

    public static String getIndent(Space after) {
        var whitespace = after.getWhitespace();
        if (!whitespace.startsWith("\n")) {
            whitespace = "\n" + whitespace;
        }
        return whitespace + "    ";
    }

}
