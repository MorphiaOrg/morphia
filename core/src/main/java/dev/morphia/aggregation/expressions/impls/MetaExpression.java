package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.MetadataKeyword;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class MetaExpression extends Expression {

    private final MetadataKeyword metadataKeyword;

    /**
     * @param metadataKeyword the metadata keyword to use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MetaExpression(MetadataKeyword metadataKeyword) {
        super("$meta");
        this.metadataKeyword = metadataKeyword;
    }

    /**
     * @return the metadata keyword
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MetadataKeyword metadataKeyword() {
        return metadataKeyword;
    }
}
