package dev.morphia.rewrite.recipes.datastore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Method;
import org.openrewrite.java.tree.TypeUtils;

import jakarta.annotation.Nullable;

import static dev.morphia.rewrite.recipes.RewriteUtils.datastoreMethod;
import static dev.morphia.rewrite.recipes.RewriteUtils.isKotlin;
import static dev.morphia.rewrite.recipes.RewriteUtils.isOfType;
import static dev.morphia.rewrite.recipes.RewriteUtils.isSideEffectFree;
import static dev.morphia.rewrite.recipes.RewriteUtils.template;

/**
 * An OpenRewrite recipe that replaces the removed {@code Datastore.save(T, InsertOptions)}, {@code save(Iterable)}, and
 * {@code save(Iterable, InsertOptions)} with {@code save(T, InsertOneOptions)}, {@code save(List)}, and
 * {@code save(List, InsertManyOptions)}.
 * <p>
 * Only the cases that convert cleanly are rewritten: the {@code InsertOptions} must be created inline (e.g.,
 * {@code new InsertOptions().writeConcern(wc)}) so that it can be rebuilt as the new options type, and, in Java, an
 * {@code Iterable} must be a {@code Collection} so that it can be copied into an {@code ArrayList}, as the
 * removed overloads did. Because the copy now happens before the options are evaluated, and a dropped
 * {@code ordered()} is no longer evaluated at all, those option arguments must also be free of side effects. Anything
 * else is left unchanged for a manual migration.
 */
public class DatastoreSaveInsertOptions extends Recipe {
    /** Creates a new instance. */
    public DatastoreSaveInsertOptions() {
    }

    private static final String INSERT_OPTIONS = "dev.morphia.InsertOptions";
    private static final String INSERT_ONE_OPTIONS = "dev.morphia.InsertOneOptions";
    private static final String INSERT_MANY_OPTIONS = "dev.morphia.InsertManyOptions";

    private static final Set<String> INSERT_OPTIONS_METHODS = Set.of("writeConcern", "bypassDocumentValidation", "ordered");

    private static final MethodMatcher SAVE = datastoreMethod("save(..)");

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Replace Datastore.save() overloads taking InsertOptions or Iterable";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "Converts InsertOptions to InsertOneOptions or InsertManyOptions and copies an Iterable to a List for the "
                + "removed Datastore.save() overloads.";
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public MethodInvocation visitMethodInvocation(MethodInvocation method, ExecutionContext executionContext) {
                MethodInvocation invocation = super.visitMethodInvocation(method, executionContext);
                Method type = invocation.getMethodType();
                if (!SAVE.matches(invocation) || type == null || type.getParameterTypes().isEmpty()) {
                    return invocation;
                }
                List<JavaType> parameterTypes = type.getParameterTypes();
                boolean iterable = isOfType(parameterTypes.get(0), "java.lang.Iterable",
                        "kotlin.collections.Iterable", "kotlin.collections.MutableIterable");
                boolean insertOptions = parameterTypes.size() == 2 && isOfType(parameterTypes.get(1), INSERT_OPTIONS);
                if (!iterable && !insertOptions) {
                    return invocation;
                }

                List<Object> parameters = new ArrayList<>();
                List<String> imports = new ArrayList<>();
                StringBuilder java = new StringBuilder();
                StringBuilder kotlin = new StringBuilder();

                Expression entity = invocation.getArguments().get(0);
                parameters.add(entity);
                // the removed Iterable overloads always saved, and returned, a new mutable copy, even of a List
                if (!iterable) {
                    java.append("#{any()}");
                    kotlin.append("#{any()}");
                } else if (isKotlin(getCursor())) {
                    kotlin.append("#{any()}.toMutableList()");
                } else if (TypeUtils.isAssignableTo("java.util.Collection", entity.getType())) {
                    java.append("new ArrayList<>(#{any(java.util.Collection)})");
                    imports.add("java.util.ArrayList");
                } else {
                    return invocation;
                }

                JavaType optionsType = null;
                if (insertOptions) {
                    String newOptions = iterable ? INSERT_MANY_OPTIONS : INSERT_ONE_OPTIONS;
                    List<MethodInvocation> calls = optionsCalls(invocation.getArguments().get(1));
                    if (calls == null) {
                        return invocation;
                    }
                    String simpleName = newOptions.substring(newOptions.lastIndexOf('.') + 1);
                    java.append(", new ").append(simpleName).append("()");
                    kotlin.append(", ").append(simpleName).append("()");
                    for (MethodInvocation call : calls) {
                        // ordered() has no single entity equivalent and was ignored by save(T, InsertOptions)
                        Expression argument = call.getArguments().get(0);
                        // the copy of an Iterable now happens before the options are evaluated, and a dropped
                        // ordered() is no longer evaluated at all, so both need arguments without side effects
                        boolean dropped = !iterable && call.getSimpleName().equals("ordered");
                        if ((iterable || dropped) && !isSideEffectFree(argument)) {
                            return invocation;
                        }
                        if (dropped) {
                            continue;
                        }
                        java.append('.').append(call.getSimpleName()).append("(#{any()})");
                        kotlin.append('.').append(call.getSimpleName()).append("(#{any()})");
                        parameters.add(argument);
                    }
                    imports.add(newOptions);
                    optionsType = JavaType.buildType(newOptions);
                }

                MethodInvocation updated = template(getCursor(), kotlin.toString(), java.toString(),
                        imports.toArray(new String[0]))
                        .apply(new Cursor(getCursor().getParentOrThrow(), invocation),
                                invocation.getCoordinates().replaceArguments(),
                                parameters.toArray());
                imports.forEach(name -> maybeAddImport(name, false));
                maybeRemoveImport(INSERT_OPTIONS);

                List<JavaType> newTypes = new ArrayList<>();
                newTypes.add(iterable ? JavaType.buildType("java.util.List") : parameterTypes.get(0));
                if (optionsType != null) {
                    newTypes.add(optionsType);
                }
                Method newType = type.withParameterTypes(newTypes);
                return invocation
                        .withArguments(updated.getArguments())
                        .withName(invocation.getName().withType(newType))
                        .withMethodType(newType);
            }
        };
    }

    /**
     * Collects the calls made on an inline {@code new InsertOptions()}, in order, as long as they are all InsertOptions
     * setters. Returns null for anything else, such as a variable, whose configuration can't be seen here.
     */
    private static @Nullable List<MethodInvocation> optionsCalls(Expression expression) {
        List<MethodInvocation> calls = new ArrayList<>();
        while (expression instanceof MethodInvocation call) {
            if (!INSERT_OPTIONS_METHODS.contains(call.getSimpleName()) || call.getArguments().size() != 1) {
                return null;
            }
            calls.add(0, call);
            expression = call.getSelect();
        }
        if (expression instanceof J.NewClass newClass && isOfType(newClass.getType(), INSERT_OPTIONS)
                && newClass.getArguments().stream().allMatch(argument -> argument instanceof J.Empty)) {
            return calls;
        }
        return null;
    }
}
