package dev.morphia.critter;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Transient;

import org.objectweb.asm.Type;

public class Critter {
    public static final List<Type> propertyAnnotations = new ArrayList<>(List.of(Type.getType(Property.class)));
    public static final List<Type> transientAnnotations = new ArrayList<>(List.of(Type.getType(Transient.class)));

    public static String critterPackage(Class<?> entity) {
        return entity.getPackageName() + ".__morphia." + entity.getSimpleName().toLowerCase();
    }

    public static String titleCase(String s) {
        if (s == null || s.isEmpty())
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String identifierCase(String s) {
        if (s == null || s.isEmpty())
            return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private final File root;
    private final File outputDir;
    private final File ksp;

    public Critter(File root) {
        this.root = root;
        this.outputDir = new File(root, "target");
        this.ksp = new File(outputDir, "ksp");
    }

    private URI loadPath() throws Exception {
        return Entity.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    }
}
