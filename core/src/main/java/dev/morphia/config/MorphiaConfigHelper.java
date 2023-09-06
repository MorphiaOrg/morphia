package dev.morphia.config;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;

import dev.morphia.annotations.PossibleValues;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.sofia.Sofia;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SysPropConfigSource;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

import static dev.morphia.annotations.internal.PossibleValuesBuilder.possibleValuesBuilder;
import static io.smallrye.config.PropertiesConfigSourceProvider.classPathSources;
import static java.lang.String.join;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;

/**
 * @hidden
 * @since 2.4
 * @morphia.internal
 */
@MorphiaInternal
public class MorphiaConfigHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaConfig.class);

    private static final String MORPHIA_CONFIG_PROPERTIES = "META-INF/morphia-config.properties";
    final String prefix;
    private final MorphiaConfig config;
    private final boolean showComplete;
    private final List<Entry> entries;

    /**
     * Logs the configuration file form of the MapperOptions without default values listed.
     *
     * @param options  the options to convert
     * @param database the database name to configure
     * @return
     * @since 2.4
     */
    public static String dumpConfigurationFile(MapperOptions options, String database, boolean showComplete) {
        MapperOptionsWrapper wrapper = new MapperOptionsWrapper(options, database);
        MorphiaConfigHelper helper = new MorphiaConfigHelper(wrapper, showComplete);
        return helper.toString();
    }

    /**
     * @param config
     * @param showComplete
     * @hidden
     */
    MorphiaConfigHelper(MorphiaConfig config, boolean showComplete) {
        this.config = config;
        this.showComplete = showComplete;
        prefix = getPrefix();
        entries = stream(MorphiaConfig.class.getDeclaredMethods())
                .sorted(Comparator.comparing(Method::getName))
                .map(m -> getEntry(prefix, m))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Map<String, String> getEnvProperties() {
        return unmodifiableMap(new TreeMap<>(System.getenv()));
    }

    /**
     * @hidden
     * @return
     * @morphia.internal
     */
    @MorphiaInternal
    public static MorphiaConfig loadConfig() {
        return loadConfig(MORPHIA_CONFIG_PROPERTIES);
    }

    /**
     * @hidden
     * @return
     * @morphia.internal
     */
    @MorphiaInternal
    public static MorphiaConfig loadConfig(String path) {
        List<ConfigSource> configSources = classPathSources(path.startsWith("META-INF/") ? path : "META-INF/" + path,
                currentThread().getContextClassLoader());
        if (configSources.isEmpty()) {
            LOG.warn(Sofia.missingConfigFile(path));
            return MorphiaConfigHelper.defaultConfig();
        }
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(MorphiaConfig.class)
                .withSources(new EnvConfigSource(getEnvProperties(), 300),
                        new SysPropConfigSource())
                .withSources(configSources)
                .addDefaultSources()
                .build()
                .getConfigMapping(MorphiaConfig.class);
    }

    private static MorphiaConfig defaultConfig() {
        return new DefaultMorphiaConfig();

    }

    @Override
    public String toString() {
        return entries
                .stream()
                .map(Entry::printEntry)
                .filter(Objects::nonNull)
                .collect(joining("\n"));
    }

    private Entry getEntry(String prefix, Method m) {
        WithConverter annotation = m.getAnnotation(WithConverter.class);
        Converter<?> converter = null;
        if (annotation != null) {
            try {
                converter = annotation
                        .value()
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        if (m.getAnnotation(MorphiaInternal.class) != null) {
            return null;
        } else {

            return new Entry(
                    prefix + NamingStrategy.kebabCase().apply(m.getName()),
                    Optional.class.isAssignableFrom(m.getReturnType()),
                    getValue(m),
                    getDefault(m),
                    getPossibles(m),
                    converter);
        }
    }

    private static String getPrefix() {
        var prefix = MorphiaConfig.class.getAnnotation(ConfigMapping.class).prefix();
        if (!prefix.isEmpty()) {
            prefix = prefix + ".";
        }
        return prefix;
    }

    private Object getValue(Method m) {
        try {
            Object invoke = m.invoke(config);
            return convertToString(invoke);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static String convertToString(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Enum) {
            return ((Enum<?>) value).name().toLowerCase();
        } else if (value instanceof Optional) {
            return convertToString(((Optional<?>) value).orElse(null));
        } else if (value instanceof Class) {
            return ((Class<?>) value).getName();
        } else if (value instanceof Boolean) {
            return value.toString().toLowerCase();
        } else if (value instanceof List) {
            var list = (List<?>) value;
            StringJoiner joiner = new StringJoiner(",");
            for (Object o : list) {
                joiner.add(convertToString(o));
            }
            return joiner.toString();
        } else {
            return value.getClass().getName();
        }
    }

    private PossibleValues getPossibles(Method m) {
        if (!m.getReturnType().isEnum()) {
            return m.getAnnotation(PossibleValues.class);
        }
        return possibleValuesBuilder()
                .value(stream(m.getReturnType().getEnumConstants())
                        .map(v -> (Enum<?>) v)
                        .map(e -> e.name().toLowerCase())
                        .toArray(String[]::new))
                .build();
    }

    private String getDefault(Method m) {
        WithDefault annotation = m.getAnnotation(WithDefault.class);
        if (annotation == null) {
            return null;
        }
        return annotation.value();
    }

    private class Entry {
        String name;
        private final boolean optional;
        Object value;
        String defaultValue;
        PossibleValues possibleValues;
        Converter<?> converter;

        public Entry(String name, boolean optional, Object value, String defaultValue, PossibleValues possibleValues,
                Converter<?> converter) {
            this.name = name;
            this.value = value;
            this.defaultValue = defaultValue;
            this.possibleValues = possibleValues;
            this.converter = converter;
            this.optional = optional;
            normalize();
        }

        @Override
        public String toString() {
            return name;
        }

        private void normalize() {
            if (possibleValues != null) {
                stream(possibleValues.value())
                        .filter(v1 -> {
                            String converted = converter != null ? convertToString(converter.convert(v1)) : v1;
                            return value.equals(converted);
                        })
                        .findFirst().ifPresent(v -> {
                            value = v;
                        });
            }

        }

        String printEntry() {
            if (showComplete || value != null && !value.equals(defaultValue)) {
                StringJoiner joiner = new StringJoiner("\n");
                joiner.add("######");
                if (optional) {
                    joiner.add("# Optional");
                }
                if (!optional && defaultValue == null) {
                    joiner.add("# Required");
                }
                if (defaultValue != null) {
                    joiner.add("# default=" + defaultValue);
                }
                if (possibleValues != null) {
                    joiner.add("# possible values=" +
                            join(", ", possibleValues.value()));
                }
                joiner.add("######");

                joiner.add(name + "=" + (value != null ? value : ""));
                return joiner.toString();
            }
            return null;
        }
    }
}
