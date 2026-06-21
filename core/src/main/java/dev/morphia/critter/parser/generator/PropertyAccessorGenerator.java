package dev.morphia.critter.parser.generator;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;

import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.ExtensionFunctions;
import dev.morphia.critter.parser.FieldInfo;
import dev.morphia.critter.parser.MethodInfo;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassSignature;
import io.github.dmlloyd.classfile.attribute.SignatureAttribute;

/**
 * Generates a {@link org.bson.codecs.pojo.PropertyAccessor} implementation for a single
 * entity property, delegating to the synthetic {@code __readXxx}/{@code __writeXxx} methods.
 */
public class PropertyAccessorGenerator extends BaseGenerator {
    private final String propertyName;
    private final String propertyType;

    public PropertyAccessorGenerator(Class<?> entity, CritterClassLoader critterClassLoader, FieldInfo field) {
        super(entity, critterClassLoader);
        this.propertyName = field.name();
        this.propertyType = GenerationUtils.typeClassName(ClassDesc.ofDescriptor(field.desc()));
        generatedType = "%s.%sAccessor".formatted(baseName, Critter.titleCase(propertyName));
    }

    public PropertyAccessorGenerator(Class<?> entity, CritterClassLoader critterClassLoader, MethodInfo method) {
        super(entity, critterClassLoader);
        this.propertyName = ExtensionFunctions.getterToPropertyName(method, entity);
        String returnDesc = MethodTypeDesc.ofDescriptor(method.desc()).returnType().descriptorString();
        this.propertyType = GenerationUtils.typeClassName(ClassDesc.ofDescriptor(returnDesc));
        generatedType = "%s.%sAccessor".formatted(baseName, Critter.titleCase(propertyName));
    }

    public boolean isPrimitive() {
        return GenerationUtils.PRIMITIVE_TO_WRAPPER.containsKey(propertyType);
    }

    public String getWrapperType() {
        return GenerationUtils.PRIMITIVE_TO_WRAPPER.getOrDefault(propertyType, propertyType);
    }

    public PropertyAccessorGenerator emit() {
        ClassDesc thisDesc = ClassDesc.of(generatedType);
        ClassDesc entityDesc = ClassDesc.of(entity.getName());
        ClassDesc propertyDesc;
        if (isPrimitive()) {
            propertyDesc = ClassDesc.ofDescriptor(primitiveDescriptor());
        } else if (propertyType.startsWith("[")) {
            propertyDesc = ClassDesc.ofDescriptor(propertyType.replace('.', '/'));
        } else {
            propertyDesc = ClassDesc.ofDescriptor("L" + propertyType.replace('.', '/') + ";");
        }
        ClassDesc wrapperDesc = ClassDesc.of(getWrapperType());
        ClassDesc accessorDesc = ClassDesc.of("org.bson.codecs.pojo.PropertyAccessor");

        byte[] bytes = ClassFile.of().build(thisDesc, cb -> {
            cb.withVersion(ClassFile.JAVA_17_VERSION, 0);
            cb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER);
            cb.withSuperclass(ConstantDescs.CD_Object);
            cb.withInterfaceSymbols(accessorDesc);

            // Class signature: Ljava/lang/Object;Lorg/bson/codecs/pojo/PropertyAccessor<propertyType>;
            // Primitives must use their wrapper type in generic signatures (e.g. "int" → "Ljava/lang/Integer;")
            String sigType = isPrimitive() ? getWrapperType() : propertyType;
            String propDesc = sigType.startsWith("[")
                    ? sigType.replace('.', '/')
                    : "L" + sigType.replace('.', '/') + ";";
            String sigStr = "Ljava/lang/Object;L"
                    + accessorDesc.descriptorString().substring(1, accessorDesc.descriptorString().length() - 1) + "<"
                    + propDesc + ">" + ";";
            cb.with(SignatureAttribute.of(ClassSignature.parseFrom(sigStr)));

            // default constructor
            cb.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), ClassFile.ACC_PUBLIC, cod -> {
                cod.aload(0);
                cod.invokespecial(ConstantDescs.CD_Object, "<init>", MethodTypeDesc.ofDescriptor("()V"));
                cod.return_();
            });

            // get(Object model): Object
            // __readXxx() returns Object for reference types (bridge cast lives inside entity)
            cb.withMethodBody("get", MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;)Ljava/lang/Object;"),
                    ClassFile.ACC_PUBLIC, cod -> {
                        cod.aload(1);
                        cod.checkcast(entityDesc);
                        String readName = "__read%s".formatted(Critter.titleCase(propertyName));
                        if (isPrimitive()) {
                            cod.invokevirtual(entityDesc, readName, MethodTypeDesc.of(propertyDesc));
                            // box the primitive
                            cod.invokestatic(wrapperDesc, "valueOf",
                                    MethodTypeDesc.of(wrapperDesc, propertyDesc));
                        } else {
                            cod.invokevirtual(entityDesc, readName, MethodTypeDesc.of(ConstantDescs.CD_Object));
                        }
                        cod.areturn();
                    });

            // set(Object model, Object value): void
            // __writeXxx(Object) for reference types — no non-public type in descriptor
            cb.withMethodBody("set", MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;Ljava/lang/Object;)V"),
                    ClassFile.ACC_PUBLIC, cod -> {
                        cod.aload(1);
                        cod.checkcast(entityDesc);
                        String writeName = "__write%s".formatted(Critter.titleCase(propertyName));
                        if (isPrimitive()) {
                            cod.aload(2);
                            cod.checkcast(wrapperDesc);
                            // unbox: WrapperType.primitiveValue()
                            String unboxMethod = primitiveUnboxMethod();
                            cod.invokevirtual(wrapperDesc, unboxMethod, MethodTypeDesc.of(propertyDesc));
                            cod.invokevirtual(entityDesc, writeName,
                                    MethodTypeDesc.of(ConstantDescs.CD_void, propertyDesc));
                        } else {
                            cod.aload(2);
                            cod.invokevirtual(entityDesc, writeName,
                                    MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object));
                        }
                        cod.return_();
                    });
        });

        critterClassLoader.register(generatedType, bytes);
        return this;
    }

    private String primitiveDescriptor() {
        return switch (propertyType) {
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            default -> throw new IllegalArgumentException("Not a primitive: " + propertyType);
        };
    }

    private String primitiveUnboxMethod() {
        return GenerationUtils.primitiveUnboxMethod(propertyType);
    }
}
