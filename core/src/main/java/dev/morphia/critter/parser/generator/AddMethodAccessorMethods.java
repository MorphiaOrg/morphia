package dev.morphia.critter.parser.generator;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
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

                        boolean hasSetter = false;
                        for (java.lang.reflect.Method m : entity.getMethods()) {
                            if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                                hasSetter = true;
                                break;
                            }
                        }

                        // __readXxx(): return type of getter
                        String readerName = "__read%s".formatted(Critter.titleCase(propertyName));
                        MethodTypeDesc readerMtd = MethodTypeDesc.of(returnDesc);
                        classBuilder.withMethodBody(readerName, readerMtd,
                                ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC,
                                cod -> {
                                    cod.aload(0);
                                    cod.invokevirtual(entityDesc, getterName, MethodTypeDesc.of(returnDesc));
                                    cod.return_(returnKind);
                                });

                        // __writeXxx(T): void
                        String writerName = "__write%s".formatted(Critter.titleCase(propertyName));
                        MethodTypeDesc writerMtd = MethodTypeDesc.of(ClassDesc.ofDescriptor("V"), returnDesc);
                        if (hasSetter) {
                            classBuilder.withMethodBody(writerName, writerMtd,
                                    ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC,
                                    cod -> {
                                        cod.aload(0);
                                        cod.loadLocal(returnKind, 1);
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
}
