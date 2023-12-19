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

    public MetaExpression(MetadataKeyword metadataKeyword) {
        super("$meta");
        this.metadataKeyword = metadataKeyword;
    }

    /**
     * @hidden
     * @return
     * @morphia.internal
     */
    @MorphiaInternal
    public MetadataKeyword metadataKeyword() {
        return metadataKeyword;
    }
}
