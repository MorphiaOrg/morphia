package dev.morphia.rewrite.recipes.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

/**
 * An OpenRewrite recipe that migrates the {@code @Embedded} annotation, removed in 3.0, to {@code @Entity}. A type with only
 * {@code @Embedded} gets {@code @Entity} in its place, keeping its discriminator settings. A type with both keeps its
 * {@code @Entity}, which takes on any discriminator settings it doesn't already set; settings that differ are left as
 * {@code @Entity} has them and the annotation is marked so they can be reviewed. {@code @Embedded} anywhere else is removed.
 */
public class EmbeddedToEntity extends Recipe {
    /** Creates a new instance. */
    public EmbeddedToEntity() {
    }

    private static final String EMBEDDED = "dev.morphia.annotations.Embedded";

    private static final String ENTITY = "dev.morphia.annotations.Entity";

    /**
     * The {@code @Embedded} attributes that {@code @Entity} also has with the same meaning and default. {@code value} isn't one: it
     * was unused on {@code @Embedded} but names the collection on {@code @Entity}.
     */
    private static final Set<String> CARRIED = Set.of("useDiscriminator", "discriminatorKey", "discriminator");

    @Override
    public @DisplayName String getDisplayName() {
        return "Migrate @Embedded to @Entity";
    }

    @Override
    public @Description String getDescription() {
        return "Replaces the @Embedded annotation, removed in 3.0, with @Entity, carrying over its discriminator settings.";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration result = super.visitClassDeclaration(classDecl, ctx);
                J.Annotation embedded = find(result.getAllAnnotations(), EMBEDDED);
                if (embedded != null) {
                    J.Annotation entity = find(result.getAllAnnotations(), ENTITY);
                    J.Annotation replacement = entity == null ? toEntity(embedded) : merge(entity, embedded);
                    result = replace(result, entity == null ? embedded : entity, replacement);
                    maybeAddImport(ENTITY);
                }
                // whatever is left, on this type when it also has @Entity or on its members, is simply dropped
                result = (J.ClassDeclaration) new RemoveAnnotationVisitor(new AnnotationMatcher("@" + EMBEDDED))
                        .visitNonNull(result, ctx, getCursor().getParentOrThrow());
                maybeRemoveImport(EMBEDDED);
                return result;
            }

            /**
             * Turns {@code @Embedded} into {@code @Entity}, keeping the attributes {@code @Entity} shares.
             */
            private J.Annotation toEntity(J.Annotation embedded) {
                List<Expression> kept = carried(embedded).values().stream().toList();
                return embedded.withAnnotationType(entityType(embedded.getAnnotationType().getPrefix()))
                        .withArguments(kept.isEmpty() ? null : reformat(kept));
            }

            /**
             * Adds the attributes of {@code @Embedded} that {@code @Entity} doesn't set to {@code @Entity}, marking it when they
             * disagree on one it does set.
             */
            private J.Annotation merge(J.Annotation entity, J.Annotation embedded) {
                Map<String, Expression> existing = attributes(entity);
                List<Expression> arguments = new ArrayList<>(existing.values());
                List<String> conflicts = new ArrayList<>();
                carried(embedded).forEach((name, argument) -> {
                    Expression current = existing.get(name);
                    if (current == null) {
                        arguments.add(argument.withId(Tree.randomId()));
                    } else if (!value(current).equals(value(argument))) {
                        conflicts.add(name);
                    }
                });
                if (arguments.size() > existing.size() && !(arguments.get(0) instanceof J.Assignment)) {
                    // a lone unnamed value has to be named once other attributes follow it
                    arguments.set(0, named("value", arguments.get(0)));
                }
                J.Annotation result = arguments.size() == existing.size() ? entity : entity.withArguments(reformat(arguments));
                return conflicts.isEmpty() ? result
                        : SearchResult.found(result, ("@Embedded also set %s to a different value, which was dropped in favor of "
                                + "@Entity's").formatted(String.join(", ", conflicts)));
            }

            /**
             * Builds a reference to {@code @Entity}, fully qualified when another type named {@code Entity} is imported (or
             * aliased to that name) or declared in the same file, as a type or a type parameter.
             */
            private TypeTree entityType(Space prefix) {
                JavaSourceFile source = getCursor().firstEnclosing(JavaSourceFile.class);
                if (source != null && (importsOtherEntity(source) || declaresEntity(source))) {
                    return TypeTree.<J.FieldAccess> build(ENTITY).withPrefix(prefix);
                }
                return new J.Identifier(Tree.randomId(), prefix, Markers.EMPTY, List.of(), "Entity",
                        JavaType.ShallowClass.build(ENTITY), null);
            }

            private String value(Expression argument) {
                Expression value = argument instanceof J.Assignment assignment ? assignment.getAssignment() : argument;
                return value.printTrimmed(getCursor());
            }
        };
    }

    private static boolean importsOtherEntity(JavaSourceFile source) {
        return source.getImports().stream()
                .anyMatch(i -> i.getAlias() != null ? "Entity".equals(i.getAlias().getSimpleName())
                        : "Entity".equals(i.getQualid().getSimpleName()) && (i.isStatic() || !ENTITY.equals(i.getTypeName())));
    }

    private static boolean declaresEntity(JavaSourceFile source) {
        AtomicBoolean found = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, AtomicBoolean found) {
                if ("Entity".equals(classDecl.getSimpleName())) {
                    found.set(true);
                }
                return super.visitClassDeclaration(classDecl, found);
            }

            @Override
            public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, AtomicBoolean found) {
                if (typeParam.getName() instanceof J.Identifier name && "Entity".equals(name.getSimpleName())) {
                    found.set(true);
                }
                return super.visitTypeParameter(typeParam, found);
            }
        }.visit(source, found);
        return found.get();
    }

    /**
     * Swaps an annotation of the type wherever it appears: before the modifiers, among them, or before the class keyword.
     */
    private static J.ClassDeclaration replace(J.ClassDeclaration classDecl, J.Annotation target, J.Annotation replacement) {
        UnaryOperator<List<J.Annotation>> swap = annotations -> ListUtils.map(annotations,
                annotation -> annotation == target ? replacement : annotation);
        J.ClassDeclaration result = classDecl.withLeadingAnnotations(swap.apply(classDecl.getLeadingAnnotations()))
                .withModifiers(ListUtils.map(classDecl.getModifiers(),
                        modifier -> modifier.withAnnotations(swap.apply(modifier.getAnnotations()))));
        J.ClassDeclaration.Kind kind = result.getPadding().getKind();
        return result.getPadding().withKind(kind.withAnnotations(swap.apply(kind.getAnnotations())));
    }

    private static J.@Nullable Annotation find(List<J.Annotation> annotations, String type) {
        return annotations.stream()
                .filter(annotation -> TypeUtils.isOfClassType(annotation.getType(), type))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return the arguments of the annotation keyed by attribute name, with a lone unnamed argument keyed as {@code value}
     */
    private static Map<String, Expression> attributes(J.Annotation annotation) {
        Map<String, Expression> attributes = new LinkedHashMap<>();
        if (annotation.getArguments() != null) {
            for (Expression argument : annotation.getArguments()) {
                if (argument instanceof J.Empty) {
                    continue;
                }
                String name = argument instanceof J.Assignment assignment
                        && assignment.getVariable() instanceof J.Identifier identifier ? identifier.getSimpleName() : "value";
                attributes.put(name, argument);
            }
        }
        return attributes;
    }

    private static J.Assignment named(String name, Expression value) {
        J.Identifier variable = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, List.of(), name, value.getType(), null);
        return new J.Assignment(Tree.randomId(), value.getPrefix(), Markers.EMPTY, variable,
                JLeftPadded.<Expression> build(value.withPrefix(Space.SINGLE_SPACE)).withBefore(Space.SINGLE_SPACE), value.getType());
    }

    private static Map<String, Expression> carried(J.Annotation embedded) {
        Map<String, Expression> carried = attributes(embedded);
        carried.keySet().retainAll(CARRIED);
        return carried;
    }

    /**
     * Spaces the arguments as {@code a, b, c}, since dropping or adding one leaves the spacing of the old list behind.
     */
    private static List<Expression> reformat(List<Expression> arguments) {
        List<Expression> formatted = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            Expression argument = arguments.get(i);
            formatted.add(argument.withPrefix(i == 0 ? Space.EMPTY : Space.SINGLE_SPACE));
        }
        return formatted;
    }
}
