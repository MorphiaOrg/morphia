package dev.morphia.critter.parser.generator;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import dev.morphia.critter.parser.MethodInfo;
import dev.morphia.mapping.conventions.DiscoveryRules;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.ClassTransform;
import io.smallrye.classfile.MethodModel;
import io.smallrye.classfile.TypeKind;

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

    /**
     * Creates a generator that will add accessor methods for the given getter methods to the entity class's parsed classfile.
     */
    public AddMethodAccessorMethods(Class<?> entity, ClassModel model, List<MethodInfo> methods) {
        super(entity, model);
        this.methods = methods;
    }

    @Override
    public byte[] emit() {
        ClassDesc entityDesc = ClassDesc.of(entity.getName());

        ClassTransform transform = ClassTransform.dropping(
                element -> element instanceof MethodModel m
                        && (m.flags().flagsMask() & ClassFile.ACC_SYNTHETIC) != 0
                        && (m.methodName().stringValue().startsWith("__read")
                                || m.methodName().stringValue().startsWith("__write")))
                .andThen(ClassTransform.endHandler(classBuilder -> {
                    for (MethodInfo method : methods) {
                        String propertyName = DiscoveryRules.propertyName(method.name());
                        MethodTypeDesc methodMtd = MethodTypeDesc.ofDescriptor(method.desc());
                        ClassDesc returnDesc = methodMtd.returnType();
                        TypeKind returnKind = TypeKind.fromDescriptor(returnDesc.descriptorString());
                        String getterName = method.name();
                        String setterName = "set%s".formatted(DiscoveryRules.titleCase(propertyName));
                        MethodTypeDesc setterMtd = GenerationUtils.setterType(entity, method, setterName, returnDesc);
                        boolean hasSetter = setterMtd != null;

                        // __readXxx(): returns Object for reference types (keeps non-public types
                        // out of the accessor's constant pool; concrete type widens inside entity)
                        boolean isPrimitive = returnKind != TypeKind.REFERENCE;
                        String readerName = "__read%s".formatted(DiscoveryRules.titleCase(propertyName));
                        MethodTypeDesc readerMtd = MethodTypeDesc.of(isPrimitive ? returnDesc : ConstantDescs.CD_Object);
                        classBuilder.withMethodBody(readerName, readerMtd,
                                ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC,
                                cod -> {
                                    cod.aload(0);
                                    cod.invokevirtual(entityDesc, getterName, MethodTypeDesc.of(returnDesc));
                                    cod.return_(returnKind);
                                });

                        // __writeXxx(Object): void for reference types (cast inside entity where concrete type is accessible)
                        String writerName = "__write%s".formatted(DiscoveryRules.titleCase(propertyName));
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
                                        cod.invokevirtual(entityDesc, setterName, setterMtd);
                                        // a fluent setter's result
                                        if (!setterMtd.returnType().equals(ConstantDescs.CD_void)) {
                                            if (TypeKind.from(setterMtd.returnType()).slotSize() == 2) {
                                                cod.pop2();
                                            } else {
                                                cod.pop();
                                            }
                                        }
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

}
