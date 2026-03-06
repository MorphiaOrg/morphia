package dev.morphia.critter.parser.gizmo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.internal.AnnotationNodeExtensions;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

public class GizmoEntityModelGenerator extends BaseGizmoGenerator {
    private final ClassNode classNode;
    private final List<PropertyModelGenerator> properties;
    private final List<AnnotationNode> annotations;
    private final List<Annotation> morphiaAnnotations;
    private final Entity entityAnnotation;
    private final MorphiaConfig config;

    public GizmoEntityModelGenerator(Class<?> type, CritterClassLoader critterClassLoader,
            ClassNode classNode, List<PropertyModelGenerator> properties, MorphiaConfig config) {
        super(type, critterClassLoader);
        this.config = config;
        this.classNode = classNode;
        this.properties = properties;

        generatedType = "%s.%sEntityModel".formatted(baseName, type.getSimpleName());
        this.annotations = classNode.visibleAnnotations != null ? classNode.visibleAnnotations : Collections.emptyList();

        Entity ann = type.getAnnotation(Entity.class);
        if (ann == null) {
            throw new IllegalStateException("Class %s does not have @Entity annotation".formatted(type.getName()));
        }
        this.entityAnnotation = ann;

        this.morphiaAnnotations = new ArrayList<>();
        for (AnnotationNode a : annotations) {
            if (a.desc.startsWith("Ldev/morphia/annotations/")) {
                morphiaAnnotations.add((Annotation) AnnotationNodeExtensions.INSTANCE.toMorphiaAnnotation(a));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T annotation(Class<T> type) {
        return entity.getAnnotation(type);
    }

    public String getGeneratedType() {
        return generatedType;
    }

    public GizmoEntityModelGenerator emit() {
        getBuilder().superClass(CritterEntityModel.class);

        try (var creator = getCreator()) {
            ctor();
            collectionName();
            discriminator();
            discriminatorKey();
            hasLifecycle();
            isAbstract();
            isInterface();
            useDiscriminator();
        }

        return this;
    }

    private void useDiscriminator() {
        try (MethodCreator mc = getCreator().getMethodCreator("useDiscriminator", boolean.class)) {
            mc.returnValue(mc.load(entityAnnotation.useDiscriminator()));
        }
    }

    private void isInterface() {
        try (MethodCreator mc = getCreator().getMethodCreator("isInterface", boolean.class)) {
            mc.returnValue(mc.load(entity.isInterface()));
        }
    }

    private void hasLifecycle() {
        try (MethodCreator mc = getCreator().getMethodCreator("hasLifecycle", boolean.class, Class.class)) {
            mc.setParameterNames(new String[] { "type" });
            mc.returnValue(mc.load(false));
        }
    }

    private void isAbstract() {
        try (MethodCreator mc = getCreator().getMethodCreator("isAbstract", boolean.class)) {
            mc.returnValue(mc.load(Modifier.isAbstract(entity.getModifiers())));
        }
    }

    private void discriminatorKey() {
        try (MethodCreator mc = getCreator().getMethodCreator("discriminatorKey", String.class)) {
            String key = entityAnnotation.discriminator();
            String result = Mapper.IGNORED_FIELDNAME.equals(key)
                    ? config.discriminatorKey()
                    : key;
            mc.returnValue(mc.load(result));
        }
    }

    private void discriminator() {
        try (MethodCreator mc = getCreator().getMethodCreator("discriminator", String.class)) {
            String discriminator = config.discriminator()
                    .apply(entity, entityAnnotation.discriminator());
            mc.returnValue(mc.load(discriminator));
        }
    }

    private void collectionName() {
        try (MethodCreator mc = getCreator().getMethodCreator("collectionName", String.class)) {
            String key = entityAnnotation.value();
            String result = Mapper.IGNORED_FIELDNAME.equals(key)
                    ? config.collectionNaming().apply(entity.getSimpleName())
                    : key;
            mc.returnValue(mc.load(result));
        }
    }

    private void ctor() {
        try (MethodCreator constructor = getCreator().getConstructorCreator(Mapper.class)) {
            constructor.invokeSpecialMethod(
                    MethodDescriptor.ofConstructor(CritterEntityModel.class, Mapper.class, Class.class),
                    constructor.getThis(),
                    constructor.getMethodParam(0),
                    constructor.loadClass(entity));
            constructor.setParameterNames(new String[] { "mapper" });

            constructor.invokeVirtualMethod(
                    ofMethod(generatedType, "setType", "void", Class.class),
                    constructor.getThis(),
                    constructor.loadClass(entity));
            loadProperties(constructor);
            registerAnnotations(constructor);
            constructor.returnVoid();
        }
    }

    private void loadProperties(MethodCreator creator) {
        MethodDescriptor addProperty = ofMethod(generatedType, "addProperty", "boolean", PropertyModel.class);
        for (PropertyModelGenerator property : properties) {
            MethodDescriptor modelCtor = MethodDescriptor.ofConstructor(property.generatedType, EntityModel.class);
            var model = creator.newInstance(modelCtor, creator.getThis());
            creator.invokeVirtualMethod(addProperty, creator.getThis(), model);
        }
    }

    private void registerAnnotations(MethodCreator constructor) {
        MethodDescriptor annotationMethod = ofMethod(generatedType, "annotation", "void", Annotation.class);
        for (AnnotationNode annotation : annotations) {
            constructor.invokeVirtualMethod(
                    annotationMethod,
                    constructor.getThis(),
                    GizmoExtensions.annotationBuilder(annotation, constructor));
        }
    }
}
