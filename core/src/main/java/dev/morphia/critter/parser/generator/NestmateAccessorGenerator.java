package dev.morphia.critter.parser.generator;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Field;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.critter.Critter;
import dev.morphia.critter.parser.FieldInfo;
import dev.morphia.critter.parser.MethodInfo;
import dev.morphia.mapping.conventions.DiscoveryRules;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassSignature;
import io.smallrye.classfile.TypeKind;
import io.smallrye.classfile.attribute.SignatureAttribute;

import static dev.morphia.critter.parser.generator.GenerationUtils.PRIMITIVE_TO_WRAPPER;
import static dev.morphia.critter.parser.generator.GenerationUtils.primitiveClassDesc;
import static dev.morphia.critter.parser.generator.GenerationUtils.primitiveUnboxMethod;
import static dev.morphia.critter.parser.generator.GenerationUtils.typeClassName;

/**
 * Generates a {@link org.bson.codecs.pojo.PropertyAccessor} implementation that uses direct
 * {@code getfield}/{@code putfield} (for fields) or {@code invokevirtual} (for getter/setter methods)
 * to access entity properties. The generated class must be defined as a hidden nestmate of the entity
 * class so the JVM grants it private-member access.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
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
    // the setter's type, or null for a field or a read-only property
    @Nullable
    private final MethodTypeDesc setterType;
    // registryKey: stable key used for NestmateAccessorRegistry (critter subpackage name)
    final String registryKey;
    // bytecodeName: class name embedded in the bytecode; MUST be in the declaringClass's package for defineHiddenClass
    private final String bytecodeName;

    public NestmateAccessorGenerator(Class<?> entity, FieldInfo field) {
        this.entity = entity;
        this.declaringClass = fieldHost(entity, field);
        this.propertyName = field.name();
        this.propertyType = typeClassName(ClassDesc.ofDescriptor(field.desc()));
        this.isFieldBased = true;
        this.isFinalField = (field.access() & ClassFile.ACC_FINAL) != 0;
        this.fieldOrGetterName = field.name();
        this.setterName = null;
        this.setterType = null;
        this.registryKey = "%s.%sAccessor".formatted(Critter.critterPackage(entity), DiscoveryRules.titleCase(propertyName));
        this.bytecodeName = entityPackagePrefix(declaringClass) + declaringClass.getSimpleName() + "$$"
                + DiscoveryRules.titleCase(propertyName)
                + "Accessor";
    }

    public NestmateAccessorGenerator(Class<?> entity, MethodInfo method) {
        this.entity = entity;
        this.declaringClass = accessorHost(entity, method);
        this.propertyName = DiscoveryRules.propertyName(method.name());
        String returnDesc = MethodTypeDesc.ofDescriptor(method.desc()).returnType().descriptorString();
        this.propertyType = typeClassName(ClassDesc.ofDescriptor(returnDesc));
        this.isFieldBased = false;
        this.isFinalField = false;
        this.fieldOrGetterName = method.name();
        this.setterName = "set%s".formatted(DiscoveryRules.titleCase(propertyName));
        this.setterType = GenerationUtils.setterType(entity, method, setterName, propertyClassDesc());
        this.registryKey = "%s.%sAccessor".formatted(Critter.critterPackage(entity), DiscoveryRules.titleCase(propertyName));
        this.bytecodeName = entityPackagePrefix(declaringClass) + declaringClass.getSimpleName() + "$$"
                + DiscoveryRules.titleCase(propertyName) + "Accessor";
    }

    /**
     * @return the class whose nest the accessor joins: the entity's, unless the pair is a superclass's and one of its
     *         methods isn't public, which only a nestmate of that superclass can call. Joining a superclass's nest needs
     *         access to its module, which a public pair, such as one inherited from the JDK, mustn't depend on. An
     *         {@code @ExternalEntity} stand-in's pairs are declared outside the target's hierarchy, and the target's own
     *         methods are called.
     */
    private static Class<?> accessorHost(Class<?> entity, MethodInfo getter) {
        Class<?> declaring = getter.declaringClass();
        if (declaring == null || declaring == entity || !declaring.isAssignableFrom(entity)) {
            return entity;
        }
        boolean isPublic = (getter.access() & ClassFile.ACC_PUBLIC) != 0
                && getter.setter() != null && (getter.setter().access() & ClassFile.ACC_PUBLIC) != 0;
        return isPublic ? entity : declaring;
    }

    /**
     * @return the class declaring the field in {@code entity}'s hierarchy. An {@code @ExternalEntity} stand-in's field is
     *         declared outside the target's hierarchy, and the target's field of the same name is used.
     */
    private static Class<?> fieldHost(Class<?> entity, FieldInfo field) {
        Class<?> declaring = field.declaringClass();
        Field found = GenerationUtils.findField(entity,
                declaring != null && declaring.isAssignableFrom(entity) ? declaring : null, field.name());
        return found != null ? found.getDeclaringClass() : entity;
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
     * Generates the accessor bytecode. An array property's values are converted to its type before they reach it: see
     * {@link dev.morphia.mapping.codec.ArrayAccessor}.
     */
    public byte[] generate() {

        ClassDesc thisDesc = ClassDesc.of(bytecodeName);
        // the class declaring the field or the getter and setter, which may be a superclass of the entity
        ClassDesc ownerDesc = ClassDesc.of(declaringClass.getName());
        ClassDesc propertyDesc = propertyClassDesc();
        ClassDesc wrapperDesc = isPrimitive() ? ClassDesc.of(getWrapperType()) : propertyDesc;
        ClassDesc accessorDesc = ClassDesc.of("org.bson.codecs.pojo.PropertyAccessor");

        boolean useSetterHandle = !isFieldBased && setterType != null;

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

            ClassDesc reflectFieldDesc = ClassDesc.of("java.lang.reflect.Field");
            if (isFinalField) {
                cb.withField("finalField", reflectFieldDesc, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL);
            }

            // no-arg constructor
            cb.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), ClassFile.ACC_PUBLIC, cod -> {
                cod.aload(0);
                cod.invokespecial(ConstantDescs.CD_Object, "<init>", MethodTypeDesc.ofDescriptor("()V"));
                if (isFinalField) {
                    // putfield cannot write to final fields outside <init>, so set() uses reflection. Look the
                    // field up once here rather than on every write.
                    cod.aload(0);
                    cod.ldc(ownerDesc);
                    cod.ldc(fieldOrGetterName);
                    cod.invokestatic(ClassDesc.of("dev.morphia.critter.Critter"), "accessibleField",
                            MethodTypeDesc.of(reflectFieldDesc, ConstantDescs.CD_Class, ConstantDescs.CD_String));
                    cod.putfield(thisDesc, "finalField", reflectFieldDesc);
                }
                cod.return_();
            });

            // get(Object model): Object
            cb.withMethodBody("get", MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;)Ljava/lang/Object;"),
                    ClassFile.ACC_PUBLIC, cod -> {
                        cod.aload(1);
                        // Cast to the declaring class (not the leaf entity): the nestmate is defined in the declaring
                        // class's nest, so it can reference the declaring class but may not be able to reference a
                        // private nested leaf class.
                        cod.checkcast(ownerDesc);
                        if (isFieldBased) {
                            cod.getfield(ownerDesc, fieldOrGetterName, propertyDesc);
                        } else {
                            cod.invokevirtual(ownerDesc, fieldOrGetterName, MethodTypeDesc.of(propertyDesc));
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
                            ClassDesc rteDesc = ClassDesc.of("java.lang.RuntimeException");
                            cod.trying(tryBody -> {
                                tryBody.aload(0);
                                tryBody.getfield(thisDesc, "finalField", reflectFieldDesc);
                                tryBody.aload(1);
                                tryBody.aload(2);
                                tryBody.invokevirtual(reflectFieldDesc, "set",
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
                        cod.checkcast(ownerDesc);
                        if (isPrimitive()) {
                            cod.aload(2);
                            cod.checkcast(wrapperDesc);
                            cod.invokevirtual(wrapperDesc, primitiveUnboxMethod(propertyType), MethodTypeDesc.of(propertyDesc));
                        } else {
                            cod.aload(2);
                            cod.checkcast(propertyDesc);
                        }

                        if (isFieldBased) {
                            cod.putfield(ownerDesc, fieldOrGetterName, propertyDesc);
                        } else {
                            cod.invokevirtual(ownerDesc, setterName, setterType);
                            // a fluent setter's result
                            if (!setterType.returnType().equals(ConstantDescs.CD_void)) {
                                if (TypeKind.from(setterType.returnType()).slotSize() == 2) {
                                    cod.pop2();
                                } else {
                                    cod.pop();
                                }
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
