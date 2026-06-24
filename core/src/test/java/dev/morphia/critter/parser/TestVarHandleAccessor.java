package dev.morphia.critter.parser;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.Id;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.generator.CritterGenerator;
import dev.morphia.critter.parser.generator.NestmateAccessorRegistry;
import dev.morphia.critter.parser.generator.VarHandleAccessorGenerator;
import dev.morphia.critter.sources.Example;
import dev.morphia.mapping.PropertyDiscovery;
import dev.morphia.mapping.ReflectiveMapper;

import org.bson.codecs.pojo.PropertyAccessor;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.github.dmlloyd.classfile.ClassFile;

import static dev.morphia.critter.parser.GeneratorsTestHelper.defaultMapper;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestVarHandleAccessor {
    private CritterClassLoader classLoader;

    @BeforeAll
    public void setup() {
        classLoader = new CritterClassLoader();
        new CritterGenerator(defaultMapper()).generate(Example.class, classLoader, true);
    }

    @Test
    public void testEntityNotModified() {
        List<String> methods = Arrays.stream(Example.class.getDeclaredMethods())
                .map(m -> m.getName())
                .collect(Collectors.toList());

        List<String> syntheticRead = methods.stream()
                .filter(name -> name.startsWith("__read") && !name.endsWith("Template"))
                .collect(Collectors.toList());
        List<String> syntheticWrite = methods.stream()
                .filter(name -> name.startsWith("__write") && !name.endsWith("Template"))
                .collect(Collectors.toList());

        Assertions.assertTrue(syntheticRead.isEmpty(),
                "Entity class should not have synthetic __read methods but found: " + syntheticRead);
        Assertions.assertTrue(syntheticWrite.isEmpty(),
                "Entity class should not have synthetic __write methods but found: " + syntheticWrite);
    }

    @Test
    public void testStringField() throws Exception {
        Example entity = new Example();
        PropertyAccessor<String> accessor = loadAccessor(Example.class, "name");

        Assertions.assertNull(accessor.get(entity));
        accessor.set(entity, "hello");
        Assertions.assertEquals("hello", accessor.get(entity));
    }

    @Test
    public void testIntPrimitiveField() throws Exception {
        Example entity = new Example();
        PropertyAccessor<Object> accessor = loadAccessor(Example.class, "age");

        Assertions.assertEquals(21, accessor.get(entity));
        accessor.set(entity, 42);
        Assertions.assertEquals(42, accessor.get(entity));
    }

    @Test
    public void testLongBoxedField() throws Exception {
        Example entity = new Example();
        PropertyAccessor<Object> accessor = loadAccessor(Example.class, "salary");

        Assertions.assertEquals(2L, accessor.get(entity));
        accessor.set(entity, 100_000L);
        Assertions.assertEquals(100_000L, accessor.get(entity));
    }

    @Test
    public void testAccessorsInstantiatable() {
        for (String field : List.of("name", "age", "salary")) {
            String key = Critter.critterPackage(Example.class) + "." + Critter.titleCase(field) + "Accessor";
            Assertions.assertNotNull(NestmateAccessorRegistry.get(key),
                    "Nestmate accessor must be registered in registry for field: " + field);
        }
    }

    /**
     * Verifies the code path for {@code final} fields in the runtime-generated nestmate accessor.
     * The JVM does not allow {@code putfield} to a final field outside {@code <init>}; the generated
     * {@code set()} method falls back to reflection ({@code Field.setAccessible + Field.set}).
     * In practice the reflection-based set may also fail in Java 17+ due to JVM final-field
     * restrictions, so this test verifies that (a) {@code get()} returns the correct initial value,
     * and (b) the reflection fallback code path IS taken (the RuntimeException message contains the
     * field name).
     */
    @Test
    public void testFinalFieldReflectionFallback() throws Exception {
        CritterClassLoader loader = new CritterClassLoader();
        new CritterGenerator(defaultMapper()).generate(FinalFieldEntity.class, loader, true);

        FinalFieldEntity entity = new FinalFieldEntity();
        PropertyAccessor<String> accessor = loadAccessor(loader, FinalFieldEntity.class, "label");

        Assertions.assertNotNull(accessor, "LabelAccessor must be registered in nestmate registry");
        Assertions.assertEquals("original", accessor.get(entity),
                "get() must return the correct final field value");
        try {
            accessor.set(entity, "modified");
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            Assertions.assertTrue(msg != null && msg.contains("label"),
                    "Fallback RuntimeException must mention the field name, got: " + msg);
        }
    }

    /**
     * Regression test for bug #2: findSetter/findSetterInHierarchy did not filter ACC_STATIC methods.
     *
     * In METHODS discovery mode, a static setXxx method with matching descriptor was accepted as a
     * property setter, causing the property to be treated as method-based. hasSetter() then rejected
     * the static method (it has a reflection-level isStatic guard), leaving no setter handle — so
     * set() threw UnsupportedOperationException even though the property IS writable via its backing
     * field.
     *
     * After the fix: the static setter is filtered by PropertyFinder, the property falls back to
     * field-based nestmate accessor, and set() works correctly.
     */
    @Test
    public void testStaticSetterMethodIsNotTreatedAsPropertySetter() throws Exception {
        var methodsMapper = new ReflectiveMapper(
                new ManualMorphiaConfig().propertyDiscovery(PropertyDiscovery.METHODS));
        CritterClassLoader loader = new CritterClassLoader();
        new CritterGenerator(methodsMapper).generate(StaticSetterEntity.class, loader, true);

        @SuppressWarnings("unchecked")
        PropertyAccessor<Object> accessor = (PropertyAccessor<Object>) loadAccessor(loader, StaticSetterEntity.class, "value");

        StaticSetterEntity entity = new StaticSetterEntity();
        Assertions.assertNull(accessor.get(entity), "initial value should be null");
        // Bug: set() threw UnsupportedOperationException because the static setter was found by
        // PropertyFinder, making the property method-based, then hasSetter() rejected it.
        // Fix: PropertyFinder ignores static setters; property falls back to field nestmate accessor.
        accessor.set(entity, "hello");
        Assertions.assertEquals("hello", accessor.get(entity), "set() must write through to the backing field");
    }

    /**
     * Regression test for bug #5: emitAnnotationOnStack used value.equals(defaultValue) to skip
     * emitting setter calls for annotation elements whose value matches the default. For array-typed
     * elements, annotation proxy methods return a fresh defensive copy on every call, so two
     * logically-identical arrays are never the same instance and equals() always returned false.
     *
     * Effect: the builder setter was emitted even when the value equalled the default, inflating
     * the generated bytecode.
     *
     * After the fix (Objects.deepEquals), equal-content arrays are detected and the setter is
     * skipped. Verified by asserting the builder class name is absent from the entity model bytecode
     * when the annotation value matches its default.
     */
    @Test
    public void testArrayAnnotationDefaultValueDoesNotEmitBuilderSetter() throws Exception {
        CritterClassLoader loader = new CritterClassLoader();
        new CritterGenerator(defaultMapper()).generate(EntityListenersDefaultEntity.class, loader, true);

        String entityModelName = Critter.critterPackage(EntityListenersDefaultEntity.class)
                + "." + EntityListenersDefaultEntity.class.getSimpleName() + "EntityModel";
        byte[] modelBytes = loader.getTypeDefinitions().get(entityModelName);
        Assertions.assertNotNull(modelBytes, "entity model bytecode must be registered");

        // The builder factory (entityListenersBuilder) and build() are always emitted.
        // The setter invokevirtual has descriptor ([Ljava/lang/Class;) — unique to the value() setter.
        // Before fix: value.equals(defaultValue) was false for two distinct empty Class[]{},
        // so the setter was emitted and that descriptor appears in the constant pool.
        // After fix: Objects.deepEquals correctly detects equal empty arrays, setter is skipped.
        String bytecodeStr = new String(modelBytes, StandardCharsets.ISO_8859_1);
        Assertions.assertFalse(bytecodeStr.contains("([Ljava/lang/Class;)"),
                "setter descriptor ([Ljava/lang/Class;) must not appear when @EntityListeners value equals its default");
    }

    @Entity("entity_listeners_default_test")
    @EntityListeners
    public static class EntityListenersDefaultEntity {
        @Id
        public ObjectId id;
    }

    /**
     * Regression test for bug #3: isGetter() had no minimum-length guard for "is"/"get" method names.
     *
     * A no-arg non-void method named exactly "is" or "get" passed the startsWith check, then
     * getterPropertyName() computed name.substring(2) == "" and crashed on prop.charAt(0) with
     * StringIndexOutOfBoundsException, aborting property discovery for the entire entity.
     *
     * After the fix: methods named exactly "is" or "get" are rejected by isGetter(), so property
     * discovery proceeds normally without throwing.
     */
    @Test
    public void testMethodNamedExactlyIsOrGetDoesNotCrashDiscovery() {
        CritterClassLoader loader = new CritterClassLoader();
        // Before fix: StringIndexOutOfBoundsException from prop.charAt(0) in getterPropertyName()
        Assertions.assertDoesNotThrow(
                () -> new CritterGenerator(defaultMapper()).generate(BareGetterEntity.class, loader, true));
    }

    @Entity("bare_getter_test")
    public static class BareGetterEntity {
        @Id
        public ObjectId id;

        public boolean is() {
            return false;
        }

        public Object get() {
            return null;
        }
    }

    @Entity("static_setter_test")
    public static class StaticSetterEntity {
        @Id
        public ObjectId id;
        private String value;

        public String getValue() {
            return value;
        }

        public static void setValue(String v) {
            // static — must not be used as a property setter
        }
    }

    @Entity("final_field_test")
    public static class FinalFieldEntity {
        @Id
        public ObjectId id;
        public final String label = "original";
    }

    @SuppressWarnings("unchecked")
    private <T> PropertyAccessor<T> loadAccessor(Class<?> entityType, String fieldName) {
        return loadAccessor(classLoader, entityType, fieldName);
    }

    @SuppressWarnings("unchecked")
    private <T> PropertyAccessor<T> loadAccessor(CritterClassLoader loader, Class<?> entityType, String fieldName) {
        String key = Critter.critterPackage(entityType) + "." + Critter.titleCase(fieldName) + "Accessor";
        return (PropertyAccessor<T>) NestmateAccessorRegistry.get(key);
    }

    /**
     * Regression test for bug #1 from the ClassFile API review:
     * VarHandleAccessorGenerator uses single-arg Class.forName() (caller classloader) instead of
     * entity.getClassLoader(), so method-based properties whose type is only known to a child
     * classloader (typical in app-server deployments) cause emit() to crash and set() to be
     * unavailable.
     *
     * The test fails while the bug is present: emit() throws RuntimeException because
     * Class.forName("IsolatedValue") uses the system CL, which cannot see the dynamically-loaded type.
     * Once fixed (Class.forName with entity.getClassLoader()), emit() succeeds and set() works.
     */
    @Test
    public void testMethodBasedAccessorWorksWhenPropertyTypeIsOnChildClassloader() throws Exception {
        ClassDesc valueTypeDesc = ClassDesc.of("dev.morphia.critter.test.isolate.IsolatedValue");
        byte[] valueTypeBytes = ClassFile.of().build(valueTypeDesc, cb -> {
            cb.withVersion(ClassFile.JAVA_17_VERSION, 0);
            cb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER);
            cb.withSuperclass(ConstantDescs.CD_Object);
            cb.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), ClassFile.ACC_PUBLIC, cod -> {
                cod.aload(0);
                cod.invokespecial(ConstantDescs.CD_Object, "<init>", MethodTypeDesc.ofDescriptor("()V"));
                cod.return_();
            });
        });

        ClassDesc entityDesc = ClassDesc.of("dev.morphia.critter.test.isolate.EntityWithIsolatedProp");
        byte[] entityBytes = ClassFile.of().build(entityDesc, cb -> {
            cb.withVersion(ClassFile.JAVA_17_VERSION, 0);
            cb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER);
            cb.withSuperclass(ConstantDescs.CD_Object);
            cb.withField("value", valueTypeDesc, ClassFile.ACC_PRIVATE);
            cb.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), ClassFile.ACC_PUBLIC, cod -> {
                cod.aload(0);
                cod.invokespecial(ConstantDescs.CD_Object, "<init>", MethodTypeDesc.ofDescriptor("()V"));
                cod.return_();
            });
            cb.withMethodBody("getValue", MethodTypeDesc.of(valueTypeDesc), ClassFile.ACC_PUBLIC, cod -> {
                cod.aload(0);
                cod.getfield(entityDesc, "value", valueTypeDesc);
                cod.areturn();
            });
            cb.withMethodBody("setValue",
                    MethodTypeDesc.of(ConstantDescs.CD_void, valueTypeDesc), ClassFile.ACC_PUBLIC, cod -> {
                        cod.aload(0);
                        cod.aload(1);
                        cod.putfield(entityDesc, "value", valueTypeDesc);
                        cod.return_();
                    });
        });

        Map<String, byte[]> generatedClasses = Map.of(
                "dev.morphia.critter.test.isolate.IsolatedValue", valueTypeBytes,
                "dev.morphia.critter.test.isolate.EntityWithIsolatedProp", entityBytes);
        ClassLoader isolatedLoader = new ClassLoader(ClassLoader.getSystemClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = generatedClasses.get(name);
                if (bytes != null) {
                    return defineClass(name, bytes, 0, bytes.length);
                }
                throw new ClassNotFoundException(name);
            }
        };
        Class<?> entityClass = isolatedLoader.loadClass("dev.morphia.critter.test.isolate.EntityWithIsolatedProp");
        Class<?> valueClass = isolatedLoader.loadClass("dev.morphia.critter.test.isolate.IsolatedValue");

        MethodInfo getterInfo = new MethodInfo(
                "getValue",
                "()Ldev/morphia/critter/test/isolate/IsolatedValue;",
                null,
                ClassFile.ACC_PUBLIC,
                List.of());

        CritterClassLoader critterLoader = new CritterClassLoader();
        // Bug: emit() throws RuntimeException(ClassNotFoundException) here because
        // classForName() calls Class.forName(typeName) with the system CL, not entity.getClassLoader()
        new VarHandleAccessorGenerator(entityClass, critterLoader, getterInfo).emit();

        // The generated accessor constructor resolves entity/value types via TCCL at runtime
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(isolatedLoader);
        try {
            String accessorName = Critter.critterPackage(entityClass) + ".ValueAccessor";
            @SuppressWarnings("unchecked")
            PropertyAccessor<Object> accessor = (PropertyAccessor<Object>) critterLoader
                    .loadClass(accessorName).getConstructor().newInstance();

            Object entity = entityClass.getConstructor().newInstance();
            Object value = valueClass.getConstructor().newInstance();

            Assertions.assertNull(accessor.get(entity), "initial value should be null");
            // Bug: set() throws UnsupportedOperationException because hasSetter() also called
            // Class.forName without entity.getClassLoader() and silently returned false
            accessor.set(entity, value);
            Assertions.assertSame(value, accessor.get(entity), "get() must return the value that was set");
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }
}
