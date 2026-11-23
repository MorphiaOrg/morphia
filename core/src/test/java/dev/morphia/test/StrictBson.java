package dev.morphia.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonValue;

/**
 * Strict BSON equality for operator cases: documents must have the same keys in the same order, and every value the
 * same BSON type and value. {@link BsonDocument#equals} ignores key order, so it is not used for documents. Arrays
 * keep their order unless a comparison names them as unordered, by RFC 6901 pointer relative to each result; an
 * unordered array must hold the same items, as often, in any order. The golden vectors in
 * {@code generated/vectors/strict.json} pin this to the audits comparator's verdicts.
 */
public final class StrictBson {
    private StrictBson() {
    }

    public static boolean equal(BsonValue left, BsonValue right) {
        return equal(left, right, "", Set.of());
    }

    /**
     * @param ordered whether position matters; if not, the lists must be equal as multisets
     */
    public static boolean equal(List<? extends BsonValue> left, List<? extends BsonValue> right, boolean ordered) {
        return equal(left, right, ordered, Set.of());
    }

    /**
     * @param ordered         whether position matters; if not, the lists must be equal as multisets
     * @param unorderedArrays the pointers, relative to each result, of the arrays compared as multisets
     */
    public static boolean equal(List<? extends BsonValue> left, List<? extends BsonValue> right, boolean ordered,
            Set<String> unorderedArrays) {
        return equal(left, right, ordered, row -> "", unorderedArrays);
    }

    /** {@code path} is the pointer of {@code left} and {@code right}, and items are addressed by their left index. */
    private static boolean equal(BsonValue left, BsonValue right, String path, Set<String> unordered) {
        if (left instanceof BsonDocument a && right instanceof BsonDocument b) {
            if (!new ArrayList<>(a.keySet()).equals(new ArrayList<>(b.keySet()))) {
                return false;
            }
            for (String key : a.keySet()) {
                if (!equal(a.get(key), b.get(key), child(path, key), unordered)) {
                    return false;
                }
            }
            return true;
        }
        if (left instanceof BsonArray a && right instanceof BsonArray b) {
            return equal(a.getValues(), b.getValues(), !unordered.contains(path),
                    item -> child(path, String.valueOf(item)), unordered);
        }
        if (left instanceof BsonDouble a && right instanceof BsonDouble b) {
            // Raw bits, as the audits comparator compares doubles: -0.0 is not 0.0, and NaN payloads differ.
            return Double.doubleToRawLongBits(a.getValue()) == Double.doubleToRawLongBits(b.getValue());
        }
        return left.getBsonType() == right.getBsonType() && left.equals(right);
    }

    /**
     * {@code at} gives each left element's pointer: the row's own for results, the item's for an array. Strict
     * equality is an equivalence, so taking the first match never rejects a valid pairing.
     */
    private static boolean equal(List<? extends BsonValue> left, List<? extends BsonValue> right, boolean ordered,
            IntFunction<String> at, Set<String> unordered) {
        if (left.size() != right.size()) {
            return false;
        }
        if (ordered) {
            for (int i = 0; i < left.size(); i++) {
                if (!equal(left.get(i), right.get(i), at.apply(i), unordered)) {
                    return false;
                }
            }
            return true;
        }
        List<BsonValue> remaining = new ArrayList<>(right);
        for (int index = 0; index < left.size(); index++) {
            int match = -1;
            for (int i = 0; i < remaining.size() && match < 0; i++) {
                if (equal(left.get(index), remaining.get(i), at.apply(index), unordered)) {
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

    private static String child(String path, String segment) {
        return path + "/" + segment.replace("~", "~0").replace("/", "~1");
    }
}
