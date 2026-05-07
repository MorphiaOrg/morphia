package dev.morphia.aggregation.expressions;

/**
 * Keywords for use with the $meta aggregation expression.
 *
 * @since 3.0
 */
public enum MetadataKeyword {
    /** The text search score */
    TEXTSCORE("textScore"),
    /** The index key */
    INDEXKEY("indexKey");

    private final String keyword;

    MetadataKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * @return the keyword string value
     */
    public String keyword() {
        return keyword;
    }
}
