package dev.morphia.aggregation.expressions;

public enum MetadataKeyword {
    TEXTSCORE("textScore"),
    INDEXKEY("indexKey");

    private final String keyword;

    MetadataKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }
}
