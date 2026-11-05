package dev.morphia.rewrite.recipes.lifecycle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

/**
 * Migrates implementations of {@code EntityListener}, which is now only a marker, to the per-event listener interfaces:
 * {@code PrePersistListener}, {@code PostPersistListener}, {@code PreLoadListener} and {@code PostLoadListener}.
 * <p>
 * A method is migrated only if it overrides one of {@code EntityListener}'s old methods, directly or through a superclass.
 * Each overriding {@code prePersist}, {@code postPersist}, {@code preLoad} or {@code postLoad} moves to its event's
 * interface, with a {@code Datastore} for its last parameter, and calls to it in the same source file are updated to match.
 * Only {@code preLoad} keeps the {@code Document}. {@code hasAnnotation} overrides are removed, as Morphia now checks for the
 * interfaces itself.
 * <p>
 * Some code can't be migrated automatically and is marked for a person to look at:
 * <ul>
 * <li>a {@code prePersist}, {@code postPersist} or {@code postLoad} that reads its {@code Document}, or a method that uses
 * the deprecated {@code Mapper} parameter;</li>
 * <li>an anonymous listener that handles more than one event, or that extends a class, since it can only implement one
 * interface;</li>
 * <li>a call to a listener method that this recipe didn't migrate in the same source file, or a call to
 * {@code hasAnnotation}.</li>
 * </ul>
 */
public class EntityListenerMigration extends Recipe {
    private static final String ENTITY_LISTENER = "dev.morphia.EntityListener";
    private static final String DATASTORE = "dev.morphia.Datastore";
    private static final String MAPPER = "dev.morphia.mapping.Mapper";
    private static final String DOCUMENT = "org.bson.Document";
    private static final String LISTENERS = "listeners";
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
        return new Migrator();
    }

    private static final class Migrator extends JavaIsoVisitor<ExecutionContext> {
        // the listener methods this source file declares and migrates, so calls to them can be updated
        private final Set<String> migrated = new HashSet<>();

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            migrated.clear();
            new JavaIsoVisitor<Set<String>>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Set<String> keys) {
                    Plan plan = plan(method);
                    if (plan != null && plan.event() != null && plan.problem() == null) {
                        keys.add(key(method.getMethodType()));
                    }
                    return super.visitMethodDeclaration(method, keys);
                }
            }.visit(cu, migrated);

            J.CompilationUnit result = super.visitCompilationUnit(cu, ctx);
            if (result != cu) {
                maybeRemoveImport(ENTITY_LISTENER);
                maybeRemoveImport(DOCUMENT);
                maybeRemoveImport(MAPPER);
                maybeRemoveImport("java.lang.annotation.Annotation");
                EVENTS.values().forEach(event -> maybeRemoveImport("dev.morphia.annotations." + event.annotation()));
            }
            return result;
        }

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
            J.Block result = super.visitBlock(block, ctx);
            // a removed first member's whitespace goes to the new first member, so no blank line opens the body
            List<Statement> before = block.getStatements();
            List<Statement> after = result.getStatements();
            if (!before.isEmpty() && !after.isEmpty() && !after.get(0).getId().equals(before.get(0).getId())
                    && after.stream().noneMatch(statement -> statement.getId().equals(before.get(0).getId()))) {
                List<Statement> statements = new ArrayList<>(after);
                statements.set(0, statements.get(0).withPrefix(before.get(0).getPrefix()));
                result = result.withStatements(statements);
            }
            return result;
        }

        @Override
        public J.@Nullable MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration result = super.visitMethodDeclaration(method, ctx);
            Plan plan = plan(method);
            if (plan == null) {
                return result;
            }
            if (plan.event() == null) {
                // a hasAnnotation override
                return null;
            }
            if (plan.problem() != null) {
                return SearchResult.found(result, plan.problem());
            }
            if (plan.needsInterface()) {
                Cursor owner = getCursor().dropParentUntil(tree -> tree instanceof J.ClassDeclaration || tree instanceof J.NewClass);
                Map<Event, TypeTree> listeners = owner.computeMessageIfAbsent(LISTENERS, key -> new LinkedHashMap<>());
                listeners.put(plan.event(), ((J.VariableDeclarations) method.getParameters().get(0)).getTypeExpression());
            }
            return migrate(result, plan.event(), ctx);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration result = super.visitClassDeclaration(classDecl, ctx);
            List<TypeTree> implemented = result.getImplements() != null ? result.getImplements() : List.of();
            Map<Event, TypeTree> listeners = missing(getCursor().pollMessage(LISTENERS), implemented);
            if (listeners.isEmpty()) {
                return result;
            }
            List<TypeTree> types = new ArrayList<>();
            boolean replaced = false;
            for (TypeTree type : implemented) {
                if (!replaced && TypeUtils.isOfClassType(type.getType(), ENTITY_LISTENER)) {
                    types.addAll(listenerTypes(listeners, type.getPrefix()));
                    replaced = true;
                } else {
                    types.add(type);
                }
            }
            if (!replaced) {
                // the class inherits EntityListener: it adds the interfaces of the events no superclass migrates
                types.addAll(listenerTypes(listeners, Space.SINGLE_SPACE));
            }
            addImports(listeners);
            JContainer<TypeTree> container = result.getPadding().getImplements();
            List<JRightPadded<TypeTree>> padded = types.stream().map(JRightPadded::build).toList();
            return result.getPadding().withImplements(container != null
                    ? container.getPadding().withElements(padded)
                    : JContainer.build(Space.SINGLE_SPACE, padded, Markers.EMPTY));
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass result = super.visitNewClass(newClass, ctx);
            Map<Event, TypeTree> listeners = missing(getCursor().pollMessage(LISTENERS),
                    result.getClazz() != null ? List.of(result.getClazz()) : List.of());
            if (listeners.isEmpty()) {
                return result;
            }
            if (listeners.size() > 1 || result.getClazz() == null
                    || !TypeUtils.isOfClassType(result.getClazz().getType(), ENTITY_LISTENER)) {
                return SearchResult.found(newClass, "an anonymous class can implement only one listener interface: "
                        + "move this listener to a named class that implements " + String.join(", ", listeners.keySet().stream()
                                .map(Event::listener)
                                .toList()));
            }
            addImports(listeners);
            return result.withClazz(listenerTypes(listeners, result.getClazz().getPrefix()).get(0));
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation result = super.visitMethodInvocation(method, ctx);
            JavaType.Method type = method.getMethodType();
            if (type == null) {
                return result;
            }
            boolean throughMarker = TypeUtils.isOfClassType(type.getDeclaringType(), ENTITY_LISTENER);
            JavaType.Method root = throughMarker ? type : root(type);
            if (root == null) {
                return result;
            }
            if (root.getName().equals("hasAnnotation")) {
                return SearchResult.found(result, "hasAnnotation is gone: check for a listener interface, such as "
                        + "instanceof PrePersistListener");
            }
            Event event = EVENTS.get(root.getName());
            if (event == null || method.getArguments().size() != 3) {
                return result;
            }
            if (throughMarker || !migrated.contains(key(type))) {
                return SearchResult.found(result, "call the migrated %s method instead".formatted(event.listener()));
            }
            List<Expression> arguments = method.getArguments();
            if (!TypeUtils.isAssignableTo(DATASTORE, arguments.get(2).getType())) {
                return SearchResult.found(result, "%s's method takes a Datastore".formatted(event.listener()));
            }
            if (event.keepsDocument()) {
                return result;
            }
            List<Expression> updated = new ArrayList<>(result.getArguments());
            updated.remove(1);
            return result.withArguments(updated);
        }

        private J.MethodDeclaration migrate(J.MethodDeclaration method, Event event, ExecutionContext ctx) {
            List<Statement> parameters = method.getParameters();
            J.VariableDeclarations document = (J.VariableDeclarations) parameters.get(1);
            J.VariableDeclarations retyped = retype((J.VariableDeclarations) parameters.get(2));
            if (event.keepsDocument() && retyped == parameters.get(2)) {
                // already a preLoad(entity, document, datastore)
                return method;
            }
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
            maybeAddImport(DATASTORE, false);
            // an annotated method would also be called through the interface when the class is an @EntityListeners class
            return (J.MethodDeclaration) new RemoveAnnotationVisitor(
                    new AnnotationMatcher("@dev.morphia.annotations." + event.annotation()))
                    .visitNonNull(result, ctx, getCursor().getParentOrThrow());
        }

        /**
         * @return the listeners whose interfaces aren't among the implemented types yet
         */
        private Map<Event, TypeTree> missing(@Nullable Map<Event, TypeTree> listeners, List<TypeTree> implemented) {
            Map<Event, TypeTree> missing = new LinkedHashMap<>();
            if (listeners != null) {
                listeners.forEach((event, entityType) -> {
                    if (implemented.stream()
                            .noneMatch(type -> TypeUtils.isOfClassType(type.getType(), "dev.morphia." + event.listener()))) {
                        missing.put(event, entityType);
                    }
                });
            }
            return missing;
        }

        private void addImports(Map<Event, TypeTree> listeners) {
            listeners.keySet().forEach(event -> maybeAddImport("dev.morphia." + event.listener(), false));
        }
    }

    /**
     * What to do with a method declaration: remove it (a {@code hasAnnotation} override, with no event), migrate it to an
     * event's interface, or mark it with a problem. Null if it doesn't override one of {@code EntityListener}'s methods.
     *
     * @param needsInterface true if the method overrides {@code EntityListener}'s own, so its class must implement the
     *                       event's interface; false if it overrides a superclass's, which implements it once migrated
     */
    private record Plan(@Nullable Event event, @Nullable String problem, boolean needsInterface) {
    }

    @Nullable
    private static Plan plan(J.MethodDeclaration method) {
        JavaType.Method type = method.getMethodType();
        JavaType.Method root = type != null ? root(type) : null;
        if (root == null) {
            return null;
        }
        if (root.getName().equals("hasAnnotation")) {
            return new Plan(null, null, false);
        }
        Event event = EVENTS.get(root.getName());
        if (event == null || method.getParameters().size() != 3) {
            return null;
        }
        boolean needsInterface = TypeUtils.findOverriddenMethod(type)
                .map(overridden -> TypeUtils.isOfClassType(overridden.getDeclaringType(), ENTITY_LISTENER))
                .orElse(false);
        J.VariableDeclarations document = (J.VariableDeclarations) method.getParameters().get(1);
        J.VariableDeclarations datastore = (J.VariableDeclarations) method.getParameters().get(2);
        String problem = null;
        if (!event.keepsDocument() && uses(method, document)) {
            problem = "%s no longer takes the Document: move this logic to a PreLoadListener, or to an annotated method on the "
                    .formatted(event.listener()) + "entity or on an @EntityListeners class";
        } else if (TypeUtils.isOfClassType(datastore.getType(), MAPPER) && uses(method, datastore)) {
            problem = "the Mapper parameter is gone: use datastore.getMapper()";
        }
        return new Plan(event, problem, needsInterface);
    }

    /**
     * @return the {@code EntityListener} method that the given method overrides, directly or through superclasses, or null
     */
    private static JavaType.@Nullable Method root(JavaType.Method method) {
        Optional<JavaType.Method> overridden = TypeUtils.findOverriddenMethod(method);
        while (overridden.isPresent()) {
            if (TypeUtils.isOfClassType(overridden.get().getDeclaringType(), ENTITY_LISTENER)) {
                return overridden.get();
            }
            overridden = TypeUtils.findOverriddenMethod(overridden.get());
        }
        return null;
    }

    private static String key(JavaType.@Nullable Method method) {
        return method == null ? "" : method.getDeclaringType().getFullyQualifiedName() + "#" + method.getName();
    }

    private static boolean uses(J.MethodDeclaration method, J.VariableDeclarations parameter) {
        if (method.getBody() == null) {
            return false;
        }
        String name = parameter.getVariables().get(0).getSimpleName();
        AtomicBoolean found = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation, AtomicBoolean used) {
                // the Document passed on to another listener method is dropped along with that call's
                JavaType.Method type = invocation.getMethodType();
                JavaType.Method root = type == null ? null
                        : TypeUtils.isOfClassType(type.getDeclaringType(), ENTITY_LISTENER) ? type : root(type);
                Event event = root != null ? EVENTS.get(root.getName()) : null;
                if (event != null && !event.keepsDocument() && invocation.getArguments().size() == 3) {
                    visit(invocation.getSelect(), used);
                    visit(invocation.getArguments().get(0), used);
                    visit(invocation.getArguments().get(2), used);
                    return invocation;
                }
                return super.visitMethodInvocation(invocation, used);
            }

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
        if (TypeUtils.isOfClassType(parameter.getType(), DATASTORE)) {
            return parameter;
        }
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

    /**
     * @return the event interfaces, each parameterized by the entity type its method takes
     */
    private static List<TypeTree> listenerTypes(Map<Event, TypeTree> listeners, Space prefix) {
        List<TypeTree> types = new ArrayList<>();
        for (Map.Entry<Event, TypeTree> entry : listeners.entrySet()) {
            JavaType.ShallowClass raw = JavaType.ShallowClass.build("dev.morphia." + entry.getKey().listener());
            J.Identifier name = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, List.of(), entry.getKey().listener(),
                    raw, null);
            TypeTree entityType = entry.getValue();
            JavaType argument = entityType.getType() != null ? entityType.getType() : JavaType.ShallowClass.build("java.lang.Object");
            types.add(new J.ParameterizedType(Tree.randomId(), types.isEmpty() ? prefix : Space.SINGLE_SPACE, Markers.EMPTY, name,
                    JContainer.build(Space.EMPTY, List.of(JRightPadded.build((Expression) entityType.withPrefix(Space.EMPTY))),
                            Markers.EMPTY),
                    new JavaType.Parameterized(null, raw, List.of(argument))));
        }
        return types;
    }

    private record Event(String listener, String annotation, boolean keepsDocument) {
    }
}
