package dev.morphia.critter.parser.gizmo;

import java.util.Map;

import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.ExtensionFunctions;

import org.bson.codecs.pojo.PropertyAccessor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.gizmo.SignatureBuilder.forClass;
import static io.quarkus.gizmo.SignatureBuilder.forMethod;
import static io.quarkus.gizmo.Type.classType;
import static io.quarkus.gizmo.Type.parameterizedType;
import static io.quarkus.gizmo.Type.typeVariable;
import static io.quarkus.gizmo.Type.voidType;

public class PropertyAccessorGenerator extends BaseGizmoGenerator {
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

    public PropertyAccessorGenerator(Class<?> entity, CritterClassLoader critterClassLoader, FieldNode field) {
        super(entity, critterClassLoader);
        this.propertyName = field.name;
        this.propertyType = Type.getType(field.desc).getClassName();
        generatedType = baseName + "." + Critter.titleCase(propertyName) + "Accessor";
    }

    public PropertyAccessorGenerator(Class<?> entity, CritterClassLoader critterClassLoader, MethodNode method) {
        super(entity, critterClassLoader);
        this.propertyName = ExtensionFunctions.getterToPropertyName(method, entity);
        this.propertyType = Type.getReturnType(method.desc).getClassName();
        generatedType = baseName + "." + Critter.titleCase(propertyName) + "Accessor";
    }

    public boolean isPrimitive() {
        return PRIMITIVE_TO_WRAPPER.containsKey(propertyType);
    }

    public String getWrapperType() {
        return PRIMITIVE_TO_WRAPPER.getOrDefault(propertyType, propertyType);
    }

    public PropertyAccessorGenerator emit() {
        getBuilder().signature(
                forClass()
                        .addInterface(
                                parameterizedType(
                                        classType(PropertyAccessor.class),
                                        classType(propertyType))));

        try (var creator = getCreator()) {
            ctor();
            get();
            set();
        }

        return this;
    }

    private void get() {
        var method = getCreator().getMethodCreator(
                ofMethod(generatedType, "get", Object.class.getName(), Object.class.getName()));
        method.setSignature(
                forMethod()
                        .addTypeParameter(typeVariable("S"))
                        .setReturnType(classType(propertyType))
                        .addParameterType(typeVariable("S"))
                        .build());
        method.setParameterNames(new String[] { "model" });
        ResultHandle castModel = method.checkCast(method.getMethodParam(0), entity);
        MethodDescriptor toInvoke = ofMethod(entity, "__read" + Critter.titleCase(propertyName), propertyType);
        ResultHandle result = method.invokeVirtualMethod(toInvoke, castModel);
        ResultHandle boxed = isPrimitive() ? method.smartCast(result, getWrapperType()) : result;
        method.returnValue(boxed);
    }

    private void set() {
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
        ResultHandle castModel = method.checkCast(method.getMethodParam(0), entity);
        ResultHandle castValue;
        if (isPrimitive()) {
            ResultHandle boxed = method.checkCast(method.getMethodParam(1), getWrapperType());
            castValue = method.smartCast(boxed, propertyType);
        } else {
            castValue = method.checkCast(method.getMethodParam(1), propertyType);
        }
        MethodDescriptor toInvoke = ofMethod(entity, "__write" + Critter.titleCase(propertyName), "void", propertyType);
        method.invokeVirtualMethod(toInvoke, castModel, castValue);
        method.returnValue(null);
    }

    private void ctor() {
        var constructor = getCreator().getConstructorCreator(new String[0]);
        constructor.invokeSpecialMethod(ofConstructor(Object.class), constructor.getThis());
        constructor.returnVoid();
        constructor.close();
    }
}
