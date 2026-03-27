package dev.morphia.critter.parser.gizmo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.internal.CollationBuilder;
import dev.morphia.annotations.internal.EntityBuilder;
import dev.morphia.annotations.internal.EntityListenersBuilder;
import dev.morphia.annotations.internal.FieldBuilder;
import dev.morphia.annotations.internal.IndexBuilder;
import dev.morphia.annotations.internal.IndexOptionsBuilder;
import dev.morphia.annotations.internal.IndexesBuilder;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.ClassfileOutput;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.Generators;
import dev.morphia.critter.parser.asm.AddMethodAccessorMethods;
import dev.morphia.critter.sources.Example;
import dev.morphia.critter.sources.MethodExample;
import dev.morphia.mapping.ReflectiveMapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.lifecycle.EntityListenerAdapter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodDescriptor;

import static com.mongodb.client.model.CollationCaseFirst.LOWER;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

public class TestGizmoGeneration {
    private final CritterClassLoader critterClassLoader = new CritterClassLoader();

    @Test
    public void testMapStringExample() {
        String descString = "Ljava/util/Map<Ljava/lang/String;Ldev/morphia/critter/sources/Example;>;";
        String descriptor = descriptor(
                java.util.Map.class,
                descriptor(String.class),
                descriptor(Example.class));

        Assert.assertEquals(descriptor, descString);
        TypeData<?> typeData = PropertyModelGenerator.typeData(descString, Thread.currentThread().getContextClassLoader()).get(0);
        Assert.assertEquals(
                typeData,
                typeDataHelper(java.util.Map.class, typeDataHelper(String.class), typeDataHelper(Example.class)));
    }

    @Test
    public void testListMapStringExample() {
        String descString = "Ljava/util/List<Ljava/util/Map<Ljava/lang/String;Ldev/morphia/critter/sources/Example;>;>;";
        String descriptor = descriptor(
                java.util.List.class,
                descriptor(
                        java.util.Map.class,
                        descriptor(String.class),
                        descriptor(Example.class)));
        Assert.assertEquals(descriptor, descString);

        TypeData<?> typeData = PropertyModelGenerator.typeData(descString, Thread.currentThread().getContextClassLoader()).get(0);
        Assert.assertEquals(
                typeData,
                typeDataHelper(java.util.List.class,
                        typeDataHelper(java.util.Map.class, typeDataHelper(String.class), typeDataHelper(Example.class))));
    }

    @Test
    public void testMapOfList() {
        String descString = "Ljava/util/Map<Ljava/lang/String;Ljava/util/List<Ldev/morphia/critter/sources/Example;>;>;";
        String descriptor = descriptor(
                java.util.Map.class,
                descriptor(String.class),
                descriptor(java.util.List.class, descriptor(Example.class)));
        Assert.assertEquals(descriptor, descString);
        TypeData<?> typeData = PropertyModelGenerator.typeData(descriptor, Thread.currentThread().getContextClassLoader()).get(0);
        Assert.assertEquals(
                typeData,
                typeDataHelper(java.util.Map.class,
                        typeDataHelper(String.class),
                        typeDataHelper(java.util.List.class, typeDataHelper(Example.class))));
    }

    @Test
    public void testPrimitiveArray() {
        TypeData<?> typeData = PropertyModelGenerator.typeData("[I", Thread.currentThread().getContextClassLoader()).get(0);
        Assert.assertTrue(typeData.isArray());
    }

    @Test
    public void testAnnotationBuilding() throws Exception {
        AnnotationNode index = new AnnotationNode("Ldev/morphia/annotations/Index;");
        AnnotationNode field = new AnnotationNode("Ldev/morphia/annotations/Field;");
        index.values = List.of("fields", List.of(field));

        try (ClassCreator creator = ClassCreator.builder()
                .className("critter.AnnotationTest")
                .superClass(EntityModel.class)
                .classOutput((name, data) -> {
                    String className = name.replace('/', '.');
                    critterClassLoader.register(className, data);
                    try {
                        ClassfileOutput.dump(name, data);
                    } catch (Exception ignored) {
                    }
                })
                .build()) {
            var mc = creator.getMethodCreator("test", Void.class);
            MethodDescriptor annotationMethod = ofMethod(
                    EntityModel.class.getName(),
                    "annotation",
                    EntityModel.class.getName(),
                    java.lang.annotation.Annotation.class);
            mc.invokeVirtualMethod(
                    annotationMethod,
                    mc.getThis(),
                    GizmoExtensions.annotationBuilder(index, mc));
        }
    }

    @Test
    public void testGizmo() throws Exception {
        MorphiaConfig config = dev.morphia.config.MorphiaConfig.load();
        Generators generators = new Generators(config, new ReflectiveMapper(config));
        CritterGizmoGenerator.generate(Example.class, critterClassLoader, generators, false);
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.AgeModel");
        Class<?> nameModel = critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.NameModel");
        invokeAll(PropertyModel.class, nameModel);
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.SalaryModel");
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.AgeAccessor").getConstructor().newInstance();
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.NameAccessor").getConstructor().newInstance();
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.SalaryAccessor").getConstructor().newInstance();

        Class<?> loadClass = critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.ExampleEntityModel");
        EntityModel model = (EntityModel) loadClass.getConstructors()[0].newInstance(generators.getMapper());
        validate(model);
    }

    private void validate(EntityModel model) {
        Assert.assertEquals(
                model.getAnnotation(EntityListeners.class),
                EntityListenersBuilder.entityListenersBuilder().value(EntityListenerAdapter.class).build());
        Assert.assertEquals(
                model.getAnnotation(Entity.class),
                EntityBuilder.entityBuilder().value("examples").build());
        Assert.assertEquals(
                model.getAnnotation(Indexes.class),
                IndexesBuilder.indexesBuilder()
                        .value(IndexBuilder.indexBuilder()
                                .fields(FieldBuilder.fieldBuilder().value("name").weight(42).build())
                                .options(IndexOptionsBuilder.indexOptionsBuilder()
                                        .partialFilter("partial filter")
                                        .collation(CollationBuilder.collationBuilder().caseFirst(LOWER).build())
                                        .build())
                                .build())
                        .build());
        Assert.assertEquals(model.collectionName(), "examples");
        Assert.assertEquals(model.discriminator(), "Example");
        Assert.assertEquals(model.discriminatorKey(), "_t");
        Assert.assertEquals(model.getType().getName(), Example.class.getName());
        Assert.assertFalse(model.getProperties().isEmpty(), "Should have properties");
        Assert.assertNotNull(model.getIdProperty(), "Should have an ID property");
        Assert.assertFalse(model.isAbstract(), "Should not be abstract");
        Assert.assertFalse(model.isInterface(), "Should not be an interface");
        Assert.assertTrue(model.useDiscriminator(), "Should use the discriminator");
        Assert.assertTrue(model.classHierarchy().isEmpty(), "Should not have a class hierarchy");
    }

    private void invokeAll(Class<?> type, Class<?> klass) {
        Object instance;
        try {
            instance = klass.getConstructors()[0].newInstance(new Object[] { null });
        } catch (Exception e) {
            Assert.fail("Could not instantiate " + klass.getName() + ": " + e.getMessage());
            return;
        }
        List<String> results = Arrays.stream(type.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers())
                        && !Modifier.isFinal(m.getModifiers())
                        && m.getParameterCount() == 0)
                .filter(m -> !List.of("hashCode", "toString").contains(m.getName()))
                .sorted(Comparator.comparing(Method::getName))
                .map(method -> {
                    try {
                        klass.getDeclaredMethod(method.getName(), method.getParameterTypes());
                        return null;
                    } catch (Exception e) {
                        return e.getMessage();
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!results.isEmpty()) {
            Assert.fail("Missing methods from " + type.getName() + ": \n" + String.join("\n", results));
        }
    }

    @Test
    public void testConstructors() throws Exception {
        String className = "dev.morphia.critter.GizmoSubclass";

        try (ClassCreator constructorCall = ClassCreator.builder()
                .classOutput((name, data) -> critterClassLoader.register(name.replace('/', '.'), data))
                .className("dev.morphia.critter.ConstructorCall")
                .build()) {
            var fieldCreator = constructorCall.getFieldCreator("name", String.class)
                    .setModifiers(Modifier.PUBLIC);
            var constructorCreator = constructorCall.getConstructorCreator(String.class);
            constructorCreator.invokeSpecialMethod(
                    MethodDescriptor.ofConstructor(Object.class),
                    constructorCreator.getThis());
            constructorCreator.setParameterNames(new String[] { "name" });
            constructorCreator.writeInstanceField(
                    fieldCreator.getFieldDescriptor(),
                    constructorCreator.getThis(),
                    constructorCreator.getMethodParam(0));
            constructorCreator.returnVoid();
        }

        critterClassLoader.loadClass("dev.morphia.critter.ConstructorCall")
                .getConstructor(String.class)
                .newInstance("here i am");

        try (ClassCreator creator = ClassCreator.builder()
                .classOutput((name, data) -> critterClassLoader.register(name.replace('/', '.'), data))
                .className(className)
                .superClass("dev.morphia.critter.ConstructorCall")
                .build()) {
            var constructor = creator.getConstructorCreator(String.class);
            constructor.invokeSpecialMethod(
                    MethodDescriptor.ofConstructor("dev.morphia.critter.ConstructorCall", String.class),
                    constructor.getThis(),
                    constructor.getMethodParam(0));
            constructor.setParameterNames(new String[] { "subName" });
            constructor.returnVoid();
        }

        Object instance = critterClassLoader.loadClass(className)
                .getConstructor(String.class)
                .newInstance("This is my name");
        Assert.assertNotNull(instance);
    }

    @Test
    public void testMethodBasedAccessors() throws Exception {
        CritterClassLoader classLoader = new CritterClassLoader();

        String resourceName = MethodExample.class.getName().replace('.', '/') + ".class";
        var inputStream = MethodExample.class.getClassLoader().getResourceAsStream(resourceName);
        ClassNode classNode = new ClassNode();
        new ClassReader(inputStream).accept(classNode, 0);

        List<MethodNode> methodNodes = classNode.methods.stream()
                .filter(node -> node.name.startsWith("get"))
                .filter(node -> Type.getArgumentTypes(node.desc).length == 0)
                .filter(node -> node.visibleAnnotations != null
                        && node.visibleAnnotations.stream().anyMatch(
                                ann -> List.of("Ldev/morphia/annotations/Id;", "Ldev/morphia/annotations/Property;").contains(ann.desc)))
                .collect(Collectors.toList());

        List<String> methodNames = methodNodes.stream().map(n -> n.name).collect(Collectors.toList());
        Assert.assertTrue(methodNames.contains("getId"), "Should find getId method");
        Assert.assertTrue(methodNames.contains("getCount"), "Should find getCount method");
        Assert.assertTrue(methodNames.contains("getScore"), "Should find getScore method");
        Assert.assertTrue(methodNames.contains("getComputedValue"), "Should find getComputedValue method");
        Assert.assertEquals(4, methodNodes.size(), "Should find exactly 4 annotated getter methods");

        byte[] bytecode = new AddMethodAccessorMethods(MethodExample.class, methodNodes).emit();

        classLoader.register(MethodExample.class.getName(), bytecode);
        Class<?> modifiedClass = classLoader.loadClass(MethodExample.class.getName());

        Assert.assertNotNull(modifiedClass.getMethod("__readId"), "Should have __readId method");
        Assert.assertNotNull(modifiedClass.getMethod("__readCount"), "Should have __readCount method");
        Assert.assertNotNull(modifiedClass.getMethod("__readScore"), "Should have __readScore method");
        Assert.assertNotNull(modifiedClass.getMethod("__readComputedValue"), "Should have __readComputedValue method");

        Assert.assertNotNull(modifiedClass.getMethod("__writeId", org.bson.types.ObjectId.class), "Should have __writeId method");
        Assert.assertNotNull(modifiedClass.getMethod("__writeCount", long.class), "Should have __writeCount method");
        Assert.assertNotNull(modifiedClass.getMethod("__writeScore", double.class), "Should have __writeScore method");

        Object instance = modifiedClass.getConstructor().newInstance();
        Method writeComputedMethod = modifiedClass.getMethod("__writeComputedValue", String.class);

        try {
            writeComputedMethod.invoke(instance, "test value");
            Assert.fail("Should throw UnsupportedOperationException for read-only property");
        } catch (InvocationTargetException e) {
            Assert.assertTrue(e.getCause() instanceof UnsupportedOperationException,
                    "Should throw UnsupportedOperationException, got: " + e.getCause());
            Assert.assertTrue(
                    e.getCause().getMessage() != null && e.getCause().getMessage().contains("read-only"),
                    "Exception message should mention read-only");
        }
    }

    private String descriptor(Class<?> type, String... typeParameters) {
        String desc = Type.getDescriptor(type);
        if (typeParameters.length > 0) {
            desc = desc.substring(0, desc.length() - 1)
                    + "<" + String.join("", typeParameters) + ">"
                    + ";";
        }
        return desc;
    }

    @SuppressWarnings("unchecked")
    private static <T> TypeData<T> typeDataHelper(Class<T> clazz, TypeData<?>... params) {
        return new TypeData<>(clazz, Arrays.asList(params));
    }
}
