package dev.morphia.test;

import java.util.ArrayList;
import java.util.List;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

/**
 * Strict BSON equality for operator cases: documents must have the same keys in the same order, and every value the
 * same BSON type and value. {@link BsonDocument#equals} ignores key order, so it is not used for documents. The
 * golden vectors in {@code generated/vectors/strict.json} pin this to the audits comparator's verdicts.
 */
public final class StrictBson {
    private StrictBson() {
    }

    public static boolean equal(BsonValue left, BsonValue right) {
        if (left instanceof BsonDocument a && right instanceof BsonDocument b) {
            if (!new ArrayList<>(a.keySet()).equals(new ArrayList<>(b.keySet()))) {
                return false;
            }
            for (String key : a.keySet()) {
                if (!equal(a.get(key), b.get(key))) {
                    return false;
                }
            }
            return true;
        }
        if (left instanceof BsonArray a && right instanceof BsonArray b) {
            return equal(a.getValues(), b.getValues(), true);
        }
        return left.getBsonType() == right.getBsonType() && left.equals(right);
    }

    /**
     * @param ordered whether position matters; if not, the lists must be equal as multisets
     */
    public static boolean equal(List<? extends BsonValue> left, List<? extends BsonValue> right, boolean ordered) {
        if (left.size() != right.size()) {
            return false;
        }
        if (ordered) {
            for (int i = 0; i < left.size(); i++) {
                if (!equal(left.get(i), right.get(i))) {
                    return false;
                }
            }
            return true;
        }
        List<BsonValue> remaining = new ArrayList<>(right);
        for (BsonValue value : left) {
            int match = -1;
            for (int i = 0; i < remaining.size() && match < 0; i++) {
                if (equal(value, remaining.get(i))) {
                    match = i;
                }
            }
            if (match < 0) {
                return false;
            }
            remaining.remove(match);
        }
        return true;
    }
}
