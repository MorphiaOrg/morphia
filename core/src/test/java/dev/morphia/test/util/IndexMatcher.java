package dev.morphia.test.util;

import org.bson.Document;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.List;

import static java.lang.String.format;
import static org.testng.Assert.fail;

/**
 * Hamcrest matcher that can be used with a List of Documents representing the indexes on a Collection.  This list, for example, might be
 * from a call like {@code getDs().getCollection(Entity.class).getIndexInfo()}
 */
public final class IndexMatcher extends TypeSafeMatcher<List<Document>> {
    private final String indexName;
    private final boolean indexShouldBePresent;

    private IndexMatcher(String indexName, boolean indexShouldBePresent) {
        this.indexName = indexName;
        this.indexShouldBePresent = indexShouldBePresent;
    }

    /**
     * Use this matcher to determine if a list of Documents representing the indexes of a Collection does not contain an index with the
     * given name.
     *
     * @param indexName the expected name of the index
     * @return a Matcher that will fail if a list of Documents contains an index with the given name
     */
    public static Matcher<? super List<Document>> doesNotHaveIndexNamed(String indexName) {
        return new IndexMatcher(indexName, false);
    }

    /**
     * Use this matcher to determine if a list of Documents representing the indexes of a Collection contains an index with the given name.
     *
     * @param indexName the expected name of the index
     * @return a Matcher that will match if a list of Documents contains an index with the given name
     */
    public static Matcher<? super List<Document>> hasIndexNamed(String indexName) {
        return new IndexMatcher(indexName, true);
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(indexName);
    }

    @Override
    protected boolean matchesSafely(List<Document> indexes) {
        boolean indexFound = false;
        for (Document document : indexes) {
            if (document.get("name").equals(indexName)) {
                indexFound = true;
            }
        }
        return (indexFound && indexShouldBePresent) // index is there and it SHOULD be there
               || (!indexFound && !indexShouldBePresent); // index is not there and it SHOULD NOT be there
    }

    @Override
    protected void describeMismatchSafely(List<Document> indexes, Description mismatchDescription) {
        fail(format("Expected %s to find index with name '%s' in %s", indexShouldBePresent ? "" : "not", indexName, indexes));
    }
}
