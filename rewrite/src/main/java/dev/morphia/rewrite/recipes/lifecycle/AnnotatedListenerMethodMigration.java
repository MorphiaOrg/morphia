package dev.morphia.rewrite.recipes.lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.ImplementInterface;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
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

import static dev.morphia.rewrite.recipes.RewriteUtils.runtimeClasspathPaths;

/**
 * Moves the annotated lifecycle methods of {@code @EntityListeners} classes onto the per-event listener interfaces. Only an
 * entity's own methods may be annotated in 3.0. The class implements each event's interface, whose method calls the class's
 * annotated methods for the event in the order they're declared, and the annotations are removed.
 */
public class AnnotatedListenerMethodMigration extends ScanningRecipe<AnnotatedListenerMethodMigration.Accumulator> {
    private static final String ENTITY_LISTENER = "dev.morphia.EntityListener";
    private static final String ENTITY_LISTENERS = "dev.morphia.annotations.EntityListeners";
    private static final String DATASTORE = "dev.morphia.Datastore";
    private static final String DOCUMENT = "org.bson.Document";
    private static final List<String> ENTITY_ANNOTATIONS = List.of("dev.morphia.annotations.Entity",
            "dev.morphia.annotations.Embedded", "dev.morphia.annotations.ExternalEntity");
    private static final Map<String, Event> EVENTS = new LinkedHashMap<>();

    static {
        for (Event event : List.of(
                new Event("PrePersist", "prePersist", "PrePersistListener", null),
                new Event("PostPersist", "postPersist", "PostPersistListener", "PostPersistDocumentListener"),
                new Event("PreLoad", "preLoad", "PreLoadListener", "PreLoadListener"),
                new Event("PostLoad", "postLoad", "PostLoadListener", "PostLoadDocumentListener"))) {
            EVENTS.put("dev.morphia.annotations." + event.annotation(), event);
        }
    }

    /** Creates a new instance. */
    public AnnotatedListenerMethodMigration() {
    }

    @NotNull
    @Override
    public @DisplayName String getDisplayName() {
        return "Move the annotated methods of @EntityListeners classes to the listener interfaces";
    }

    @NotNull
    @Override
    public @Description String getDescription() {
        return "Only an entity's own methods may be annotated with @PrePersist, @PostPersist, @PreLoad or @PostLoad. A class "
                + "listed in @EntityListeners implements the event's listener interface instead, whose method calls the class's "
                + "annotated methods. Methods that can't be called that way are marked.";
    }

    @NotNull
    @Override
    public Accumulator getInitialValue(@NotNull ExecutionContext ctx) {
        return new Accumulator();
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(@NotNull Accumulator accumulator) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                if (TypeUtils.isOfClassType(annotation.getType(), ENTITY_LISTENERS)) {
                    new JavaIsoVisitor<Set<String>>() {
                        @Override
                        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Set<String> listed) {
                            JavaType.FullyQualified type = TypeUtils.asFullyQualified(fieldAccess.getTarget().getType());
                            if (fieldAccess.getSimpleName().equals("class") && type != null) {
                                listed.add(type.getFullyQualifiedName());
                            }
                            return fieldAccess;
                        }
                    }.visit(annotation, accumulator.listed);
                }
                return super.visitAnnotation(annotation, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (!events(method).isEmpty() && !method.hasModifier(J.Modifier.Type.Static) && method.getMethodType() != null) {
                    accumulator.annotated.add(method.getMethodType().getDeclaringType().getFullyQualifiedName());
                }
                return method;
            }
        };
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(@NotNull Accumulator accumulator) {
        return new Migrator(accumulator);
    }

    /**
     * The classes listed in {@code @EntityListeners}, and the classes that ever declared an annotated lifecycle method.
     */
    static final class Accumulator {
        private final Set<String> listed = new HashSet<>();
        private final Set<String> annotated = new HashSet<>();
    }

    private static final class Migrator extends JavaIsoVisitor<ExecutionContext> {
        private final Accumulator accumulator;

        Migrator(Accumulator accumulator) {
            this.accumulator = accumulator;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration result = super.visitClassDeclaration(classDecl, ctx);
            JavaType.FullyQualified type = result.getType();
            if (type == null || result.getKind() == J.ClassDeclaration.Kind.Type.Interface || EntityListenerMigration.marked(result)
                    || isEntity(result) || !(accumulator.listed.contains(type.getFullyQualifiedName())
                            || TypeUtils.isAssignableTo(ENTITY_LISTENER, type))) {
                return result;
            }
            Map<Event, List<J.MethodDeclaration>> methods = annotatedMethods(result);
            if (methods.isEmpty()) {
                String annotated = annotatedSuperclass(type);
                return annotated == null ? result
                        : SearchResult.found(result, ("inherits annotated lifecycle methods from %s, "
                                + "which isn't a listener, and only an entity's own annotated methods are called now: implement the "
                                + "per-event listener interfaces by hand").formatted(annotated));
            }
            String inherited = listenerSuperclass(type);
            if (inherited != null) {
                return SearchResult.found(result, ("extends %s, which has lifecycle methods of its own, and only an entity's own "
                        + "annotated methods are called now: implement the per-event listener interfaces by hand, calling the "
                        + "superclass's").formatted(inherited));
            }
            JavaType entityType = entityType(type, methods);
            if (entityType == null) {
                return SearchResult.found(result, "its lifecycle methods take unrelated entity types, and only an entity's own "
                        + "annotated methods are called now: implement the per-event listener interfaces by hand");
            }
            String entityName = name(entityType);

            Map<J.MethodDeclaration, String> problems = new LinkedHashMap<>();
            // the events each method moves to an interface for
            Map<J.MethodDeclaration, List<Event>> moved = new HashMap<>();
            List<String> interfaces = new ArrayList<>();
            List<String> generated = new ArrayList<>();
            for (Map.Entry<Event, List<J.MethodDeclaration>> entry : methods.entrySet()) {
                Event event = entry.getKey();
                List<J.MethodDeclaration> eventMethods = entry.getValue();
                List<Call> calls = new ArrayList<>();
                boolean document = event.annotation().equals("PreLoad");
                for (J.MethodDeclaration method : eventMethods) {
                    Call call = call(method, event, entityType);
                    if (call.problem() != null) {
                        problems.put(method, call.problem());
                    } else {
                        calls.add(call);
                        document |= call.document();
                    }
                }
                if (calls.isEmpty()) {
                    continue;
                }
                String listener = document ? event.documentListener() : event.listener();
                int arity = document ? 3 : 2;
                J.MethodDeclaration existing = declared(result, event.method(), arity);
                // either of the event's interfaces, or a method that implements one: a class may implement only one of them
                J.MethodDeclaration other = declared(result, event.method(), document ? 2 : 3);
                if (inherits(type, event.method()) || existing == null && (other != null || implementsEvent(result, type, event, null))
                        || existing != null && implementsEvent(result, type, event, listener)) {
                    calls.forEach(call -> problems.put(call.method(), ("the class already has a %s() method for this event, so this "
                            + "method can't be called from %s's: call it from there by hand").formatted(event.method(), listener)));
                    continue;
                }
                if (existing != null) {
                    if (calls.size() == 1 && calls.get(0).method() == existing && existing.hasModifier(J.Modifier.Type.Public)
                            && existing.getMethodType() != null
                            && existing.getMethodType().getReturnType() == JavaType.Primitive.Void
                            && (existing.getTypeParameters() == null || existing.getTypeParameters().isEmpty())
                            && calls.get(0).code().equals("%s(entity, %sdatastore)".formatted(event.method(),
                                    document ? "document, " : ""))
                            && TypeUtils.isOfType(existing.getMethodType().getParameterTypes().get(0), entityType)) {
                        // already the interface's method
                        moved.computeIfAbsent(existing, m -> new ArrayList<>()).add(event);
                        interfaces.add(listener);
                    } else {
                        calls.forEach(call -> problems.put(call.method(), ("the class already declares %s() with %d parameters, so "
                                + "this method can't be called from %s's: call it from there by hand")
                                .formatted(event.method(), arity, listener)));
                    }
                    continue;
                }
                calls.forEach(call -> moved.computeIfAbsent(call.method(), m -> new ArrayList<>()).add(event));
                calls.forEach(call -> call.imports().forEach(name -> maybeAddImport(name, false)));
                interfaces.add(listener);
                StringBuilder code = new StringBuilder("@Override\npublic void %s(%s entity, %sDatastore datastore) {\n"
                        .formatted(event.method(), entityName, document ? "Document document, " : ""));
                calls.forEach(call -> code.append(call.code()).append(";\n"));
                generated.add(code.append("}").toString());
            }

            J.ClassDeclaration updated = result.withBody(result.getBody().withStatements(ListUtils.map(
                    result.getBody().getStatements(), statement -> {
                        if (statement instanceof J.MethodDeclaration method) {
                            J.MethodDeclaration migrated = removeAnnotations(method, moved.getOrDefault(method, List.of()), ctx);
                            return problems.containsKey(method) ? SearchResult.found(migrated, problems.get(method)) : migrated;
                        }
                        return statement;
                    })));
            if (interfaces.isEmpty()) {
                return updated;
            }
            updated = implement(updated, interfaces, entityType, ctx);
            for (String code : generated) {
                updated = JavaTemplate.builder(code)
                        .imports(DATASTORE, DOCUMENT)
                        .javaParser(JavaParser.fromJavaVersion().classpath(runtimeClasspathPaths()))
                        .build()
                        .apply(new Cursor(getCursor().getParentOrThrow(), updated),
                                updated.getBody().getCoordinates().lastStatement());
            }
            maybeAddImport(DATASTORE, false);
            if (generated.stream().anyMatch(code -> code.contains("Document"))) {
                maybeAddImport(DOCUMENT, false);
            }
            interfaces.forEach(listener -> maybeAddImport("dev.morphia." + listener, false));
            if (entityType instanceof JavaType.FullyQualified fullyQualified) {
                maybeAddImport(fullyQualified.getFullyQualifiedName(), false);
            }
            EVENTS.values().forEach(event -> maybeRemoveImport("dev.morphia.annotations." + event.annotation()));
            return updated;
        }

        /**
         * Adds the interfaces, in place of a directly implemented {@code EntityListener}.
         */
        private J.ClassDeclaration implement(J.ClassDeclaration classDecl, List<String> interfaces, JavaType entityType,
                ExecutionContext ctx) {
            J.ClassDeclaration result = classDecl;
            if (result.getImplements() != null) {
                List<JRightPadded<TypeTree>> kept = ListUtils.map(result.getPadding().getImplements().getPadding().getElements(),
                        padded -> TypeUtils.isOfClassType(padded.getElement().getType(), ENTITY_LISTENER) ? null : padded);
                if (kept.size() != result.getImplements().size()) {
                    maybeRemoveImport(ENTITY_LISTENER);
                    result = result.getPadding().withImplements(kept.isEmpty() ? null
                            : JContainer.build(result.getPadding().getImplements().getBefore(), kept, Markers.EMPTY));
                }
            }
            for (String listener : interfaces) {
                J.Identifier argument = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, List.of(), name(entityType),
                        entityType, null);
                result = (J.ClassDeclaration) new ImplementInterface<ExecutionContext>(result,
                        JavaType.ShallowClass.build("dev.morphia." + listener), List.<Expression> of(argument))
                        .visitNonNull(result, ctx, getCursor().getParentOrThrow());
            }
            return result;
        }

        private J.MethodDeclaration removeAnnotations(J.MethodDeclaration method, List<Event> events, ExecutionContext ctx) {
            J.MethodDeclaration result = method;
            for (Event event : events) {
                result = (J.MethodDeclaration) new RemoveAnnotationVisitor(
                        new AnnotationMatcher("@dev.morphia.annotations." + event.annotation()))
                        .visitNonNull(result, ctx, getCursor());
            }
            return result;
        }

        /**
         * @return the first superclass that declares an annotated lifecycle method and isn't a listener this recipe migrates,
         *         or null
         */
        private @Nullable String annotatedSuperclass(JavaType.FullyQualified type) {
            for (JavaType.FullyQualified parent = type.getSupertype(); parent != null
                    && !parent.getFullyQualifiedName().equals("java.lang.Object"); parent = parent.getSupertype()) {
                if (declaresAnnotated(parent) && !accumulator.listed.contains(parent.getFullyQualifiedName())
                        && !TypeUtils.isAssignableTo(ENTITY_LISTENER, parent)) {
                    return parent.getFullyQualifiedName();
                }
            }
            return null;
        }

        private boolean declaresAnnotated(JavaType.FullyQualified type) {
            return accumulator.annotated.contains(type.getFullyQualifiedName())
                    || type.getMethods().stream().anyMatch(method -> method.getAnnotations().stream()
                            .anyMatch(annotation -> EVENTS.containsKey(annotation.getFullyQualifiedName())));
        }

        /**
         * @return the first superclass that declares an annotated lifecycle method or is a listener, whose methods a generated
         *         one would override or leave uncalled; or null
         */
        private @Nullable String listenerSuperclass(JavaType.FullyQualified type) {
            for (JavaType.FullyQualified parent = type.getSupertype(); parent != null
                    && !parent.getFullyQualifiedName().equals("java.lang.Object"); parent = parent.getSupertype()) {
                if (declaresAnnotated(parent) || TypeUtils.isAssignableTo(ENTITY_LISTENER, parent)) {
                    return parent.getFullyQualifiedName();
                }
            }
            return null;
        }
    }

    /**
     * @return the class's annotated lifecycle methods by event, each event's in the order they're declared
     */
    private static Map<Event, List<J.MethodDeclaration>> annotatedMethods(J.ClassDeclaration classDecl) {
        Map<Event, List<J.MethodDeclaration>> methods = new LinkedHashMap<>();
        for (Statement statement : classDecl.getBody().getStatements()) {
            if (statement instanceof J.MethodDeclaration method && !EntityListenerMigration.marked(method)
                    && !method.hasModifier(J.Modifier.Type.Static)) {
                events(method).forEach(event -> methods.computeIfAbsent(event, e -> new ArrayList<>()).add(method));
            }
        }
        return methods;
    }

    /**
     * @return the events the method is annotated for: one method may handle several
     */
    private static List<Event> events(J.MethodDeclaration method) {
        List<Event> events = new ArrayList<>();
        for (J.Annotation annotation : method.getLeadingAnnotations()) {
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
            if (type != null && EVENTS.containsKey(type.getFullyQualifiedName())) {
                events.add(EVENTS.get(type.getFullyQualifiedName()));
            }
        }
        return events;
    }

    /**
     * The interfaces' type argument: the class's {@code EntityListener} type argument if it has one, otherwise the most
     * specific of the methods' entity parameter types, or {@code Object} when they take no entity.
     *
     * @return the type, or null if the methods' entity types are unrelated or can't be named
     */
    private static @Nullable JavaType entityType(JavaType.FullyQualified type, Map<Event, List<J.MethodDeclaration>> methods) {
        JavaType argument = EntityListenerMigration.listenerArgument(type, Map.of(), new HashSet<>());
        if (argument != null && !(argument instanceof JavaType.GenericTypeVariable variable && variable.getName().equals("?"))) {
            return nameable(argument) ? argument : null;
        }
        List<JavaType> candidates = new ArrayList<>();
        methods.values().forEach(list -> list.forEach(method -> {
            JavaType.Method methodType = method.getMethodType();
            if (methodType != null) {
                methodType.getParameterTypes().stream()
                        .filter(parameter -> !isDatastore(parameter) && !isDocument(parameter) && !methodScoped(method, parameter))
                        .forEach(candidates::add);
            }
        }));
        if (candidates.isEmpty()) {
            return JavaType.ShallowClass.build("java.lang.Object");
        }
        for (JavaType candidate : candidates) {
            if (nameable(candidate) && candidates.stream().allMatch(other -> TypeUtils.isAssignableTo(other, candidate))) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Works out how the interface's method calls an annotated method.
     */
    private static Call call(J.MethodDeclaration method, Event event, JavaType entityType) {
        JavaType.Method methodType = method.getMethodType();
        if (methodType == null) {
            return Call.problem(method, "its type isn't known: call it from %s by hand".formatted(event.listener()));
        }
        for (JavaType thrown : methodType.getThrownExceptions()) {
            if (!TypeUtils.isAssignableTo("java.lang.RuntimeException", thrown) && !TypeUtils.isAssignableTo("java.lang.Error", thrown)) {
                return Call.problem(method, ("it throws a checked exception, which %s's method can't: call it from there by hand")
                        .formatted(event.listener()));
            }
        }
        List<String> arguments = new ArrayList<>();
        List<String> imports = new ArrayList<>();
        boolean document = false;
        List<Statement> parameters = method.getParameters();
        for (int i = 0; i < methodType.getParameterTypes().size(); i++) {
            JavaType parameter = methodType.getParameterTypes().get(i);
            if (isDatastore(parameter)) {
                arguments.add("datastore");
            } else if (isDocument(parameter)) {
                if (event.documentListener() != null) {
                    arguments.add("document");
                    document = true;
                } else if (parameters.get(i) instanceof J.VariableDeclarations declaration
                        && EntityListenerMigration.uses(method, declaration)) {
                    return Call.problem(method, "PrePersistListener takes no Document: the one this method was given was always "
                            + "empty, as the entity isn't encoded yet. To change what's saved, implement PostPersistDocumentListener");
                } else {
                    arguments.add("new Document()");
                }
            } else if (methodScoped(method, parameter)) {
                return Call.problem(method, ("its entity parameter's type is the method's own type variable, which %s's method "
                        + "can't name: call it from there by hand").formatted(event.listener()));
            } else if (accepts(parameter, entityType)) {
                if (!TypeUtils.isOfType(parameter, entityType) && parameter instanceof JavaType.FullyQualified cast) {
                    imports.add(cast.getFullyQualifiedName());
                }
                // cast to the parameter's type, so an overload taking the entity's own type isn't called instead
                arguments.add(TypeUtils.isOfType(parameter, entityType) ? "entity" : "(%s) entity".formatted(name(parameter)));
            } else {
                return Call.problem(method, ("%s's method can't pass its %s entity to this method's %s parameter: call it from "
                        + "there by hand").formatted(event.listener(), name(entityType), name(parameter)));
            }
        }
        return new Call(method, "%s(%s)".formatted(method.getSimpleName(), String.join(", ", arguments)), document, imports, null);
    }

    private static boolean accepts(JavaType parameter, JavaType entityType) {
        if (TypeUtils.isOfClassType(parameter, "java.lang.Object")) {
            return true;
        }
        if (entityType instanceof JavaType.GenericTypeVariable variable) {
            if (parameter instanceof JavaType.GenericTypeVariable other && other.getName().equals(variable.getName())) {
                return true;
            }
            return variable.getBounds().stream().anyMatch(bound -> TypeUtils.isAssignableTo(parameter, bound));
        }
        return TypeUtils.isAssignableTo(parameter, entityType);
    }

    private static J.@Nullable MethodDeclaration declared(J.ClassDeclaration classDecl, String name, int arity) {
        for (Statement statement : classDecl.getBody().getStatements()) {
            if (statement instanceof J.MethodDeclaration method && method.getSimpleName().equals(name)
                    && method.getParameters().stream().filter(parameter -> !(parameter instanceof J.Empty)).count() == arity) {
                return method;
            }
        }
        return null;
    }

    /**
     * @param except an interface to leave out, or null
     * @return true if the class implements one of the event's interfaces, directly, through a subinterface or a superclass,
     *         or as rewritten by an earlier recipe in this run
     */
    private static boolean implementsEvent(J.ClassDeclaration classDecl, JavaType.FullyQualified type, Event event,
            @Nullable String except) {
        return Stream.of(event.listener(), event.documentListener())
                .filter(listener -> listener != null && !listener.equals(except))
                .map(listener -> "dev.morphia." + listener)
                .anyMatch(listener -> TypeUtils.isAssignableTo(listener, type) || classDecl.getImplements() != null
                        && classDecl.getImplements().stream().anyMatch(implemented -> TypeUtils.isAssignableTo(listener,
                                implemented.getType())));
    }

    /**
     * @return true if a superclass declares a method with the name and the arity of one of the event's interface methods,
     *         which a generated method would override or clash with
     */
    private static boolean inherits(JavaType.FullyQualified type, String name) {
        for (JavaType.FullyQualified parent = type.getSupertype(); parent != null
                && !parent.getFullyQualifiedName().equals("java.lang.Object"); parent = parent.getSupertype()) {
            if (parent.getMethods().stream().anyMatch(method -> method.getName().equals(name)
                    && (method.getParameterTypes().size() == 2 || method.getParameterTypes().size() == 3))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the type is one of the method's own type variables, which can't be named outside it
     */
    private static boolean methodScoped(J.MethodDeclaration method, JavaType type) {
        return type instanceof JavaType.GenericTypeVariable variable && method.getTypeParameters() != null
                && method.getTypeParameters().stream().anyMatch(parameter -> parameter.getName() instanceof J.Identifier name
                        && name.getSimpleName().equals(variable.getName()));
    }

    private static boolean isEntity(J.ClassDeclaration classDecl) {
        return classDecl.getLeadingAnnotations().stream()
                .anyMatch(annotation -> ENTITY_ANNOTATIONS.stream().anyMatch(name -> TypeUtils.isOfClassType(annotation.getType(), name)));
    }

    private static boolean isDatastore(JavaType type) {
        return TypeUtils.isOfClassType(type, DATASTORE);
    }

    private static boolean isDocument(JavaType type) {
        return TypeUtils.isOfClassType(type, DOCUMENT);
    }

    private static boolean nameable(JavaType type) {
        return type instanceof JavaType.GenericTypeVariable
                || type instanceof JavaType.Class c && !(type instanceof JavaType.Parameterized) && c.getTypeParameters().isEmpty();
    }

    private static String name(JavaType type) {
        if (type instanceof JavaType.GenericTypeVariable variable) {
            return variable.getName();
        } else if (type instanceof JavaType.FullyQualified fullyQualified) {
            return fullyQualified.getClassName();
        }
        return type.toString();
    }

    /**
     * A call from an interface's method to an annotated method, or why there can't be one.
     *
     * @param document true if it passes the {@code Document}
     * @param imports  the types its casts name
     */
    private record Call(J.MethodDeclaration method, String code, boolean document, List<String> imports,
            @Nullable String problem) {
        static Call problem(J.MethodDeclaration method, String problem) {
            return new Call(method, "", false, List.of(), problem);
        }
    }

    /**
     * A lifecycle event.
     *
     * @param documentListener the interface whose method takes the {@code Document}, or null if there's none
     */
    private record Event(String annotation, String method, String listener, @Nullable String documentListener) {
    }
}
