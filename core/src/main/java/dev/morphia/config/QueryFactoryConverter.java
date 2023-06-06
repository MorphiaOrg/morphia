package dev.morphia.config;

import dev.morphia.query.QueryFactory;
import org.eclipse.microprofile.config.spi.Converter;

public class QueryFactoryConverter implements Converter<QueryFactory> {
    @Override
    public QueryFactory convert(String value) throws IllegalArgumentException, NullPointerException {
        try {
            return (QueryFactory) Class.forName(value).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
