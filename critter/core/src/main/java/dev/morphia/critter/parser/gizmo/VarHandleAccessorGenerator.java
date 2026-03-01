package dev.morphia.critter.parser.gizmo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Modifier;
import java.util.Map;

import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.ExtensionFunctions;

import org.bson.codecs.pojo.PropertyAccessor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.gizmo.SignatureBuilder.forClass;
import static io.quarkus.gizmo.SignatureBuilder.forMethod;
import static io.quarkus.gizmo.Type.classType;
import static io.quarkus.gizmo.Type.parameterizedType;
import static io.quarkus.gizmo.Type.typeVariable;
import static io.quarkus.gizmo.Type.voidType;

public class VarHandleAccessorGenerator extends BaseGizmoGenerator {
    private static final Map<String, String> PRIMITIVE_TO_WRAPPER = Map.of(
            "boolean", "java.lang.Boolean",
            "byte", "java.lang.Byte",
            "char", "java.lang.Character",
            "short", "java.lang.Short",
            "int", "java.lang.Integer",
            "long", "java.lang.Long",
            "float", "java.lang.Float",
            "double", "java.lang.Double");

    private static final Map<String, Class<?>> PRIMITIVE_CLASSES = Map.of(
            "boolean", boolean.class,
            "byte", byte.class,
            "char", char.class,
            "short", short.class,
            "int", int.class,
            "long", long.class,
            "float", float.class,
            "double", double.class);

    private final String propertyName;
    private final String propertyType;
    private final boolean isFieldBased;
    private final String getterName;
    private final String setterName;

    public VarHandleAccessorGenerator(Class<?> entity, CritterClassLoader critterClassLoader, FieldNode field) {
        super(entity, critterClassLoader);
        this.propertyName = field.name;
        this.propertyType = Type.getType(field.desc).getClassName();
        this.isFieldBased = true;
        this.getterName = null;
        this.setterName = null;
        generatedType = "%s.%sAccessor".formatted(baseName, Critter.titleCase(propertyName));
    }

    public VarHandleAccessorGenerator(Class<?> entity, CritterClassLoader critterClassLoader, MethodNode method) {
        super(entity, critterClassLoader);
        this.propertyName = ExtensionFunctions.getterToPropertyName(method, entity);
        this.propertyType = Type.getReturnType(method.desc).getClassName();
        this.isFieldBased = false;
        this.getterName = method.name;
        this.setterName = "set%s".formatted(Critter.titleCase(propertyName));
        generatedType = "%s.%sAccessor".formatted(baseName, Critter.titleCase(propertyName));
    }

    public boolean isPrimitive() {
        return PRIMITIVE_TO_WRAPPER.containsKey(propertyType);
    }

    public String getWrapperType() {
        return PRIMITIVE_TO_WRAPPER.getOrDefault(propertyType, propertyType);
    }

    private boolean hasSetter() {
        if (isFieldBased || setterName == null)
            return false;
        Class<?> paramClass = isPrimitive() ? PRIMITIVE_CLASSES.get(propertyType) : null;
        if (paramClass == null) {
            try {
                paramClass = Class.forName(propertyType);
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        try {
            entity.getDeclaredMethod(setterName, paramClass);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public VarHandleAccessorGenerator emit() {
        getBuilder().signature(
                forClass()
                        .addInterface(
                                parameterizedType(
                                        classType(PropertyAccessor.class),
                                        classType(propertyType))));

        FieldDescriptor handleDesc;
        FieldDescriptor setterHandleDesc = null;

        if (isFieldBased) {
            FieldCreator varHandleCreator = getCreator().getFieldCreator("varHandle", VarHandle.class);
            varHandleCreator.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            handleDesc = varHandleCreator.getFieldDescriptor();
        } else {
            FieldCreator getterHandleCreator = getCreator().getFieldCreator("getterHandle", MethodHandle.class);
            getterHandleCreator.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            handleDesc = getterHandleCreator.getFieldDescriptor();
            if (hasSetter()) {
                FieldCreator setterHandleCreator = getCreator().getFieldCreator("setterHandle", MethodHandle.class);
                setterHandleCreator.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                setterHandleDesc = setterHandleCreator.getFieldDescriptor();
            }
        }

        try (var creator = getCreator()) {
            ctor(handleDesc, setterHandleDesc);
            get(handleDesc);
            set(handleDesc, setterHandleDesc);
        }

        return this;
    }

    private void ctor(FieldDescriptor handleDesc, FieldDescriptor setterHandleDesc) {
        var constructor = getCreator().getConstructorCreator(new String[0]);
        constructor.invokeSpecialMethod(ofConstructor(Object.class), constructor.getThis());

        TryBlock tryBlock = constructor.tryBlock();

        ResultHandle callerLookup = tryBlock.invokeStaticMethod(
                ofMethod(MethodHandles.class, "lookup", MethodHandles.Lookup.class));

        // Load entity class via TCCL to avoid classloader mismatch
        ResultHandle currentThread = tryBlock.invokeStaticMethod(
                ofMethod(Thread.class, "currentThread", Thread.class));
        ResultHandle tccl = tryBlock.invokeVirtualMethod(
                ofMethod(Thread.class, "getContextClassLoader", ClassLoader.class),
                currentThread);
        ResultHandle entityClass = tryBlock.invokeStaticMethod(
                ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                tryBlock.load(entity.getName()),
                tryBlock.load(true),
                tccl);
        ResultHandle privateLookup = tryBlock.invokeStaticMethod(
                ofMethod(MethodHandles.class, "privateLookupIn", MethodHandles.Lookup.class, Class.class, MethodHandles.Lookup.class),
                entityClass,
                callerLookup);

        if (isFieldBased) {
            ResultHandle fieldTypeClass = tryBlock.loadClass(propertyType);
            ResultHandle handle = tryBlock.invokeVirtualMethod(
                    ofMethod(MethodHandles.Lookup.class, "findVarHandle", VarHandle.class, Class.class, String.class, Class.class),
                    privateLookup,
                    entityClass,
                    tryBlock.load(propertyName),
                    fieldTypeClass);
            tryBlock.writeInstanceField(handleDesc, tryBlock.getThis(), handle);
        } else {
            // Getter MethodHandle
            ResultHandle returnTypeClass = tryBlock.loadClass(propertyType);
            ResultHandle getterMethodType = tryBlock.invokeStaticMethod(
                    ofMethod(MethodType.class, "methodType", MethodType.class, Class.class),
                    returnTypeClass);
            ResultHandle getterHandle = tryBlock.invokeVirtualMethod(
                    ofMethod(MethodHandles.Lookup.class, "findVirtual", MethodHandle.class, Class.class, String.class, MethodType.class),
                    privateLookup,
                    entityClass,
                    tryBlock.load(getterName),
                    getterMethodType);
            tryBlock.writeInstanceField(handleDesc, tryBlock.getThis(), getterHandle);

            // Setter MethodHandle (if applicable)
            if (setterHandleDesc != null) {
                ResultHandle voidClass = tryBlock.loadClass(void.class);
                ResultHandle paramTypeClass = tryBlock.loadClass(propertyType);
                ResultHandle setterMethodType = tryBlock.invokeStaticMethod(
                        ofMethod(MethodType.class, "methodType", MethodType.class, Class.class, Class.class),
                        voidClass,
                        paramTypeClass);
                ResultHandle setterHandle = tryBlock.invokeVirtualMethod(
                        ofMethod(MethodHandles.Lookup.class, "findVirtual", MethodHandle.class, Class.class, String.class,
                                MethodType.class),
                        privateLookup,
                        entityClass,
                        tryBlock.load(setterName),
                        setterMethodType);
                tryBlock.writeInstanceField(setterHandleDesc, tryBlock.getThis(), setterHandle);
            }
        }

        var catchBlock = tryBlock.addCatch(ReflectiveOperationException.class);
        ResultHandle ex = catchBlock.getCaughtException();
        ResultHandle wrapped = catchBlock.newInstance(
                ofConstructor(RuntimeException.class, Throwable.class),
                ex);
        catchBlock.throwException(wrapped);

        constructor.returnVoid();
        constructor.close();
    }

    private void get(FieldDescriptor handleDesc) {
        var method = getCreator().getMethodCreator(
                ofMethod(generatedType, "get", Object.class.getName(), Object.class.getName()));
        method.setSignature(
                forMethod()
                        .addTypeParameter(typeVariable("S"))
                        .setReturnType(classType(propertyType))
                        .addParameterType(typeVariable("S"))
                        .build());
        method.setParameterNames(new String[] { "model" });

        ResultHandle model = method.getMethodParam(0);
        ResultHandle handleRef = method.readInstanceField(handleDesc, method.getThis());

        ResultHandle result;
        if (isFieldBased) {
            result = method.invokeVirtualMethod(
                    ofMethod(VarHandle.class, "get", Object.class.getName(), Object.class.getName()),
                    handleRef,
                    model);
        } else {
            result = method.invokeVirtualMethod(
                    ofMethod(MethodHandle.class, "invoke", Object.class.getName(), Object.class.getName()),
                    handleRef,
                    model);
        }

        ResultHandle boxed = isPrimitive() ? method.smartCast(result, getWrapperType()) : result;
        method.returnValue(boxed);
    }

    private void set(FieldDescriptor handleDesc, FieldDescriptor setterHandleDesc) {
        var method = getCreator().getMethodCreator(
                ofMethod(generatedType, "set", "void", Object.class.getName(), Object.class.getName()));
        method.setSignature(
                forMethod()
                        .addTypeParameter(typeVariable("S"))
                        .setReturnType(voidType())
                        .addParameterType(typeVariable("S"))
                        .addParameterType(classType(propertyType))
                        .build());
        method.setParameterNames(new String[] { "model", "value" });

        if (!isFieldBased && setterHandleDesc == null) {
            // Read-only property — no setter
            method.throwException(UnsupportedOperationException.class, "Property '%s' is read-only".formatted(propertyName));
            return;
        }

        ResultHandle castModel = method.getMethodParam(0);
        ResultHandle castValue;
        if (isPrimitive()) {
            ResultHandle boxed = method.checkCast(method.getMethodParam(1), getWrapperType());
            castValue = method.smartCast(boxed, propertyType);
        } else {
            castValue = method.checkCast(method.getMethodParam(1), propertyType);
        }

        ResultHandle handleRef = isFieldBased
                ? method.readInstanceField(handleDesc, method.getThis())
                : method.readInstanceField(setterHandleDesc, method.getThis());

        if (isFieldBased) {
            method.invokeVirtualMethod(
                    ofMethod(VarHandle.class, "set", "void", Object.class.getName(), Object.class.getName()),
                    handleRef,
                    castModel,
                    castValue);
        } else {
            method.invokeVirtualMethod(
                    ofMethod(MethodHandle.class, "invoke", "void", Object.class.getName(), Object.class.getName()),
                    handleRef,
                    castModel,
                    castValue);
        }

        method.returnValue(null);
    }
}
