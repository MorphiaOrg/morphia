package dev.morphia.critter.parser.generator;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.Map;

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
    private static final Map<String, String> PRIMITIVE_TO_WRAPPER = Map.of(
            "boolean", "java.lang.Boolean",
            "byte", "java.lang.Byte",
            "char", "java.lang.Character",
            "short", "java.lang.Short",
            "int", "java.lang.Integer",
            "long", "java.lang.Long",
            "float", "java.lang.Float",
            "double", "java.lang.Double");

    private final String propertyName;
    private final String propertyType;

    public PropertyAccessorGenerator(Class<?> entity, CritterClassLoader critterClassLoader, FieldInfo field) {
        super(entity, critterClassLoader);
        this.propertyName = field.name();
        this.propertyType = typeClassName(ClassDesc.ofDescriptor(field.desc()));
        generatedType = "%s.%sAccessor".formatted(baseName, Critter.titleCase(propertyName));
    }

    private static String typeClassName(ClassDesc cd) {
        String desc = cd.descriptorString();
        if (desc.length() == 1) {
            return switch (desc.charAt(0)) {
                case 'Z' -> "boolean";
                case 'C' -> "char";
                case 'B' -> "byte";
                case 'S' -> "short";
                case 'I' -> "int";
                case 'J' -> "long";
                case 'F' -> "float";
                case 'D' -> "double";
                default -> throw new IllegalArgumentException("Unknown primitive: " + desc);
            };
        }
        if (desc.startsWith("[")) {
            // Array: return Class.forName-compatible form, e.g. [Ljava.lang.String;
            return desc.replace('/', '.');
        }
        // Object type: Ljava/lang/String; → java.lang.String
        return desc.substring(1, desc.length() - 1).replace('/', '.');
    }

    public PropertyAccessorGenerator(Class<?> entity, CritterClassLoader critterClassLoader, MethodInfo method) {
        super(entity, critterClassLoader);
        this.propertyName = ExtensionFunctions.getterToPropertyName(method, entity);
        String returnDesc = MethodTypeDesc.ofDescriptor(method.desc()).returnType().descriptorString();
        this.propertyType = typeClassName(ClassDesc.ofDescriptor(returnDesc));
        generatedType = "%s.%sAccessor".formatted(baseName, Critter.titleCase(propertyName));
    }

    public boolean isPrimitive() {
        return PRIMITIVE_TO_WRAPPER.containsKey(propertyType);
    }

    public String getWrapperType() {
        return PRIMITIVE_TO_WRAPPER.getOrDefault(propertyType, propertyType);
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
            String propDesc = propertyType.startsWith("[")
                    ? propertyType.replace('.', '/')
                    : "L" + propertyType.replace('.', '/') + ";";
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
                            cod.invokevirtual(entityDesc, readName, MethodTypeDesc.of(propertyDesc));
                        }
                        cod.areturn();
                    });

            // set(Object model, Object value): void
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
                        } else {
                            cod.aload(2);
                            cod.checkcast(propertyDesc);
                        }
                        cod.invokevirtual(entityDesc, writeName,
                                MethodTypeDesc.of(ConstantDescs.CD_void, propertyDesc));
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
        return switch (propertyType) {
            case "boolean" -> "booleanValue";
            case "byte" -> "byteValue";
            case "char" -> "charValue";
            case "short" -> "shortValue";
            case "int" -> "intValue";
            case "long" -> "longValue";
            case "float" -> "floatValue";
            case "double" -> "doubleValue";
            default -> throw new IllegalArgumentException("Not a primitive: " + propertyType);
        };
    }
}
