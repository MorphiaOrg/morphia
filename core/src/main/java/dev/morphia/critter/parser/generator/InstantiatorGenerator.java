package dev.morphia.critter.parser.generator;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.internal.Instantiator;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.ClassTransform;
import io.smallrye.classfile.CodeBuilder;
import io.smallrye.classfile.MethodModel;

import static dev.morphia.critter.parser.generator.GenerationUtils.PRIMITIVE_TO_WRAPPER;
import static dev.morphia.critter.parser.generator.GenerationUtils.primitiveClassDesc;
import static dev.morphia.critter.parser.generator.GenerationUtils.primitiveUnboxMethod;

/**
 * Generates an {@link Instantiator} calling one of an entity's constructors. At runtime it's a hidden nestmate of the entity,
 * so it can call a constructor of any visibility. At build time it calls a synthetic static {@code __newN} method woven into
 * the entity, which calls the constructor, so neither the constructor nor its parameter types need to be accessible from the
 * generated class.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class InstantiatorGenerator extends BaseGenerator {
    private static final ClassDesc CD_INSTANTIATOR = ClassDesc.of(Instantiator.class.getName());
    private static final MethodTypeDesc NEW_INSTANCE = MethodTypeDesc.of(ConstantDescs.CD_Object,
            ConstantDescs.CD_Object.arrayType());
    private static final String WOVEN_PREFIX = "__new";

    private final Constructor<?> constructor;
    private final int index;
    private final boolean runtimeMode;
    // the key the runtime instantiator is registered with in NestmateAccessorRegistry
    final String registryKey;

    /**
     * @param entity             the entity
     * @param critterClassLoader the loader that receives the generated classes
     * @param constructor        the constructor to call
     * @param index              names the generated class and, at build time, is the constructor's index among
     *                           {@link #wovenConstructors}, naming the woven method it calls
     * @param runtimeMode        {@code true} to generate a hidden nestmate for runtime use
     */
    public InstantiatorGenerator(Class<?> entity, CritterClassLoader critterClassLoader, Constructor<?> constructor, int index,
            boolean runtimeMode) {
        super(entity, critterClassLoader);
        this.constructor = constructor;
        this.index = index;
        this.runtimeMode = runtimeMode;
        generatedType = "%s.Instantiator%d".formatted(baseName, index);
        registryKey = generatedType;
    }

    /**
     * @return the descriptor of the constructor the instantiator calls
     */
    public String constructorDescriptor() {
        return descriptor(constructor);
    }

    /**
     * @param constructor the constructor
     * @return its descriptor, which {@link dev.morphia.mapping.codec.pojo.EntityModel#getInstantiator} looks instantiators up by
     */
    public static String descriptor(Constructor<?> constructor) {
        return MethodType.methodType(void.class, constructor.getParameterTypes()).toMethodDescriptorString();
    }

    /**
     * @return whether the instantiator is a hidden nestmate, registered in {@link NestmateAccessorRegistry}
     */
    public boolean isRuntime() {
        return runtimeMode;
    }

    /**
     * Generates the instantiator. A runtime one is named in the entity's package, as {@code defineHiddenClass} requires.
     *
     * @return the class's bytecode
     */
    public byte[] generate() {
        String pkg = entity.getPackageName();
        ClassDesc thisDesc = ClassDesc.of(runtimeMode
                ? (pkg.isEmpty() ? "" : pkg + ".") + entity.getSimpleName() + "$$Instantiator" + index
                : generatedType);
        ClassDesc entityDesc = ClassDesc.of(entity.getName());
        return ClassFile.of().build(thisDesc, cb -> {
            cb.withVersion(ClassFile.JAVA_17_VERSION, 0);
            cb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER | ClassFile.ACC_FINAL);
            cb.withSuperclass(ConstantDescs.CD_Object);
            cb.withInterfaceSymbols(CD_INSTANTIATOR);
            cb.withMethodBody("<init>", MethodTypeDesc.of(ConstantDescs.CD_void), ClassFile.ACC_PUBLIC, cod -> {
                cod.aload(0);
                cod.invokespecial(ConstantDescs.CD_Object, "<init>", MethodTypeDesc.of(ConstantDescs.CD_void));
                cod.return_();
            });
            cb.withMethodBody("newInstance", NEW_INSTANCE, ClassFile.ACC_PUBLIC, cod -> {
                if (runtimeMode) {
                    construct(cod, entityDesc, MethodTypeDesc.ofDescriptor(descriptor(constructor)), 1);
                } else {
                    cod.aload(1);
                    cod.invokestatic(entityDesc, WOVEN_PREFIX + index, NEW_INSTANCE);
                }
                cod.areturn();
            });
        });
    }

    /**
     * @param model a classfile
     * @return the descriptors of the constructors a woven class gets a synthetic static {@code __newN(Object[])} method for,
     *         {@code N} being the constructor's index here: every constructor of a concrete class, in classfile order
     */
    public static List<String> wovenConstructors(ClassModel model) {
        if ((model.flags().flagsMask() & (ClassFile.ACC_ABSTRACT | ClassFile.ACC_INTERFACE | ClassFile.ACC_ENUM)) != 0) {
            return List.of();
        }
        return model.methods().stream()
                .filter(m -> m.methodName().stringValue().equals("<init>") && (m.flags().flagsMask() & ClassFile.ACC_SYNTHETIC) == 0)
                .map(m -> m.methodType().stringValue())
                .toList();
    }

    /**
     * Weaves a synthetic static {@code __newN(Object[])} method calling each of {@link #wovenConstructors}, replacing any woven
     * before. Every woven class gets them, whichever entity it's woven for, so that a superclass is woven the same way for each
     * of its subclasses.
     *
     * @param model the classfile being woven
     * @return the transform
     */
    public static ClassTransform weaving(ClassModel model) {
        ClassDesc owner = model.thisClass().asSymbol();
        List<String> constructors = wovenConstructors(model);
        return ClassTransform.dropping(element -> element instanceof MethodModel m
                && (m.flags().flagsMask() & ClassFile.ACC_SYNTHETIC) != 0
                && m.methodName().stringValue().startsWith(WOVEN_PREFIX))
                .andThen(ClassTransform.endHandler(cb -> {
                    for (int i = 0; i < constructors.size(); i++) {
                        MethodTypeDesc constructor = MethodTypeDesc.ofDescriptor(constructors.get(i));
                        cb.withMethodBody(WOVEN_PREFIX + i, NEW_INSTANCE,
                                ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC | ClassFile.ACC_SYNTHETIC, cod -> {
                                    construct(cod, owner, constructor, 0);
                                    cod.areturn();
                                });
                    }
                }));
    }

    /**
     * Emits {@code new Owner(args...)}, unboxing a primitive parameter's argument and casting any other.
     */
    private static void construct(CodeBuilder cod, ClassDesc owner, MethodTypeDesc constructor, int argumentsSlot) {
        cod.new_(owner);
        cod.dup();
        for (int i = 0; i < constructor.parameterCount(); i++) {
            ClassDesc parameter = constructor.parameterType(i);
            cod.aload(argumentsSlot);
            cod.loadConstant(i);
            cod.aaload();
            if (parameter.isPrimitive()) {
                String name = parameter.displayName();
                ClassDesc wrapper = ClassDesc.of(PRIMITIVE_TO_WRAPPER.get(name));
                cod.checkcast(wrapper);
                cod.invokevirtual(wrapper, primitiveUnboxMethod(name), MethodTypeDesc.of(primitiveClassDesc(name)));
            } else if (!parameter.equals(ConstantDescs.CD_Object)) {
                cod.checkcast(parameter);
            }
        }
        cod.invokespecial(owner, "<init>", constructor);
    }

    /**
     * @param entity the entity
     * @return whether a build-time instantiator, in the entity's critter package, can call the entity's woven methods
     */
    public static boolean isAccessible(Class<?> entity) {
        for (Class<?> type = entity; type != null; type = type.getEnclosingClass()) {
            if (!Modifier.isPublic(type.getModifiers())) {
                return false;
            }
        }
        return true;
    }
}
