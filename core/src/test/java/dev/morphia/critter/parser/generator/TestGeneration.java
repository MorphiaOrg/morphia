package dev.morphia.critter.parser.generator;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.tools.ToolProvider;

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
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.MethodInfo;
import dev.morphia.critter.sources.CircleExample;
import dev.morphia.critter.sources.EmbeddedExample;
import dev.morphia.critter.sources.Example;
import dev.morphia.critter.sources.FinalFieldsExample;
import dev.morphia.critter.sources.GetterIdExample;
import dev.morphia.critter.sources.MethodExample;
import dev.morphia.critter.sources.PackageChildExample;
import dev.morphia.critter.sources.ShadowingExample;
import dev.morphia.critter.sources.ShapeExample;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.lifecycle.EntityListenerAdapter;

import org.bson.codecs.pojo.PropertyAccessor;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.attribute.RuntimeVisibleAnnotationsAttribute;

import static com.mongodb.client.model.CollationCaseFirst.LOWER;
import static dev.morphia.critter.parser.GeneratorsTestHelper.defaultMapper;
import static io.smallrye.classfile.Attributes.runtimeVisibleAnnotations;

public class TestGeneration {
    private final CritterClassLoader critterClassLoader = new CritterClassLoader();

    @Test
    public void testMapStringExample() {
        String descString = "Ljava/util/Map<Ljava/lang/String;Ldev/morphia/critter/sources/Example;>;";
        TypeData<?> typeData = PropertyModelGenerator.typeData(descString, Thread.currentThread().getContextClassLoader()).get(0);
        Assertions.assertEquals(typeDataHelper(java.util.Map.class, typeDataHelper(String.class), typeDataHelper(Example.class)), typeData);
    }

    @Test
    public void testListMapStringExample() {
        String descString = "Ljava/util/List<Ljava/util/Map<Ljava/lang/String;Ldev/morphia/critter/sources/Example;>;>;";
        TypeData<?> typeData = PropertyModelGenerator.typeData(descString, Thread.currentThread().getContextClassLoader()).get(0);
        Assertions.assertEquals(typeDataHelper(java.util.List.class,
                typeDataHelper(java.util.Map.class, typeDataHelper(String.class), typeDataHelper(Example.class))), typeData);
    }

    @Test
    public void testMapOfList() {
        String descString = "Ljava/util/Map<Ljava/lang/String;Ljava/util/List<Ldev/morphia/critter/sources/Example;>;>;";
        TypeData<?> typeData = PropertyModelGenerator.typeData(descString, Thread.currentThread().getContextClassLoader()).get(0);
        Assertions.assertEquals(typeDataHelper(java.util.Map.class,
                typeDataHelper(String.class),
                typeDataHelper(java.util.List.class, typeDataHelper(Example.class))), typeData);
    }

    @Test
    public void testPrimitiveArray() {
        TypeData<?> typeData = PropertyModelGenerator.typeData("[I", Thread.currentThread().getContextClassLoader()).get(0);
        Assertions.assertTrue(typeData.isArray());
    }

    @Test
    public void testMultiDimensionalArray() {
        TypeData<?> typeData = PropertyModelGenerator.typeData("[[I", Thread.currentThread().getContextClassLoader()).get(0);
        Assertions.assertTrue(typeData.isArray());
        Assertions.assertEquals(int[][].class, typeData.getType());
    }

    @Test
    public void testMalformedSignatureReturnsEmpty() {
        var result = PropertyModelGenerator.typeData("!!not-a-valid-signature!!", Thread.currentThread().getContextClassLoader());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testGenerator() throws Exception {
        new CritterGenerator(defaultMapper()).generate(Example.class, critterClassLoader, false);
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.AgeModel");
        Class<?> nameModel = critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.NameModel");
        invokeAll(PropertyModel.class, nameModel);
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.SalaryModel");
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.AgeAccessor").getConstructor().newInstance();
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.NameAccessor").getConstructor().newInstance();
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.SalaryAccessor").getConstructor().newInstance();

        Class<?> loadClass = critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.ExampleEntityModel");
        EntityModel model = (EntityModel) loadClass.getConstructors()[0].newInstance(defaultMapper());
        validate(model);
    }

    @Test
    public void testGeneratorWithoutId() throws Exception {
        new CritterGenerator(defaultMapper()).generate(EmbeddedExample.class, critterClassLoader, false);

        Class<?> loadClass = critterClassLoader
                .loadClass("dev.morphia.critter.sources.__morphia.embeddedexample.EmbeddedExampleEntityModel");
        EntityModel model = (EntityModel) loadClass.getConstructors()[0].newInstance(defaultMapper());
        Assertions.assertEquals(EmbeddedExample.class.getName(), model.getType().getName());
        Assertions.assertNull(model.getIdProperty(), "Should not have an ID property");
        Assertions.assertEquals(List.of("street_name", "number"),
                model.getProperties().stream().map(PropertyModel::getMappedName).toList());
    }

    @Test
    public void testGeneratorSkipsIdOnGetter() {
        var e = Assertions.assertThrows(UnsupportedOperationException.class,
                () -> new CritterGenerator(defaultMapper()).generate(GetterIdExample.class, critterClassLoader, false));
        Assertions.assertTrue(e.getMessage().contains("@Id on getter"), e.getMessage());
    }

    @Test
    public void testGeneratorInheritedPrivateFields() throws Exception {
        EntityModel model = generateModel(CircleExample.class);
        Object circle = critterClassLoader.loadClass(CircleExample.class.getName()).getConstructor().newInstance();

        assertRoundTrip(model, circle, "color", "red");
        assertRoundTrip(model, circle, "radius", 2.5);
        Assertions.assertNotNull(model.getIdProperty(), "Should find the inherited ID property");
    }

    @Test
    public void testGeneratorInheritedPackagePrivateFields() throws Exception {
        EntityModel model = generateModel(PackageChildExample.class);
        Object child = critterClassLoader.loadClass(PackageChildExample.class.getName()).getConstructor().newInstance();

        assertRoundTrip(model, child, "label", "a label");
        assertRoundTrip(model, child, "size", 42);
    }

    @Test
    public void testGeneratorFinalFields() throws Exception {
        EntityModel model = generateModel(FinalFieldsExample.class);
        Object entity = critterClassLoader.loadClass(FinalFieldsExample.class.getName())
                .getConstructor(String.class, int.class)
                .newInstance("initial", 1);

        assertRoundTrip(model, entity, "code", "updated");
        assertRoundTrip(model, entity, "count", 42);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testGeneratorFinalFieldWriteFailureIsWrapped() throws Exception {
        EntityModel model = generateModel(FinalFieldsExample.class);
        Class<?> woven = critterClassLoader.loadClass(FinalFieldsExample.class.getName());
        Object entity = woven.getConstructor(String.class, int.class).newInstance("initial", 1);

        // Seed the woven writer's Field cache with a copy that was never made accessible, so Field.set throws the
        // checked IllegalAccessException.
        Field cache = woven.getDeclaredField("__fieldCode");
        cache.setAccessible(true);
        cache.set(null, woven.getDeclaredField("code"));

        PropertyAccessor accessor = model.getProperty("code").getAccessor();
        Throwable thrown = Assertions.assertThrows(Throwable.class, () -> accessor.set(entity, "updated"));
        Assertions.assertEquals(RuntimeException.class, thrown.getClass());
        Assertions.assertEquals("Failed to set final field 'code'", thrown.getMessage());
        Assertions.assertInstanceOf(IllegalAccessException.class, thrown.getCause());
    }

    @Test
    public void testRuntimeGeneratorFinalFields() throws Exception {
        // A fresh loader: runtime mode leaves the entity class alone, so it must resolve to the application's copy.
        CritterClassLoader loader = new CritterClassLoader(getClass().getClassLoader());
        EntityModelGenerator generator = new CritterGenerator(defaultMapper()).generate(FinalFieldsExample.class, loader, true);
        EntityModel model = (EntityModel) loader.loadClass(generator.getGeneratedType())
                .getConstructor(Mapper.class)
                .newInstance(defaultMapper());
        FinalFieldsExample entity = new FinalFieldsExample("initial", 1);

        assertRoundTrip(model, entity, "code", "updated");
        assertRoundTrip(model, entity, "count", 42);
    }

    @Test
    public void testGeneratorAbstractEntity() throws Exception {
        EntityModel model = generateModel(ShapeExample.class);

        Assertions.assertTrue(model.isAbstract(), "Should be abstract");
        Assertions.assertEquals(List.of("_id", "color"),
                model.getProperties().stream().map(PropertyModel::getMappedName).toList());
    }

    @Test
    public void testGeneratorSiblingsShareSuperclassAccessors() throws Exception {
        CritterGenerator generator = new CritterGenerator(defaultMapper());
        generator.generate(CircleExample.class, critterClassLoader, false);
        byte[] fromCircle = critterClassLoader.getTypeDefinitions().get(ShapeExample.class.getName());
        generator.generate(ShapeExample.class, critterClassLoader, false);
        byte[] fromShape = critterClassLoader.getTypeDefinitions().get(ShapeExample.class.getName());

        Assertions.assertArrayEquals(fromCircle, fromShape,
                "The superclass should be woven the same way no matter which entity triggers it");
    }

    @Test
    public void testGeneratorSkipsShadowedFields() {
        var e = Assertions.assertThrows(UnsupportedOperationException.class,
                () -> new CritterGenerator(defaultMapper()).generate(ShadowingExample.class, critterClassLoader, false));
        Assertions.assertTrue(e.getMessage().contains("shadows"), e.getMessage());
    }

    @Test
    public void testGeneratorLibrarySuperclassFields(@TempDir Path dir) throws Exception {
        // The superclasses live in a different classpath location than the entities, like a library would, so they
        // can't be rewritten: an accessible field is woven into the entity, an unreachable one skips AOT.
        Path library = dir.resolve("library");
        Path app = dir.resolve("app");
        compile(library, Map.of(
                "lib/SharedBase.java", "package lib; public class SharedBase { protected String shared; }",
                "lib/PrivateBase.java", "package lib; public class PrivateBase { private String hidden; }",
                "lib/PackageBase.java", "package lib; public class PackageBase { String hidden; }"));
        String entity = """
                package app;
                @dev.morphia.annotations.Entity
                public class %s extends lib.%s {
                    @dev.morphia.annotations.Id
                    private org.bson.types.ObjectId id;
                }
                """;
        compile(app, Map.of(
                "app/SharedChild.java", entity.formatted("SharedChild", "SharedBase"),
                "app/PrivateChild.java", entity.formatted("PrivateChild", "PrivateBase"),
                "app/PackageChild.java", entity.formatted("PackageChild", "PackageBase")), library);

        try (URLClassLoader loader = new URLClassLoader(new URL[] { app.toUri().toURL(), library.toUri().toURL() },
                getClass().getClassLoader())) {
            CritterClassLoader classLoader = new CritterClassLoader(loader);
            CritterGenerator generator = new CritterGenerator(defaultMapper());

            EntityModelGenerator modelGenerator = generator.generate(loader.loadClass("app.SharedChild"), classLoader, false);
            Assertions.assertTrue(classLoader.getTypeDefinitions().containsKey("app.SharedChild"));
            Assertions.assertFalse(classLoader.getTypeDefinitions().containsKey("lib.SharedBase"),
                    "A library superclass must not be rewritten");
            EntityModel model = (EntityModel) classLoader.loadClass(modelGenerator.getGeneratedType())
                    .getConstructor(Mapper.class).newInstance(defaultMapper());
            Object child = classLoader.loadClass("app.SharedChild").getConstructor().newInstance();
            assertRoundTrip(model, child, "shared", "a value");

            for (String name : List.of("app.PrivateChild", "app.PackageChild")) {
                Class<?> type = loader.loadClass(name);
                var e = Assertions.assertThrows(UnsupportedOperationException.class,
                        () -> generator.generate(type, classLoader, false));
                Assertions.assertTrue(e.getMessage().contains("inaccessible inherited field 'hidden'"), e.getMessage());
            }
        }
    }

    @Test
    public void testGeneratorFinalFieldBehindTransientField(@TempDir Path dir) throws Exception {
        // The entity's transient field isn't mapped and the library superclass can't be rewritten, so the inherited final
        // field's writer is woven into the entity, where a lookup by name alone would find the transient field instead.
        Path library = dir.resolve("library");
        Path app = dir.resolve("app");
        compile(library, Map.of("lib/FinalBase.java", """
                package lib;
                public class FinalBase {
                    protected final int value;
                    public FinalBase() {
                        value = 1;
                    }
                }
                """));
        compile(app, Map.of("app/TransientChild.java", """
                package app;
                @dev.morphia.annotations.Entity
                public class TransientChild extends lib.FinalBase {
                    @dev.morphia.annotations.Id
                    private org.bson.types.ObjectId id;
                    private transient String value;
                }
                """), library);

        try (URLClassLoader loader = new URLClassLoader(new URL[] { app.toUri().toURL(), library.toUri().toURL() },
                getClass().getClassLoader())) {
            CritterClassLoader classLoader = new CritterClassLoader(loader);
            EntityModelGenerator modelGenerator = new CritterGenerator(defaultMapper())
                    .generate(loader.loadClass("app.TransientChild"), classLoader, false);
            EntityModel model = (EntityModel) classLoader.loadClass(modelGenerator.getGeneratedType())
                    .getConstructor(Mapper.class).newInstance(defaultMapper());
            Object child = classLoader.loadClass("app.TransientChild").getConstructor().newInstance();

            assertRoundTrip(model, child, "value", 42);
        }
    }

    private static void compile(Path output, Map<String, String> sources, Path... classpath) throws Exception {
        Path sourceDir = output.resolveSibling(output.getFileName() + "-src");
        List<String> files = new ArrayList<>();
        for (Map.Entry<String, String> source : sources.entrySet()) {
            Path file = sourceDir.resolve(source.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, source.getValue());
            files.add(file.toString());
        }
        List<String> entries = new ArrayList<>();
        for (Class<?> type : List.of(Entity.class, ObjectId.class)) {
            entries.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        }
        for (Path path : classpath) {
            entries.add(path.toString());
        }
        List<String> args = new ArrayList<>(List.of("-d", output.toString(), "-cp", String.join(File.pathSeparator, entries)));
        args.addAll(files);
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0]));
        Assertions.assertEquals(0, result, "Fixture compilation failed");
    }

    private EntityModel generateModel(Class<?> type) throws Exception {
        EntityModelGenerator generator = new CritterGenerator(defaultMapper()).generate(type, critterClassLoader, false);
        Class<?> modelClass = critterClassLoader.loadClass(generator.getGeneratedType());
        return (EntityModel) modelClass.getConstructor(Mapper.class).newInstance(defaultMapper());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void assertRoundTrip(EntityModel model, Object entity, String property, Object value) {
        PropertyAccessor accessor = model.getProperty(property).getAccessor();
        accessor.set(entity, value);
        Assertions.assertEquals(value, accessor.get(entity), property);
    }

    private void validate(EntityModel model) {
        Assertions.assertEquals(EntityListenersBuilder.entityListenersBuilder().value(EntityListenerAdapter.class).build(),
                model.getAnnotation(EntityListeners.class));
        Assertions.assertEquals(EntityBuilder.entityBuilder().value("examples").build(), model.getAnnotation(Entity.class));
        Assertions.assertEquals(IndexesBuilder.indexesBuilder()
                .value(IndexBuilder.indexBuilder()
                        .fields(FieldBuilder.fieldBuilder().value("name").weight(42).build())
                        .options(IndexOptionsBuilder.indexOptionsBuilder()
                                .partialFilter("partial filter")
                                .collation(CollationBuilder.collationBuilder().caseFirst(LOWER).build())
                                .build())
                        .build())
                .build(), model.getAnnotation(Indexes.class));
        Assertions.assertEquals("examples", model.collectionName());
        Assertions.assertEquals("Example", model.discriminator());
        Assertions.assertEquals("_t", model.discriminatorKey());
        Assertions.assertEquals(Example.class.getName(), model.getType().getName());
        Assertions.assertFalse(model.getProperties().isEmpty(), "Should have properties");
        Assertions.assertNotNull(model.getIdProperty(), "Should have an ID property");
        Assertions.assertFalse(model.isAbstract(), "Should not be abstract");
        Assertions.assertFalse(model.isInterface(), "Should not be an interface");
        Assertions.assertTrue(model.useDiscriminator(), "Should use the discriminator");
        Assertions.assertTrue(model.classHierarchy().isEmpty(), "Should not have a class hierarchy");
    }

    private void invokeAll(Class<?> type, Class<?> klass) {
        Object instance;
        try {
            instance = klass.getConstructors()[0].newInstance(new Object[] { null });
        } catch (Exception e) {
            Assertions.fail("Could not instantiate " + klass.getName() + ": " + e.getMessage());
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
            Assertions.fail("Missing methods from " + type.getName() + ": \n" + String.join("\n", results));
        }
    }

    @Test
    public void testMethodBasedAccessors() throws Exception {
        CritterClassLoader classLoader = new CritterClassLoader();

        String resourceName = MethodExample.class.getName().replace('.', '/') + ".class";
        byte[] classBytes;
        try (var inputStream = MethodExample.class.getClassLoader().getResourceAsStream(resourceName)) {
            classBytes = inputStream.readAllBytes();
        }
        ClassModel classModel = ClassFile.of().parse(classBytes);

        List<String> targetAnnotations = List.of("Ldev/morphia/annotations/Id;", "Ldev/morphia/annotations/Property;");
        List<MethodInfo> methodInfos = classModel.methods().stream()
                .filter(m -> m.methodName().stringValue().startsWith("get"))
                .filter(m -> {
                    var rva = m.findAttribute(runtimeVisibleAnnotations());
                    if (rva.isEmpty())
                        return false;
                    return rva.get().annotations().stream()
                            .anyMatch(ann -> targetAnnotations.contains(ann.classSymbol().descriptorString()));
                })
                .map(m -> {
                    var rva = m.findAttribute(runtimeVisibleAnnotations());
                    List<io.smallrye.classfile.Annotation> anns = rva.map(RuntimeVisibleAnnotationsAttribute::annotations)
                            .orElse(List.of());
                    return new MethodInfo(
                            m.methodName().stringValue(),
                            m.methodType().stringValue(),
                            null,
                            m.flags().flagsMask(),
                            anns);
                })
                .collect(Collectors.toList());

        List<String> methodNames = methodInfos.stream().map(MethodInfo::name).collect(Collectors.toList());
        Assertions.assertTrue(methodNames.contains("getId"), "Should find getId method");
        Assertions.assertTrue(methodNames.contains("getCount"), "Should find getCount method");
        Assertions.assertTrue(methodNames.contains("getScore"), "Should find getScore method");
        Assertions.assertTrue(methodNames.contains("getComputedValue"), "Should find getComputedValue method");
        Assertions.assertEquals(4, methodInfos.size(), "Should find exactly 4 annotated getter methods");

        byte[] bytecode = new AddMethodAccessorMethods(MethodExample.class, methodInfos).emit();

        classLoader.register(MethodExample.class.getName(), bytecode);
        Class<?> modifiedClass = classLoader.loadClass(MethodExample.class.getName());

        Assertions.assertNotNull(modifiedClass.getMethod("__readId"), "Should have __readId method");
        Assertions.assertNotNull(modifiedClass.getMethod("__readCount"), "Should have __readCount method");
        Assertions.assertNotNull(modifiedClass.getMethod("__readScore"), "Should have __readScore method");
        Assertions.assertNotNull(modifiedClass.getMethod("__readComputedValue"), "Should have __readComputedValue method");

        // Reference types use Object in the bridge descriptor so non-public types never
        // appear in the accessor's constant pool; primitives keep their concrete type.
        Assertions.assertNotNull(modifiedClass.getMethod("__writeId", Object.class), "Should have __writeId method");
        Assertions.assertNotNull(modifiedClass.getMethod("__writeCount", long.class), "Should have __writeCount method");
        Assertions.assertNotNull(modifiedClass.getMethod("__writeScore", double.class), "Should have __writeScore method");

        Object instance = modifiedClass.getConstructor().newInstance();
        Method writeComputedMethod = modifiedClass.getMethod("__writeComputedValue", Object.class);

        try {
            writeComputedMethod.invoke(instance, "test value");
            Assertions.fail("Should throw UnsupportedOperationException for read-only property");
        } catch (InvocationTargetException e) {
            Assertions.assertTrue(e.getCause() instanceof UnsupportedOperationException,
                    "Should throw UnsupportedOperationException, got: " + e.getCause());
            Assertions.assertTrue(
                    e.getCause().getMessage() != null && e.getCause().getMessage().contains("read-only"),
                    "Exception message should mention read-only");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> TypeData<T> typeDataHelper(Class<T> clazz, TypeData<?>... params) {
        return new TypeData<>(clazz, Arrays.asList(params));
    }
}
