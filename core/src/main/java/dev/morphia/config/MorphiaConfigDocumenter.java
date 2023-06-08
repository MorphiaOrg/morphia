package dev.morphia.config;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import dev.morphia.annotations.PossibleValues;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.NamingStrategy;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

import static dev.morphia.annotations.internal.PossibleValuesBuilder.possibleValuesBuilder;
import static java.lang.String.join;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * @hidden
 * @since 2.4
 * @morphia.internal
 */
@MorphiaInternal
public class MorphiaConfigDocumenter {
    final String prefix;
    private final MorphiaConfig config;
    private final List<Entry> entries;

    public MorphiaConfigDocumenter(MorphiaConfig config) {
        this.config = config;
        prefix = getPrefix();
        entries = stream(MorphiaConfig.class.getDeclaredMethods())
                .sorted(Comparator.comparing(Method::getName))
                .map(m -> getEntry(prefix, m))
                .collect(Collectors.toList());

    }

    @Override
    public String toString() {
        return entries
                .stream()
                .map(Entry::printEntry)
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

        var entry = new Entry(
                prefix + NamingStrategy.kebabCase().apply(m.getName()),
                Optional.class.isAssignableFrom(m.getReturnType()),
                getValue(m),
                getDefault(m),
                getPossibles(m),
                converter);

        return entry;
    }

    private static String getPrefix() {
        var prefix = MorphiaConfig.class.getAnnotation(ConfigMapping.class).prefix();
        if (!prefix.equals("")) {
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
            return "";
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

    private PossibleValues getPossibles(Method m) {
        if (!m.getReturnType().isEnum()) {
            PossibleValues annotation = m.getAnnotation(PossibleValues.class);
            return annotation;
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

    private static class Entry {
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

        private void normalize() {
            if (converter != null && possibleValues != null) {
                stream(possibleValues.value())
                        .filter(v1 -> value.equals(convertToString(converter.convert(v1))))
                        .findFirst().ifPresent(v -> {
                            value = v;
                        });
            }

        }

        String printEntry() {
            StringJoiner joiner = new StringJoiner("\n");

            if (optional) {
                joiner.add("# *optional*");
            }
            if (!optional && defaultValue == null) {
                joiner.add("# *required*");
            }
            if (defaultValue != null) {
                joiner.add("# default = " + defaultValue);
            }
            if (possibleValues != null) {
                joiner.add("# possible values = " +
                        join(", ", possibleValues.value()));
            }

            joiner.add(name + "=" + value);

            return joiner.toString();
        }
    }
}
