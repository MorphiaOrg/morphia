package dev.morphia.rewrite.recipes.lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.ScanningRecipe;
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
 * Only {@code preLoad} keeps the {@code Document}, except that a {@code postPersist} or {@code postLoad} that uses it moves
 * to {@code PostPersistDocumentListener} or {@code PostLoadDocumentListener}, which keep it. {@code hasAnnotation} overrides
 * are removed, as Morphia now checks for the interfaces itself.
 * <p>
 * Some code can't be migrated automatically and is marked for a person to look at:
 * <ul>
 * <li>a {@code prePersist} that reads its {@code Document}, which was always empty, or a method that uses the deprecated
 * {@code Mapper} parameter;</li>
 * <li>an anonymous listener that handles more than one event, or that extends a class, since it can only implement one
 * interface;</li>
 * <li>a call to a listener method that this recipe didn't migrate in the same source file, or a call to
 * {@code hasAnnotation};</li>
 * <li>a call to the removed {@code Mapper.addInterceptor}.</li>
 * </ul>
 */
public class EntityListenerMigration extends ScanningRecipe<EntityListenerMigration.Accumulator> {
    private static final String ENTITY_LISTENER = "dev.morphia.EntityListener";
    private static final String DATASTORE = "dev.morphia.Datastore";
    private static final String MAPPER = "dev.morphia.mapping.Mapper";
    private static final String DOCUMENT = "org.bson.Document";
    private static final String LISTENERS = "listeners";
    private static final Map<String, Event> EVENTS = Map.of(
            "prePersist", new Event("PrePersistListener", "PrePersist", false, null),
            "postPersist", new Event("PostPersistListener", "PostPersist", false, "PostPersistDocumentListener"),
            "preLoad", new Event("PreLoadListener", "PreLoad", true, null),
            "postLoad", new Event("PostLoadListener", "PostLoad", false, "PostLoadDocumentListener"));

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
                + "PreLoadListener and PostLoadListener, or to PostPersistDocumentListener and PostLoadDocumentListener when "
                + "they use the Document, and their hasAnnotation overrides are removed. Calls to the removed "
                + "Mapper.addInterceptor are marked: list those listeners in MorphiaConfig.listeners() instead.";
    }

    @NotNull
    @Override
    public Accumulator getInitialValue(@NotNull ExecutionContext ctx) {
        return new Accumulator();
    }

    /**
     * Collects every source file's listener methods first, so an override can be settled against its superclass's in
     * another file.
     */
    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(@NotNull Accumulator accumulator) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                Plan plan = plan(method, getCursor().getParentTreeCursor().getValue());
                if (plan != null && !marked(method)) {
                    String key = key(method.getMethodType());
                    accumulator.own.put(key, plan);
                    TypeUtils.findOverriddenMethod(method.getMethodType())
                            .ifPresent(parent -> accumulator.overridden.put(key, key(parent)));
                }
                return super.visitMethodDeclaration(method, ctx);
            }
        };
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(@NotNull Accumulator accumulator) {
        return new Migrator(accumulator.settle());
    }

    /**
     * The plan for each listener method in the source set, and the method each overrides.
     */
    static final class Accumulator {
        private final Map<String, Plan> own = new LinkedHashMap<>();
        private final Map<String, String> overridden = new HashMap<>();
        private @Nullable Map<String, Plan> settled;

        /**
         * @return the plans, with each override settled against its superclass's
         */
        Map<String, Plan> settle() {
            if (settled == null) {
                settled = new HashMap<>();
                own.keySet().forEach(this::settle);
            }
            return settled;
        }

        /**
         * An override of a superclass's listener method must keep the signature its superclass's override migrates to: it
         * keeps the {@code Document} if that one does, and can't use it if that one doesn't.
         */
        private Plan settle(String key) {
            Plan plan = settled.get(key);
            if (plan != null) {
                return plan;
            }
            plan = own.get(key);
            String parentKey = overridden.get(key);
            if (parentKey != null && own.containsKey(parentKey) && plan.event() != null && plan.problem() == null) {
                Plan parent = settle(parentKey);
                Event event = plan.event();
                // a marked superclass override keeps its old signature, so this one migrates alone, and adds its interface
                if (parent.event() != null && parent.problem() == null && !parent.event().equals(event)) {
                    plan = parent.event().keepsDocument()
                            ? new Plan(parent.event(), null, plan.needsInterface())
                            : new Plan(event, ("the overridden %s migrates to %s, which takes no Document: stop using the Document "
                                    + "here, or migrate both by hand").formatted(parentKey.replace('#', '.'), parent.event().listener()),
                                    plan.needsInterface());
                }
            }
            settled.put(key, plan);
            return plan;
        }
    }

    private static final class Migrator extends JavaIsoVisitor<ExecutionContext> {
        // the plan for each listener method in the source set, settled across overrides
        private final Map<String, Plan> plans;
        // the listener methods that migrate, and the events they migrate to, so calls to them can be updated
        private final Map<String, Event> migrated = new HashMap<>();

        Migrator(Map<String, Plan> plans) {
            this.plans = plans;
            plans.forEach((key, plan) -> {
                if (plan.event() != null && plan.problem() == null) {
                    migrated.put(key, plan.event());
                }
            });
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
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
            Plan plan = plan(method, getCursor().getParentTreeCursor().getValue());
            if (plan == null || marked(method)) {
                // not a listener method, or already marked for a person to look at in an earlier cycle
                return result;
            }
            plan = plans.getOrDefault(key(method.getMethodType()), plan);
            if (plan.event() == null) {
                // a hasAnnotation override
                return null;
            }
            if (plan.problem() != null) {
                return SearchResult.found(result, plan.problem());
            }
            // a superclass's override supplies the interface only if it migrates; one in another file may, so it's added anyway
            boolean needsInterface = plan.needsInterface() || TypeUtils.findOverriddenMethod(method.getMethodType())
                    .map(overridden -> !migrated.containsKey(key(overridden)))
                    .orElse(false);
            if (needsInterface) {
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
            String inherited = inheritedEvents(classDecl);
            if (inherited != null) {
                return SearchResult.found(result, ("inherits %s from a class that isn't a listener: implement the per-event "
                        + "listener interfaces by hand").formatted(inherited));
            }
            if (listeners.isEmpty()) {
                return result;
            }
            List<TypeTree> types = new ArrayList<>();
            boolean replaced = false;
            for (TypeTree type : implemented) {
                if (!replaced && TypeUtils.isOfClassType(type.getType(), ENTITY_LISTENER)) {
                    useTypeArgument(listeners, type);
                    types.addAll(listenerTypes(listeners, type.getPrefix()));
                    replaced = true;
                } else {
                    types.add(type);
                }
            }
            if (!replaced) {
                // the class inherits EntityListener: it adds the interfaces of the events no superclass migrates
                JavaType inheritedArgument = listenerArgument(classDecl.getType(), Map.of(), new HashSet<>());
                TypeTree argument = typeTree(inheritedArgument);
                if (argument != null) {
                    listeners.replaceAll((event, entityType) -> argument);
                    if (inheritedArgument instanceof JavaType.Class c) {
                        maybeAddImport(c.getFullyQualifiedName());
                    }
                }
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
            useTypeArgument(listeners, result.getClazz());
            addImports(listeners);
            return result.withClazz(listenerTypes(listeners, result.getClazz().getPrefix()).get(0));
        }

        /**
         * A lambda could only implement {@code hasAnnotation}, so its listener had no callbacks: an empty anonymous marker
         * does the same.
         */
        @Override
        public @Nullable J postVisit(J tree, ExecutionContext ctx) {
            J result = super.postVisit(tree, ctx);
            return result instanceof J.Lambda lambda && TypeUtils.isOfClassType(lambda.getType(), ENTITY_LISTENER)
                    ? emptyMarker(lambda)
                    : result;
        }

        private J.NewClass emptyMarker(J.Lambda lambda) {
            JavaType.ShallowClass marker = JavaType.ShallowClass.build(ENTITY_LISTENER);
            maybeAddImport(ENTITY_LISTENER, false);
            J.Identifier name = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, List.of(), "EntityListener", marker, null);
            // a diamond, so Java infers the type argument from the target, whatever it is
            J.ParameterizedType clazz = new J.ParameterizedType(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, name,
                    JContainer.build(Space.EMPTY, List.of(JRightPadded.build(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY))),
                            Markers.EMPTY),
                    marker);
            J.Block body = new J.Block(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, JRightPadded.build(false), List.of(),
                    Space.EMPTY);
            return new J.NewClass(Tree.randomId(), lambda.getPrefix(), Markers.EMPTY, null, Space.EMPTY, clazz,
                    JContainer.build(Space.EMPTY, List.of(JRightPadded.build(new J.Empty(Tree.randomId(), Space.EMPTY,
                            Markers.EMPTY))), Markers.EMPTY),
                    body, null);
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            J.MemberReference result = super.visitMemberReference(memberRef, ctx);
            if (!TypeUtils.isOfClassType(memberRef.getType(), ENTITY_LISTENER)) {
                return result;
            }
            return SearchResult.found(result, "EntityListener is now a marker: replace this method reference with a per-event "
                    + "listener, or with new EntityListener<>() {} if it only claimed events");
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation result = super.visitMethodInvocation(method, ctx);
            JavaType.Method type = method.getMethodType();
            if (type == null || method.getMarkers().findFirst(SearchResult.class).isPresent()) {
                // already marked for a person to look at
                return result;
            }
            if (type.getName().equals("addInterceptor") && TypeUtils.isAssignableTo(MAPPER, type.getDeclaringType())) {
                return SearchResult.found(result, "addInterceptor was removed in 3.0: list this listener in "
                        + "MorphiaConfig.listeners(), or name its class in morphia.listeners in a config file");
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
            if (throughMarker || !migrated.containsKey(key(type))) {
                return SearchResult.found(result, "call the migrated %s method instead".formatted(event.listener()));
            }
            event = migrated.get(key(type));
            List<Expression> arguments = method.getArguments();
            if (!TypeUtils.isAssignableTo(DATASTORE, arguments.get(2).getType())) {
                return SearchResult.found(result, "%s's method takes a Datastore".formatted(event.listener()));
            }
            if (event.keepsDocument()) {
                return result;
            }
            if (!droppable(arguments.get(1))) {
                return SearchResult.found(result, "%s's method takes no Document, and dropping this argument would drop what it does"
                        .formatted(event.listener()));
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
                return removeAnnotation(method, event, ctx);
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
            return removeAnnotation(result, event, ctx);
        }

        /**
         * An annotated method would also be called through the interface when the class is an {@code @EntityListeners} class.
         */
        private J.MethodDeclaration removeAnnotation(J.MethodDeclaration method, Event event, ExecutionContext ctx) {
            return (J.MethodDeclaration) new RemoveAnnotationVisitor(
                    new AnnotationMatcher("@dev.morphia.annotations." + event.annotation()))
                    .visitNonNull(method, ctx, getCursor().getParentOrThrow());
        }

        /**
         * Parameterizes the interfaces by the {@code EntityListener<X>} type argument, when there is one, rather than by the
         * methods' entity parameters, which may be erased.
         */
        private void useTypeArgument(Map<Event, TypeTree> listeners, TypeTree entityListener) {
            if (entityListener instanceof J.ParameterizedType parameterized && parameterized.getTypeParameters() != null
                    && parameterized.getTypeParameters().size() == 1
                    && parameterized.getTypeParameters().get(0) instanceof TypeTree argument
                    && !(argument instanceof J.Empty)) {
                listeners.replaceAll((event, entityType) -> argument);
            }
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
    private static Plan plan(J.MethodDeclaration method, Object body) {
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
        if (body instanceof J.Block block && block.getStatements().stream()
                .filter(statement -> statement instanceof J.MethodDeclaration other && event.equals(eventOf(other)))
                .count() > 1) {
            // e.g. a 2.x listener overriding both the Mapper and the Datastore overloads, which would migrate to one signature
            return new Plan(event, "%s is overridden more than once: keep one, then run the migration again"
                    .formatted(root.getName()), false);
        }
        // with no overridden method found, the root was matched by erasure, so it's EntityListener's own
        boolean needsInterface = TypeUtils.findOverriddenMethod(type)
                .map(overridden -> TypeUtils.isOfClassType(overridden.getDeclaringType(), ENTITY_LISTENER))
                .orElse(true);
        J.VariableDeclarations document = (J.VariableDeclarations) method.getParameters().get(1);
        J.VariableDeclarations datastore = (J.VariableDeclarations) method.getParameters().get(2);
        String problem = null;
        Event target = event;
        if (!event.keepsDocument() && uses(method, document)) {
            if (event.documentListener() != null) {
                // the event has an interface whose method keeps the Document
                target = event.withDocument();
            } else {
                problem = ("%s takes no Document: the one it was given was always empty, as the entity isn't encoded yet. To "
                        + "change what's saved, implement PostPersistDocumentListener").formatted(event.listener());
            }
        }
        if (problem == null && TypeUtils.isOfClassType(datastore.getType(), MAPPER) && uses(method, datastore)) {
            problem = "the Mapper parameter is gone: use datastore.getMapper()";
        }
        return new Plan(target, problem, needsInterface);
    }

    /**
     * @return the event of the old {@code EntityListener} method the given method overrides, or null
     */
    @Nullable
    private static Event eventOf(J.MethodDeclaration method) {
        JavaType.Method root = method.getMethodType() != null ? root(method.getMethodType()) : null;
        return root != null && method.getParameters().size() == 3 ? EVENTS.get(root.getName()) : null;
    }

    /**
     * @return the old event methods a listener class inherits from superclasses that aren't listeners, which nothing would
     *         migrate, or null if there are none
     */
    @Nullable
    private static String inheritedEvents(J.ClassDeclaration classDecl) {
        JavaType.FullyQualified type = classDecl.getType();
        if (type == null || !TypeUtils.isAssignableTo(ENTITY_LISTENER, type)) {
            return null;
        }
        Set<String> declared = new HashSet<>();
        classDecl.getBody().getStatements().forEach(statement -> {
            if (statement instanceof J.MethodDeclaration method && method.getMethodType() != null) {
                declared.add(signature(method.getMethodType()));
            }
        });
        List<String> inherited = new ArrayList<>();
        for (JavaType.FullyQualified parent = type.getSupertype(); parent != null
                && !TypeUtils.isAssignableTo(ENTITY_LISTENER, parent); parent = parent.getSupertype()) {
            for (JavaType.Method method : parent.getMethods()) {
                if (EVENTS.containsKey(method.getName()) && !declared.contains(signature(method))
                        && method.getParameterTypes().size() == 3
                        && TypeUtils.isOfClassType(method.getParameterTypes().get(1), DOCUMENT)) {
                    inherited.add(parent.getClassName() + "." + method.getName());
                }
            }
        }
        return inherited.isEmpty() ? null : String.join(", ", inherited);
    }

    /**
     * @return the {@code EntityListener} method that the given method overrides, directly or through superclasses, or null
     */
    private static JavaType.@Nullable Method root(JavaType.Method method) {
        JavaType.Method last = method;
        Optional<JavaType.Method> overridden = TypeUtils.findOverriddenMethod(method);
        while (overridden.isPresent()) {
            if (TypeUtils.isOfClassType(overridden.get().getDeclaringType(), ENTITY_LISTENER)) {
                return overridden.get();
            }
            last = overridden.get();
            overridden = TypeUtils.findOverriddenMethod(last);
        }
        return overriddenByErasure(last);
    }

    /**
     * {@code findOverriddenMethod} misses an override whose parameters match only by erasure, such as
     * {@code prePersist(Object, Document, Datastore)} in a {@code GenericListener<T> implements EntityListener<T>}.
     *
     * @return the {@code EntityListener} method the given method overrides by erasure, or null
     */
    private static JavaType.@Nullable Method overriddenByErasure(JavaType.Method method) {
        JavaType.FullyQualified entityListener = find(method.getDeclaringType(), new HashSet<>());
        if (entityListener == null) {
            return null;
        }
        for (JavaType.Method candidate : entityListener.getMethods()) {
            if (candidate.getName().equals(method.getName())
                    && candidate.getParameterTypes().size() == method.getParameterTypes().size()) {
                boolean matches = true;
                for (int i = 0; i < candidate.getParameterTypes().size() && matches; i++) {
                    matches = erasure(candidate.getParameterTypes().get(i)).equals(erasure(method.getParameterTypes().get(i)));
                }
                if (matches) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static JavaType.@Nullable FullyQualified find(JavaType.@Nullable FullyQualified type, Set<String> seen) {
        if (type == null || !seen.add(type.getFullyQualifiedName())) {
            return null;
        }
        if (TypeUtils.isOfClassType(type, ENTITY_LISTENER)) {
            return type;
        }
        for (JavaType.FullyQualified parent : type.getInterfaces()) {
            JavaType.FullyQualified found = find(parent, seen);
            if (found != null) {
                return found;
            }
        }
        return find(type.getSupertype(), seen);
    }

    /**
     * @return the method's name and erased parameter types
     */
    private static String signature(JavaType.Method method) {
        return method.getName() + method.getParameterTypes().stream()
                .map(EntityListenerMigration::erasure)
                .toList();
    }

    /**
     * @return true if dropping the argument loses nothing: it's a plain name, such as a forwarded parameter, or a literal
     */
    private static boolean droppable(Expression argument) {
        return argument instanceof J.Identifier || argument instanceof J.Literal;
    }

    /**
     * Resolves the {@code EntityListener} type argument a type inherits, substituting type variables along the way: for
     * {@code Listener<T> implements Audit<T>} and {@code Audit<A> extends EntityListener<A>}, it's {@code Listener}'s {@code T}.
     */
    private static @Nullable JavaType listenerArgument(@Nullable JavaType type, Map<String, JavaType> bindings, Set<String> seen) {
        JavaType.FullyQualified raw;
        Map<String, JavaType> resolved = new HashMap<>();
        if (type instanceof JavaType.Parameterized parameterized) {
            raw = parameterized.getType();
            List<JavaType> parameters = raw.getTypeParameters();
            List<JavaType> arguments = parameterized.getTypeParameters();
            for (int i = 0; i < parameters.size() && i < arguments.size(); i++) {
                if (parameters.get(i) instanceof JavaType.GenericTypeVariable variable) {
                    resolved.put(variable.getName(), substitute(arguments.get(i), bindings));
                }
            }
            if (TypeUtils.isOfClassType(raw, ENTITY_LISTENER)) {
                return arguments.isEmpty() ? null : substitute(arguments.get(0), bindings);
            }
        } else if (type instanceof JavaType.FullyQualified fullyQualified) {
            raw = fullyQualified;
        } else {
            return null;
        }
        if (!seen.add(raw.getFullyQualifiedName())) {
            return null;
        }
        for (JavaType.FullyQualified parent : raw.getInterfaces()) {
            JavaType argument = listenerArgument(parent, resolved, seen);
            if (argument != null) {
                return argument;
            }
        }
        return listenerArgument(raw.getSupertype(), resolved, seen);
    }

    private static JavaType substitute(JavaType type, Map<String, JavaType> bindings) {
        return type instanceof JavaType.GenericTypeVariable variable && bindings.containsKey(variable.getName())
                ? bindings.get(variable.getName())
                : type;
    }

    /**
     * @return a type variable or a plain class as a type tree, or null for anything else, which the caller handles
     */
    private static @Nullable TypeTree typeTree(@Nullable JavaType type) {
        if (type instanceof JavaType.GenericTypeVariable variable && !variable.getName().equals("?")) {
            return new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, List.of(), variable.getName(), variable, null);
        } else if (type instanceof JavaType.Class c && c.getTypeParameters().isEmpty()) {
            return new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, List.of(), c.getClassName(), c, null);
        }
        return null;
    }

    private static String erasure(JavaType type) {
        if (type instanceof JavaType.GenericTypeVariable variable) {
            return variable.getBounds().isEmpty() ? "java.lang.Object" : erasure(variable.getBounds().get(0));
        } else if (type instanceof JavaType.FullyQualified fullyQualified) {
            return fullyQualified.getFullyQualifiedName();
        }
        return type.toString();
    }

    private static String key(JavaType.@Nullable Method method) {
        return method == null ? "" : method.getDeclaringType().getFullyQualifiedName() + "#" + method.getName();
    }

    private static boolean toSuper(J.MethodInvocation invocation) {
        return invocation.getSelect() instanceof J.Identifier select && select.getSimpleName().equals("super");
    }

    private static boolean marked(J method) {
        return method.getMarkers().findFirst(SearchResult.class).isPresent();
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
                // the Document passed on to another listener method is dropped along with that call's, unless that method
                // may keep it. the overridden method's own may: the override is settled to match it
                JavaType.Method type = invocation.getMethodType();
                JavaType.Method root = type == null ? null
                        : TypeUtils.isOfClassType(type.getDeclaringType(), ENTITY_LISTENER) ? type : root(type);
                Event event = root != null ? EVENTS.get(root.getName()) : null;
                if (event != null && !event.keepsDocument() && (event.documentListener() == null || toSuper(invocation))
                        && invocation.getArguments().size() == 3
                        && droppable(invocation.getArguments().get(1))) {
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

    /**
     * An event's listener interface.
     *
     * @param documentListener the interface for the same event whose method takes the {@code Document}, if there's one and
     *                         this one's doesn't
     */
    private record Event(String listener, String annotation, boolean keepsDocument, @Nullable String documentListener) {
        Event withDocument() {
            return new Event(Objects.requireNonNull(documentListener), annotation, true, null);
        }
    }
}
