package dev.morphia.critter.parser.gizmo;

import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;

import io.quarkus.gizmo.ClassCreator;

public abstract class BaseGizmoGenerator {
    protected final Class<?> entity;
    protected final CritterClassLoader critterClassLoader;
    protected String generatedType;

    protected final String baseName;

    private ClassCreator.Builder builder;
    private ClassCreator creator;

    protected BaseGizmoGenerator(Class<?> entity, CritterClassLoader critterClassLoader) {
        this.entity = entity;
        this.critterClassLoader = critterClassLoader;
        this.baseName = Critter.critterPackage(entity);
    }

    protected ClassCreator.Builder getBuilder() {
        if (builder == null) {
            builder = ClassCreator.builder()
                    .classOutput((name, data) -> critterClassLoader.register(name.replace('/', '.'), data))
                    .className(generatedType);
        }
        return builder;
    }

    protected ClassCreator getCreator() {
        if (creator == null) {
            creator = getBuilder().build();
        }
        return creator;
    }
}
