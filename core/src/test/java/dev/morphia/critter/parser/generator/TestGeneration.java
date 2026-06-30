package dev.morphia.critter.parser.generator;

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
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.MethodInfo;
import dev.morphia.critter.sources.Example;
import dev.morphia.critter.sources.MethodExample;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.lifecycle.EntityListenerAdapter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassModel;
import io.github.dmlloyd.classfile.attribute.RuntimeVisibleAnnotationsAttribute;

import static com.mongodb.client.model.CollationCaseFirst.LOWER;
import static dev.morphia.critter.parser.GeneratorsTestHelper.defaultMapper;
import static io.github.dmlloyd.classfile.Attributes.runtimeVisibleAnnotations;

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
                    List<io.github.dmlloyd.classfile.Annotation> anns = rva.map(RuntimeVisibleAnnotationsAttribute::annotations)
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
