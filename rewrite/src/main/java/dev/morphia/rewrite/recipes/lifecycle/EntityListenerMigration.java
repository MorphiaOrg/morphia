package dev.morphia.rewrite.recipes.lifecycle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

/**
 * Migrates classes that implement {@code EntityListener}, which is now only a marker, to the per-event listener interfaces:
 * {@code PrePersistListener}, {@code PostPersistListener}, {@code PreLoadListener} and {@code PostLoadListener}.
 * <p>
 * Each overridden {@code prePersist}, {@code postPersist}, {@code preLoad} or {@code postLoad} method moves to its event's
 * interface, with a {@code Datastore} for its last parameter. Only {@code preLoad} keeps the {@code Document}: a
 * {@code prePersist}, {@code postPersist} or {@code postLoad} method that reads its {@code Document}, or one that uses the
 * deprecated {@code Mapper} parameter, can't be migrated automatically and is marked for a person to look at.
 * {@code hasAnnotation} overrides are removed, as Morphia now checks for the interfaces itself.
 */
public class EntityListenerMigration extends Recipe {
    private static final String ENTITY_LISTENER = "dev.morphia.EntityListener";
    private static final String DATASTORE = "dev.morphia.Datastore";
    private static final String MAPPER = "dev.morphia.mapping.Mapper";
    private static final String DOCUMENT = "org.bson.Document";
    private static final Map<String, Event> EVENTS = Map.of(
            "prePersist", new Event("PrePersistListener", "PrePersist", false),
            "postPersist", new Event("PostPersistListener", "PostPersist", false),
            "preLoad", new Event("PreLoadListener", "PreLoad", true),
            "postLoad", new Event("PostLoadListener", "PostLoad", false));

    /** Creates a new instance. */
    public EntityListenerMigration() {
    }

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Migrate EntityListener implementations to the per-event listener interfaces";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "EntityListener is now a marker interface. Implementations move to PrePersistListener, PostPersistListener, "
                + "PreLoadListener and PostLoadListener, and their hasAnnotation overrides are removed.";
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                if (cd.getImplements() == null) {
                    return cd;
                }
                TypeTree entityListener = cd.getImplements().stream()
                        .filter(type -> TypeUtils.isOfClassType(type.getType(), ENTITY_LISTENER))
                        .findFirst()
                        .orElse(null);
                if (entityListener == null) {
                    return cd;
                }

                // the interfaces the migrated methods now belong to, in the order the methods are declared
                Map<String, Event> migrated = new LinkedHashMap<>();
                List<Statement> statements = new ArrayList<>();
                // a removed member's whitespace goes to the next member, so removing the first leaves no blank line
                Space removed = null;
                for (Statement statement : cd.getBody().getStatements()) {
                    if (removed != null) {
                        statement = statement.withPrefix(removed);
                        removed = null;
                    }
                    if (statement instanceof J.MethodDeclaration method) {
                        if (isHasAnnotation(method)) {
                            if (statements.isEmpty()) {
                                removed = method.getPrefix();
                            }
                            continue;
                        }
                        Event event = EVENTS.get(method.getSimpleName());
                        if (event != null && overridesEntityListener(method)) {
                            J.MethodDeclaration updated = migrate(method, event, ctx);
                            if (!updated.getMarkers().findFirst(SearchResult.class).isPresent()) {
                                migrated.put(event.listener(), event);
                            }
                            statements.add(updated);
                            continue;
                        }
                    }
                    statements.add(statement);
                }
                cd = cd.withBody(cd.getBody().withStatements(statements));
                if (!migrated.isEmpty()) {
                    cd = cd.withImplements(replace(cd.getImplements(), entityListener, migrated.values()));
                    migrated.values().forEach(event -> maybeAddImport("dev.morphia." + event.listener(), false));
                    maybeAddImport(DATASTORE, false);
                }
                maybeRemoveImport(ENTITY_LISTENER);
                maybeRemoveImport(DOCUMENT);
                maybeRemoveImport(MAPPER);
                maybeRemoveImport("java.lang.annotation.Annotation");
                EVENTS.values().forEach(event -> maybeRemoveImport("dev.morphia.annotations." + event.annotation()));
                return cd;
            }

            private J.MethodDeclaration migrate(J.MethodDeclaration method, Event event, ExecutionContext ctx) {
                List<Statement> parameters = method.getParameters();
                J.VariableDeclarations document = (J.VariableDeclarations) parameters.get(1);
                J.VariableDeclarations datastore = (J.VariableDeclarations) parameters.get(2);
                if (!event.keepsDocument() && uses(method, document)) {
                    return SearchResult.found(method, "%s no longer takes the Document: move this logic to a PreLoadListener, or to an "
                            .formatted(event.listener()) + "annotated method on the entity or on an @EntityListeners class");
                }
                if (TypeUtils.isOfClassType(datastore.getType(), MAPPER)) {
                    if (uses(method, datastore)) {
                        return SearchResult.found(method, "the Mapper parameter is gone: use datastore.getMapper()");
                    }
                }
                J.VariableDeclarations retyped = retype(datastore);
                List<Statement> updated = new ArrayList<>();
                updated.add(parameters.get(0));
                if (event.keepsDocument()) {
                    updated.add(document);
                    updated.add(retyped);
                } else {
                    updated.add(retyped.withPrefix(document.getPrefix()));
                }
                J.MethodDeclaration result = method.withParameters(updated);
                if (method.getMethodType() != null) {
                    result = result.withMethodType(method.getMethodType().withParameterTypes(updated.stream()
                            .map(parameter -> ((J.VariableDeclarations) parameter).getType())
                            .toList()));
                }
                // an annotated method would also be called through the interface when the class is an @EntityListeners class
                return (J.MethodDeclaration) new RemoveAnnotationVisitor(
                        new AnnotationMatcher("@dev.morphia.annotations." + event.annotation()))
                        .visitNonNull(result, ctx, getCursor());
            }
        };
    }

    private static boolean isHasAnnotation(J.MethodDeclaration method) {
        return method.getSimpleName().equals("hasAnnotation") && method.getParameters().size() == 1
                && method.getParameters().get(0) instanceof J.VariableDeclarations;
    }

    private static boolean overridesEntityListener(J.MethodDeclaration method) {
        List<Statement> parameters = method.getParameters();
        return parameters.size() == 3
                && parameters.get(1) instanceof J.VariableDeclarations document
                && TypeUtils.isOfClassType(document.getType(), DOCUMENT)
                && parameters.get(2) instanceof J.VariableDeclarations;
    }

    private static boolean uses(J.MethodDeclaration method, J.VariableDeclarations parameter) {
        if (method.getBody() == null) {
            return false;
        }
        String name = parameter.getVariables().get(0).getSimpleName();
        AtomicBoolean found = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean used) {
                if (identifier.getSimpleName().equals(name)) {
                    used.set(true);
                }
                return identifier;
            }
        }.visit(method.getBody(), found);
        return found.get();
    }

    /**
     * Retypes the parameter as a {@code Datastore}: its type expression and its variable's type, so the old type's import
     * can go.
     */
    private static J.VariableDeclarations retype(J.VariableDeclarations parameter) {
        JavaType.ShallowClass type = JavaType.ShallowClass.build(DATASTORE);
        TypeTree original = parameter.getTypeExpression();
        return parameter
                .withTypeExpression(new J.Identifier(Tree.randomId(), original.getPrefix(), Markers.EMPTY, List.of(), "Datastore",
                        type, null))
                .withVariables(parameter.getVariables().stream()
                        .map(variable -> {
                            J.Identifier name = variable.getName().withType(type);
                            if (name.getFieldType() != null) {
                                name = name.withFieldType(name.getFieldType().withType(type));
                            }
                            return variable.withName(name);
                        })
                        .toList());
    }

    private static List<TypeTree> replace(List<TypeTree> implemented, TypeTree entityListener, Iterable<Event> events) {
        List<TypeTree> types = new ArrayList<>();
        for (TypeTree type : implemented) {
            if (type != entityListener) {
                types.add(type);
                continue;
            }
            boolean first = true;
            for (Event event : events) {
                TypeTree listener = listenerType(entityListener, event);
                types.add(first ? listener : listener.withPrefix(Space.format(" ")));
                first = false;
            }
        }
        return types;
    }

    private static TypeTree listenerType(TypeTree entityListener, Event event) {
        JavaType.ShallowClass type = JavaType.ShallowClass.build("dev.morphia." + event.listener());
        J.Identifier name = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, List.of(), event.listener(), type, null);
        if (entityListener instanceof J.ParameterizedType parameterized) {
            return parameterized.withId(Tree.randomId())
                    .withClazz(name.withPrefix(parameterized.getClazz().getPrefix()))
                    .withType(type);
        }
        return name.withPrefix(entityListener.getPrefix());
    }

    private record Event(String listener, String annotation, boolean keepsDocument) {
    }
}
