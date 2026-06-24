package dev.morphia.critter.parser.generator;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Method;

import dev.morphia.critter.Critter;
import dev.morphia.critter.parser.ExtensionFunctions;
import dev.morphia.critter.parser.FieldInfo;
import dev.morphia.critter.parser.MethodInfo;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassSignature;
import io.github.dmlloyd.classfile.attribute.SignatureAttribute;

import static dev.morphia.critter.parser.generator.GenerationUtils.PRIMITIVE_TO_WRAPPER;
import static dev.morphia.critter.parser.generator.GenerationUtils.findSetterMethod;
import static dev.morphia.critter.parser.generator.GenerationUtils.primitiveClassDesc;
import static dev.morphia.critter.parser.generator.GenerationUtils.primitiveUnboxMethod;
import static dev.morphia.critter.parser.generator.GenerationUtils.typeClassName;

/**
 * Generates a {@link org.bson.codecs.pojo.PropertyAccessor} implementation that uses direct
 * {@code getfield}/{@code putfield} (for fields) or {@code invokevirtual} (for getter/setter methods)
 * to access entity properties. The generated class must be defined as a hidden nestmate of the entity
 * class so the JVM grants it private-member access.
 *
 * <p>
 * Array-typed properties are not supported: the BSON codec provides {@code Object[]} at runtime,
 * which is incompatible with {@code putfield} to a typed array field. Callers should throw
 * to trigger {@link dev.morphia.mapping.CritterMapper} fallback to reflection for such entities.
 */
public class NestmateAccessorGenerator {
    private final Class<?> entity;
    // declaringClass: the class that actually declares the field (may be a superclass of entity)
    final Class<?> declaringClass;
    private final String propertyName;
    private final String propertyType;
    private final boolean isFieldBased;
    private final boolean isFinalField;
    private final String fieldOrGetterName;
    private final String setterName;
    private final Method setterMethod;
    // registryKey: stable key used for NestmateAccessorRegistry (critter subpackage name)
    final String registryKey;
    // bytecodeName: class name embedded in the bytecode; MUST be in the declaringClass's package for defineHiddenClass
    private final String bytecodeName;

    public NestmateAccessorGenerator(Class<?> entity, FieldInfo field) {
        this.entity = entity;
        this.declaringClass = findDeclaringClassInHierarchy(entity, field.name());
        this.propertyName = field.name();
        this.propertyType = typeClassName(ClassDesc.ofDescriptor(field.desc()));
        this.isFieldBased = true;
        this.isFinalField = (field.access() & ClassFile.ACC_FINAL) != 0;
        this.fieldOrGetterName = field.name();
        this.setterName = null;
        this.setterMethod = null;
        this.registryKey = "%s.%sAccessor".formatted(Critter.critterPackage(entity), Critter.titleCase(propertyName));
        this.bytecodeName = entityPackagePrefix(declaringClass) + declaringClass.getSimpleName() + "$$" + Critter.titleCase(propertyName)
                + "Accessor";
    }

    public NestmateAccessorGenerator(Class<?> entity, MethodInfo method) {
        this.entity = entity;
        this.declaringClass = entity;
        this.propertyName = ExtensionFunctions.getterToPropertyName(method, entity);
        String returnDesc = MethodTypeDesc.ofDescriptor(method.desc()).returnType().descriptorString();
        this.propertyType = typeClassName(ClassDesc.ofDescriptor(returnDesc));
        this.isFieldBased = false;
        this.isFinalField = false;
        this.fieldOrGetterName = method.name();
        this.setterName = "set%s".formatted(Critter.titleCase(propertyName));
        this.setterMethod = findSetterMethod(entity, this.setterName, propertyClassDesc());
        this.registryKey = "%s.%sAccessor".formatted(Critter.critterPackage(entity), Critter.titleCase(propertyName));
        this.bytecodeName = entityPackagePrefix(entity) + entity.getSimpleName() + "$$" + Critter.titleCase(propertyName) + "Accessor";
    }

    private static Class<?> findDeclaringClassInHierarchy(Class<?> entity, String fieldName) {
        Class<?> current = entity;
        while (current != null && current != Object.class) {
            try {
                current.getDeclaredField(fieldName);
                return current;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return entity;
    }

    private static String entityPackagePrefix(Class<?> entity) {
        String pkg = entity.getPackageName();
        return pkg.isEmpty() ? "" : pkg + ".";
    }

    boolean isPrimitive() {
        return PRIMITIVE_TO_WRAPPER.containsKey(propertyType);
    }

    String getWrapperType() {
        return PRIMITIVE_TO_WRAPPER.getOrDefault(propertyType, propertyType);
    }

    /**
     * Generates the accessor bytecode.
     *
     * @throws IllegalArgumentException if the property type is an array (BSON codec provides {@code Object[]}
     *                                  which is incompatible with typed array putfield at runtime)
     */
    public byte[] generate() {
        if (propertyType.startsWith("[")) {
            throw new IllegalArgumentException(
                    "Array-typed properties cannot use nestmate accessors (codec provides Object[]): " + propertyName);
        }

        ClassDesc thisDesc = ClassDesc.of(bytecodeName);
        ClassDesc entityDesc = ClassDesc.of(entity.getName());
        // For field access, use the declaring class (may differ from entity when field is in a superclass)
        ClassDesc fieldOwnerDesc = ClassDesc.of(declaringClass.getName());
        ClassDesc propertyDesc = propertyClassDesc();
        ClassDesc wrapperDesc = ClassDesc.of(getWrapperType());
        ClassDesc accessorDesc = ClassDesc.of("org.bson.codecs.pojo.PropertyAccessor");

        boolean useSetterHandle = !isFieldBased && setterMethod != null;

        return ClassFile.of().build(thisDesc, cb -> {
            cb.withVersion(ClassFile.JAVA_17_VERSION, 0);
            cb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER);
            cb.withSuperclass(ConstantDescs.CD_Object);
            cb.withInterfaceSymbols(accessorDesc);

            String sigType = isPrimitive() ? getWrapperType() : propertyType;
            String propDesc = sigType.startsWith("[")
                    ? sigType.replace('.', '/')
                    : "L" + sigType.replace('.', '/') + ";";
            String sigStr = "Ljava/lang/Object;L"
                    + accessorDesc.descriptorString().substring(1, accessorDesc.descriptorString().length() - 1)
                    + "<" + propDesc + ">" + ";";
            cb.with(SignatureAttribute.of(ClassSignature.parseFrom(sigStr)));

            // no-arg constructor
            cb.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), ClassFile.ACC_PUBLIC, cod -> {
                cod.aload(0);
                cod.invokespecial(ConstantDescs.CD_Object, "<init>", MethodTypeDesc.ofDescriptor("()V"));
                cod.return_();
            });

            // get(Object model): Object
            cb.withMethodBody("get", MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;)Ljava/lang/Object;"),
                    ClassFile.ACC_PUBLIC, cod -> {
                        cod.aload(1);
                        // For field-based access, cast to the declaring class (not the leaf entity):
                        // the nestmate is defined in the declaring class's nest, so it can reference
                        // the declaring class but may not be able to reference a private nested leaf class.
                        cod.checkcast(isFieldBased ? fieldOwnerDesc : entityDesc);
                        if (isFieldBased) {
                            cod.getfield(fieldOwnerDesc, fieldOrGetterName, propertyDesc);
                        } else {
                            MethodTypeDesc getterMtd = MethodTypeDesc.of(propertyDesc);
                            cod.invokevirtual(entityDesc, fieldOrGetterName, getterMtd);
                        }
                        if (isPrimitive()) {
                            cod.invokestatic(wrapperDesc, "valueOf", MethodTypeDesc.of(wrapperDesc, propertyDesc));
                        }
                        cod.areturn();
                    });

            // set(Object model, Object value): void
            cb.withMethodBody("set", MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;Ljava/lang/Object;)V"),
                    ClassFile.ACC_PUBLIC, cod -> {
                        if (!isFieldBased && !useSetterHandle) {
                            cod.new_(ClassDesc.of("java.lang.UnsupportedOperationException"));
                            cod.dup();
                            cod.ldc("Property '%s' is read-only".formatted(propertyName));
                            cod.invokespecial(ClassDesc.of("java.lang.UnsupportedOperationException"), "<init>",
                                    MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V"));
                            cod.athrow();
                            return;
                        }

                        if (isFinalField) {
                            // putfield cannot write to final fields outside <init>; use reflection
                            ClassDesc fieldDesc2 = ClassDesc.of("java.lang.reflect.Field");
                            ClassDesc rteDesc = ClassDesc.of("java.lang.RuntimeException");
                            cod.trying(tryBody -> {
                                GenerationUtils.emitClassRef(tryBody, declaringClass);
                                tryBody.ldc(fieldOrGetterName);
                                tryBody.invokevirtual(ConstantDescs.CD_Class, "getDeclaredField",
                                        MethodTypeDesc.of(fieldDesc2, ConstantDescs.CD_String));
                                int fieldSlot = tryBody.allocateLocal(io.github.dmlloyd.classfile.TypeKind.REFERENCE);
                                tryBody.astore(fieldSlot);
                                tryBody.aload(fieldSlot);
                                tryBody.iconst_1();
                                tryBody.invokevirtual(fieldDesc2, "setAccessible",
                                        MethodTypeDesc.ofDescriptor("(Z)V"));
                                tryBody.aload(fieldSlot);
                                tryBody.aload(1);
                                tryBody.aload(2);
                                tryBody.invokevirtual(fieldDesc2, "set",
                                        MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;Ljava/lang/Object;)V"));
                                tryBody.return_();
                            }, catches -> catches.catching(ClassDesc.of("java.lang.Exception"), catchBody -> {
                                catchBody.astore(3);
                                catchBody.new_(rteDesc);
                                catchBody.dup();
                                catchBody.ldc("Failed to set final field '%s'".formatted(fieldOrGetterName));
                                catchBody.aload(3);
                                catchBody.invokespecial(rteDesc, "<init>",
                                        MethodTypeDesc.ofDescriptor("(Ljava/lang/String;Ljava/lang/Throwable;)V"));
                                catchBody.athrow();
                            }));
                            return;
                        }

                        cod.aload(1);
                        cod.checkcast(isFieldBased ? fieldOwnerDesc : entityDesc);
                        if (isPrimitive()) {
                            cod.aload(2);
                            cod.checkcast(wrapperDesc);
                            cod.invokevirtual(wrapperDesc, primitiveUnboxMethod(propertyType), MethodTypeDesc.of(propertyDesc));
                        } else {
                            cod.aload(2);
                            cod.checkcast(propertyDesc);
                        }

                        if (isFieldBased) {
                            cod.putfield(fieldOwnerDesc, fieldOrGetterName, propertyDesc);
                        } else {
                            Class<?> retType = setterMethod.getReturnType();
                            ClassDesc retDesc = retType == void.class ? ConstantDescs.CD_void
                                    : retType.isPrimitive() ? primitiveClassDesc(retType.getName())
                                            : ClassDesc.of(retType.getName());
                            cod.invokevirtual(entityDesc, setterName, MethodTypeDesc.of(retDesc, propertyDesc));
                            if (retType != void.class) {
                                cod.pop();
                            }
                        }
                        cod.return_();
                    });
        });
    }

    private ClassDesc propertyClassDesc() {
        if (isPrimitive()) {
            return primitiveClassDesc(propertyType);
        }
        if (propertyType.startsWith("[")) {
            return ClassDesc.ofDescriptor(propertyType.replace('.', '/'));
        }
        return ClassDesc.of(propertyType);
    }
}
