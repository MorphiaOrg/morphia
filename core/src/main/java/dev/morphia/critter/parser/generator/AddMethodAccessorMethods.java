package dev.morphia.critter.parser.generator;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Modifier;
import java.util.List;

import dev.morphia.critter.Critter;
import dev.morphia.critter.parser.ExtensionFunctions;
import dev.morphia.critter.parser.MethodInfo;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassModel;
import io.github.dmlloyd.classfile.ClassTransform;
import io.github.dmlloyd.classfile.MethodModel;
import io.github.dmlloyd.classfile.TypeKind;

/**
 * Generates synthetic {@code __readXxx} and {@code __writeXxx} accessor methods into an entity class
 * bytecode for properties backed by getter/setter methods rather than direct fields.
 */
public class AddMethodAccessorMethods extends AccessorMethods {
    private final List<MethodInfo> methods;

    /**
     * Creates a generator that will add accessor methods for the given getter methods to the entity class.
     */
    public AddMethodAccessorMethods(Class<?> entity, List<MethodInfo> methods) {
        super(entity);
        this.methods = methods;
    }

    @Override
    public byte[] emit() {
        ClassModel model = readClassFiltering();
        ClassDesc entityDesc = ClassDesc.of(entity.getName());

        ClassTransform transform = ClassTransform.dropping(
                element -> element instanceof MethodModel m
                        && (m.flags().flagsMask() & ClassFile.ACC_SYNTHETIC) != 0
                        && (m.methodName().stringValue().startsWith("__read")
                                || m.methodName().stringValue().startsWith("__write")))
                .andThen(ClassTransform.endHandler(classBuilder -> {
                    for (MethodInfo method : methods) {
                        String propertyName = ExtensionFunctions.getterToPropertyName(method, entity);
                        MethodTypeDesc methodMtd = MethodTypeDesc.ofDescriptor(method.desc());
                        ClassDesc returnDesc = methodMtd.returnType();
                        TypeKind returnKind = TypeKind.fromDescriptor(returnDesc.descriptorString());
                        String getterName = method.name();
                        String setterName = "set%s".formatted(Critter.titleCase(propertyName));

                        boolean hasSetter = findSetter(entity, setterName, methodMtd.returnType()) != null;

                        // __readXxx(): returns Object for reference types (keeps non-public types
                        // out of the accessor's constant pool; concrete type widens inside entity)
                        boolean isPrimitive = returnKind != TypeKind.REFERENCE;
                        String readerName = "__read%s".formatted(Critter.titleCase(propertyName));
                        MethodTypeDesc readerMtd = MethodTypeDesc.of(isPrimitive ? returnDesc : ConstantDescs.CD_Object);
                        classBuilder.withMethodBody(readerName, readerMtd,
                                ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC,
                                cod -> {
                                    cod.aload(0);
                                    cod.invokevirtual(entityDesc, getterName, MethodTypeDesc.of(returnDesc));
                                    cod.return_(returnKind);
                                });

                        // __writeXxx(Object): void for reference types (cast inside entity where concrete type is accessible)
                        String writerName = "__write%s".formatted(Critter.titleCase(propertyName));
                        MethodTypeDesc writerMtd = MethodTypeDesc.of(ClassDesc.ofDescriptor("V"),
                                isPrimitive ? returnDesc : ConstantDescs.CD_Object);
                        if (hasSetter) {
                            classBuilder.withMethodBody(writerName, writerMtd,
                                    ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC,
                                    cod -> {
                                        cod.aload(0);
                                        if (isPrimitive) {
                                            cod.loadLocal(returnKind, 1);
                                        } else {
                                            cod.aload(1);
                                            cod.checkcast(returnDesc);
                                        }
                                        cod.invokevirtual(entityDesc, setterName,
                                                MethodTypeDesc.of(ClassDesc.ofDescriptor("V"), returnDesc));
                                        cod.return_();
                                    });
                        } else {
                            classBuilder.withMethodBody(writerName, writerMtd,
                                    ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC,
                                    cod -> {
                                        ClassDesc uoeDesc = ClassDesc.of("java.lang.UnsupportedOperationException");
                                        cod.new_(uoeDesc);
                                        cod.dup();
                                        cod.ldc("Property '%s' is read-only".formatted(propertyName));
                                        cod.invokespecial(uoeDesc, "<init>",
                                                MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V"));
                                        cod.athrow();
                                    });
                        }
                    }
                }));

        return ClassFile.of().transformClass(model, transform);
    }

    private static java.lang.reflect.Method findSetter(Class<?> start, String name, ClassDesc paramDesc) {
        Class<?> paramType;
        try {
            String paramName = paramDesc.descriptorString();
            if (paramName.length() == 1) {
                paramType = switch (paramName) {
                    case "Z" -> boolean.class;
                    case "B" -> byte.class;
                    case "C" -> char.class;
                    case "S" -> short.class;
                    case "I" -> int.class;
                    case "J" -> long.class;
                    case "F" -> float.class;
                    case "D" -> double.class;
                    default -> throw new IllegalArgumentException("Unknown primitive: " + paramName);
                };
            } else {
                String className = paramDesc.packageName().isEmpty()
                        ? paramDesc.displayName()
                        : paramDesc.packageName() + "." + paramDesc.displayName();
                paramType = Class.forName(className, false, start.getClassLoader() != null
                        ? start.getClassLoader()
                        : ClassLoader.getSystemClassLoader());
            }
        } catch (ClassNotFoundException e) {
            return null;
        }
        Class<?> current = start;
        while (current != null && current != Object.class) {
            try {
                java.lang.reflect.Method m = current.getDeclaredMethod(name, paramType);
                if (!Modifier.isPrivate(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())) {
                    return m;
                }
            } catch (NoSuchMethodException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
