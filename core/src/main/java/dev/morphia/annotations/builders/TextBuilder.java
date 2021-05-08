package dev.morphia.annotations.builders;

import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Text;

/**
 * @morphia.internal
 * @since 2.0
 */
public class TextBuilder extends AnnotationBuilder<Text> implements Text {
    @Override
    public Class<Text> annotationType() {
        return Text.class;
    }

    @Override
    public IndexOptions options() {
        return get("options");
    }

    @Override
    public int value() {
        return get("value");
    }

    public TextBuilder options(IndexOptions options) {
        put("options", options);
        return this;
    }

    public TextBuilder value(int value) {
        put("value", value);
        return this;
    }
}
