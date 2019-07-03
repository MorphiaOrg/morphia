package dev.morphia.mapping.codec.pojo;

import org.bson.codecs.configuration.CodecConfigurationException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public final class DiscriminatorLookup {
    private final Map<String, Class<?>> discriminatorClassMap = new ConcurrentHashMap<>();
    private final Set<String> packages;

    public DiscriminatorLookup(final Map<Class<?>, ClassModel<?>> classModels, final Set<String> packages) {
        for (ClassModel<?> classModel : classModels.values()) {
            if (classModel.getDiscriminator() != null) {
                discriminatorClassMap.put(classModel.getDiscriminator(), classModel.getType());
            }
        }
        this.packages = packages;
    }

    public Class<?> lookup(final String discriminator) {
        if (discriminatorClassMap.containsKey(discriminator)) {
            return discriminatorClassMap.get(discriminator);
        }

        Class<?> clazz = getClassForName(discriminator);
        if (clazz == null) {
            clazz = searchPackages(discriminator);
        }

        if (clazz == null) {
            throw new CodecConfigurationException(format("A class could not be found for the discriminator: '%s'.",  discriminator));
        } else {
            discriminatorClassMap.put(discriminator, clazz);
        }
        return clazz;
    }

    public void addClassModel(final ClassModel<?> classModel) {
        if (classModel.getDiscriminator() != null) {
            discriminatorClassMap.put(classModel.getDiscriminator(), classModel.getType());
        }
    }

    private Class<?> getClassForName(final String discriminator) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(discriminator);
        } catch (final ClassNotFoundException e) {
            // Ignore
        }
        return clazz;
    }

    private Class<?> searchPackages(final String discriminator) {
        Class<?> clazz = null;
        for (String packageName : packages) {
            clazz = getClassForName(packageName + "." + discriminator);
            if (clazz != null) {
                return clazz;
            }
        }
        return clazz;
    }
}