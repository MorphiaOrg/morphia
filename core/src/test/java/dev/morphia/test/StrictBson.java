package dev.morphia.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonJavaScriptWithScope;
import org.bson.BsonValue;

/**
 * Strict BSON equality for operator cases: documents must have the same keys in the same order, and every value the
 * same BSON type and value. {@link BsonDocument#equals} ignores key order, so it is not used for documents. Arrays
 * keep their order unless a comparison names them as unordered, by RFC 6901 pointer relative to each result; an
 * unordered array must hold the same items, as often, in any order. The golden vectors in
 * {@code vectors/strict.json} pin this to the audits comparator's verdicts.
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
        if (left instanceof BsonJavaScriptWithScope a && right instanceof BsonJavaScriptWithScope b) {
            // Its equals compares the scope as a document, ignoring key order.
            return a.getCode().equals(b.getCode()) && equal(a.getScope(), b.getScope(), path, unordered);
        }
        return left.getBsonType() == right.getBsonType() && left.equals(right);
    }

    /**
     * {@code at} gives each left element's pointer: the row's own for results, the item's for an array.
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
        // A pointer inside an unordered array names one left item, so a left item may equal some right items and not
        // others: the pairing is a maximum bipartite matching, not a first match.
        int size = left.size();
        boolean[][] fits = new boolean[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                fits[i][j] = equal(left.get(i), right.get(j), at.apply(i), unordered);
            }
        }
        int[] owner = new int[size];
        Arrays.fill(owner, -1);
        for (int i = 0; i < size; i++) {
            if (!augment(i, fits, owner, new boolean[size])) {
                return false;
            }
        }
        return true;
    }

    /** Kuhn's augmenting path: pairs left item {@code i}, re-pairing earlier items when that frees a right item. */
    private static boolean augment(int i, boolean[][] fits, int[] owner, boolean[] visited) {
        for (int j = 0; j < fits.length; j++) {
            if (fits[i][j] && !visited[j]) {
                visited[j] = true;
                if (owner[j] < 0 || augment(owner[j], fits, owner, visited)) {
                    owner[j] = i;
                    return true;
                }
            }
        }
        return false;
    }

    private static String child(String path, String segment) {
        return path + "/" + segment.replace("~", "~0").replace("/", "~1");
    }
}
