package dev.morphia.query.filters;

import dev.morphia.annotations.internal.MorphiaInternal;

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

    /**
     * @param searchText the search text
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected TextSearchFilter(String searchText) {
        super("$text");
        this.searchText = searchText;
    }

    /**
     * Sets the search as case-sensitive or not
     *
     * @param caseSensitive the case sensitivity
     * @return this
     */
    public TextSearchFilter caseSensitive(Boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }

    /**
     * Sets the search as diacritic sensitive or not
     *
     * @param diacriticSensitive the diacritic sensitivity
     * @return this
     */
    public TextSearchFilter diacriticSensitive(Boolean diacriticSensitive) {
        this.diacriticSensitive = diacriticSensitive;
        return this;
    }

    /**
     * Sets the language to use
     *
     * @param language the language
     * @return this
     */
    public TextSearchFilter language(String language) {
        this.language = language;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the search text
     */
    @MorphiaInternal
    public String searchText() {
        return searchText;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the language
     */
    @MorphiaInternal
    public String language() {
        return language;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return true if case sensitive
     */
    @MorphiaInternal
    public Boolean caseSensitive() {
        return caseSensitive;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return true if diacritic sensitive
     */
    @MorphiaInternal
    public Boolean diacriticSensitive() {
        return diacriticSensitive;
    }
}
