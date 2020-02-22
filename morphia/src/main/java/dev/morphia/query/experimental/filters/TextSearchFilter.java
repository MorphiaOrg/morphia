package dev.morphia.query.experimental.filters;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

/**
 * Defines a text search filter
 *
 * @since 2.0
 */
public class TextSearchFilter extends Filter {
    private final String searchText;
    private String language;
    private Boolean caseSensitive;
    private Boolean diacriticSensitive;

    protected TextSearchFilter(final String searchText) {
        super("$text");
        this.searchText = searchText;
    }

    /**
     * Sets the search as case sensitive or not
     *
     * @param caseSensitive the case sensitivity
     * @return this
     */
    public TextSearchFilter caseSensitive(final Boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }

    /**
     * Sets the search as diacritic sensitive or not
     *
     * @param diacriticSensitive the diacritic sensitivity
     * @return this
     */
    public TextSearchFilter diacriticSensitive(final Boolean diacriticSensitive) {
        this.diacriticSensitive = diacriticSensitive;
        return this;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
        writer.writeStartDocument(getFilterName());
        writeNamedValue("$search", searchText, mapper, writer, context);
        if (language != null) {
            writeNamedValue("$language", language, mapper, writer, context);
        }
        if (Boolean.TRUE.equals(caseSensitive)) {
            writeNamedValue("$caseSensitive", caseSensitive, mapper, writer, context);
        }
        if (Boolean.TRUE.equals(diacriticSensitive)) {
            writeNamedValue("$diacriticSensitive", diacriticSensitive, mapper, writer, context);
        }
        writer.writeEndDocument();
    }

    /**
     * Sets the language to use
     *
     * @param language the language
     * @return this
     */
    public TextSearchFilter language(final String language) {
        this.language = language;
        return this;
    }
}
