package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.sofia.Sofia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Unset extends Stage {
    private static final Logger LOG = LoggerFactory.getLogger(Unset.class);

    private List<Expression> fields = new ArrayList<>();

    protected Unset() {
        super("$unset");
    }

    public static Unset fields(String name, String... names) {
        Unset unset = new Unset()
                          .add(name);
        for (final String additional : names) {
            unset.add(additional);
        }
        return unset;
    }

    public Unset add(final String name) {
        final String fieldName = name;
        if(fieldName.startsWith("$")) {
            fieldName.substring(1);
            Sofia.logUnsetNamesDollarSign();
        }
        fields.add(Expression.literal(name));
        return this;
    }

    public List<Expression> getFields() {
        return fields;
    }
}
