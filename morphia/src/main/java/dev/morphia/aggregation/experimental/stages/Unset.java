package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.sofia.Sofia;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes/excludes fields from documents.
 *
 * @mongodb.driver.manual reference/operator/aggregation/unset/ $unset
 */
public class Unset extends Stage {
    private List<Expression> fields = new ArrayList<>();

    protected Unset() {
        super("$unset");
    }

    /**
     * Creates a new stage with the given fields
     *
     * @param name  the first field
     * @param names the others
     * @return this
     */
    public static Unset fields(final String name, final String... names) {
        Unset unset = new Unset()
                          .add(name);
        for (final String additional : names) {
            unset.add(additional);
        }
        return unset;
    }

    private Unset add(final String name) {
        String fieldName = name;
        if (fieldName.startsWith("$")) {
            fieldName = fieldName.substring(1);
            Sofia.logUnsetNamesDollarSign();
        }
        fields.add(Expressions.value(fieldName));
        return this;
    }

    /**
     * @return the fields
     * @morphia.internal
     */
    public List<Expression> getFields() {
        return fields;
    }
}
