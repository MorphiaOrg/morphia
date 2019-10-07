package dev.morphia.query;

import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import static java.util.stream.Collectors.toList;

class Operations {
    private Map<UpdateOperator, List<TargetValue>> ops = new HashMap<>();

    public void add(final UpdateOperator operator, final TargetValue value) {
        ops.computeIfAbsent(operator, o -> new ArrayList<>()).add(value);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Operations.class.getSimpleName() + "[", "]")
                   .add("ops=" + ops)
                   .toString();
    }

    public Document toDocument() {
        Document document = new Document();
        for (final Entry<UpdateOperator, List<TargetValue>> entry : ops.entrySet()) {
            var list = entry.getValue().stream()
                            .map(TargetValue::encode)
                            .collect(toList());

            document.put(entry.getKey().val(), list.size() == 1 ? list.get(0) : list);
        }
        return document;
    }
}
