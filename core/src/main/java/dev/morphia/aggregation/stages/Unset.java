package dev.morphia.aggregation.stages;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes/excludes fields from documents.
 *
 * @aggregation.stage $unset
 */
public class Unset extends Stage {
    private static final Logger LOG = LoggerFactory.getLogger(Unset.class);

    private final List<Expression> fields = new ArrayList<>();

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Unset() {
        super("$unset");
    }

    /**
     * Creates a new stage with the given fields
     *
     * @param name  the first field
     * @param names the others
     * @return this
     * @since 2.2
     */
    public static Unset unset(String name, String... names) {
        Unset unset = new Unset()
                .add(name);
        for (String additional : names) {
            unset.add(additional);
        }
        return unset;
    }

    /**
     * @return the fields
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public List<Expression> getFields() {
        return fields;
    }

    private Unset add(String name) {
        String fieldName = name;
        if (fieldName.startsWith("$")) {
            fieldName = fieldName.substring(1);
            LOG.warn(Sofia.unsetNamesDollarSign());
        }
        fields.add(Expressions.value(fieldName));
        return this;
    }
}
