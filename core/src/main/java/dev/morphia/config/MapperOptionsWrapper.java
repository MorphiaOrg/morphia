package dev.morphia.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import dev.morphia.annotations.PossibleValues;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.query.QueryFactory;

import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import static java.lang.String.join;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class MapperOptionsWrapper implements MorphiaConfig {
    private static final Logger LOG = LoggerFactory.getLogger(MapperOptions.class);

    private final MapperOptions options;
    private final String database;

    public MapperOptionsWrapper(MapperOptions options, String database) {
        this.options = options;
        this.database = database;
        logConfigMessage();
    }

    private void logConfigMessage() {
        final String prefix = getPrefix();
        var content = stream(MorphiaConfig.class.getDeclaredMethods())
                .sorted(Comparator.comparing(Method::getName))
                .map(m -> printEntry(prefix, m))
                .collect(joining("\n", prefix, ""));
        LOG.info("Morphia 3.0 will be moving to a configuration file based setup.  As such MapperOptions will be removed in the next " +
                "major release.  To remove this message, create the file 'META-INF/morphia-config.properties' in your resources folder " +
                "using the following text.  Entries with default values may be omitted but are included here for completeness.\n" +
                content);
    }

    private static String getPrefix() {
        var prefix = MorphiaConfig.class.getAnnotation(ConfigMapping.class).prefix();
        if (!prefix.equals("")) {
            prefix = prefix + ".";
        }
        return prefix;
    }

    private String printEntry(String prefix, Method m) {
        try {
            var name = NamingStrategy.kebabCase().apply(m.getName());
            var value = getValue(m);
            var defaultValue = getDefault(m);
            var possibles = getPossibles(m);
            StringBuilder builder = new StringBuilder(prefix + name)
                    .append(" = ")
                    .append(value);
            if (defaultValue != null || possibles != null) {
                builder.append("  # ");
                StringJoiner joiner = new StringJoiner(", ");
                if (defaultValue != null) {
                    joiner.add("default = " + defaultValue);
                }
                if (possibles != null) {
                    joiner.add("possible values = [" + possibles + "]");
                }
                builder.append(joiner);
            }

            return builder.toString();
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    private Object getValue(Method m) throws IllegalAccessException, InvocationTargetException {
        return convertToString(m.invoke(this));
    }

    private String convertToString(Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        } else if (value instanceof Optional) {
            return convertToString(((Optional<?>) value).orElse(null));
        } else if (value instanceof Class) {
            return ((Class<?>) value).getName();
        } else if (value instanceof Boolean) {
            return value.toString().toLowerCase();
        } else if (value instanceof List) {
            var list = (List) value;
            StringJoiner joiner = new StringJoiner(",");
            for (Object o : list) {
                joiner.add(convertToString(o));
            }
            return joiner.toString();
        } else {
            return value.getClass().getName();
        }
    }

    private String getPossibles(Method m) {
        if (!m.getReturnType().isEnum()) {
            PossibleValues annotation = m.getAnnotation(PossibleValues.class);
            if (annotation != null) {
                List<String> values = stream(annotation.value())
                        .collect(toCollection(ArrayList::new));
                if (annotation.fqcn()) {
                    values.add("<class name>");
                }
                return join(",", values);
            }
            return null;
        }
        return stream(m.getReturnType().getEnumConstants())
                .map(v -> (Enum<?>) v)
                .map(e -> e.name().toLowerCase())
                .collect(joining(","));
    }

    private String getDefault(Method m) {
        WithDefault annotation = m.getAnnotation(WithDefault.class);
        if (annotation == null) {
            return null;
        }
        return annotation.value();
    }

    @Override
    public String database() {
        return database;
    }

    @Override
    public Optional<CodecProvider> codecProvider() {
        return Optional.ofNullable(options.codecProvider());
    }

    @Override
    public NamingStrategy collectionNaming() {
        return options.getCollectionNaming();
    }

    @Override
    public DateStorage dateStorage() {
        return options.getDateStorage();
    }

    @Override
    public DiscriminatorFunction discriminator() {
        return options.getDiscriminator();
    }

    @Override
    public String discriminatorKey() {
        return options.getDiscriminatorKey();
    }

    @Override
    public boolean enablePolymorphicQueries() {
        return options.isEnablePolymorphicQueries();
    }

    @Override
    public boolean ignoreFinals() {
        return options.isIgnoreFinals();
    }

    @Override
    public boolean mapSubPackages() {
        return options.isMapSubPackages();
    }

    @Override
    public PropertyDiscovery propertyDiscovery() {
        return options.propertyDiscovery();
    }

    @Override
    public NamingStrategy propertyNaming() {
        return options.getPropertyNaming();
    }

    @Override
    public QueryFactory queryFactory() {
        return options.getQueryFactory();
    }

    @Override
    public boolean storeEmpties() {
        return options.isStoreEmpties();
    }

    @Override
    public boolean storeNulls() {
        return options.isStoreNulls();
    }

    @Override
    @SuppressWarnings("removal")
    public UuidRepresentation uuidRepresentation() {
        return options.getUuidRepresentation();
    }
}
