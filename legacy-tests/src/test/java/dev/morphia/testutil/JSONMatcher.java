package dev.morphia.testutil;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.junit.Assert;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONParser;

/**
 * Matcher that allows comparison between Strings as JSON Strings.  This lets you be a bit more flexible when comparing JSON strings,
 * instead of having to compare String to String with exact matches (including unnecessary spaces). So instead of having to specify every
 * quotation and exact spacing like this:
 * <pre>
 * {@code "{ \"fieldName\" : { \"anotherName\" : 99}}"}
 * </pre>
 * you can pass in the expected value as a String and have the JSON parser compare it with a valid JSON value. For example:
 * <pre>
 * {@code assertThat("{'keywords':{'$size':4}}", jsonEqual("{ keywords: { $size: 4 } }")); }
 * </pre>
 * will pass.
 * <p/>
 * Note that if either the expected JSON or actual JSON values are invalid JSON strings, the matcher will fail, but not throw an Exception.
 * The underlying JSON parse exception will be displayed in the failure message.
 */
public class JSONMatcher extends TypeSafeMatcher<String> {
    private final String expectedJson;

    /**
     * Create a new instance of the matcher with the given String as the expected JSON value to match.
     *
     * @param expectedJson a String containing valid JSON that represents the expected value to compare against.
     */
    public JSONMatcher(final String expectedJson) {
        this.expectedJson = expectedJson;
    }

    /**
     * Static helper to return a Matcher that compares the given String as a JSON value.
     *
     * @param expectedValue the expected JSON value as a String.  Things like quotation marks are optional on field names
     * @return a JSONMatcher that will can be used to compare this expected value with an actual value.
     */
    public static Matcher<String> jsonEqual(final String expectedValue) {
        return new JSONMatcher(expectedValue);
    }

    @Override
    public void describeTo(final Description description) {
        try {
            Object expectedJsonAsPrettyJson = JSONParser.parseJSON(expectedJson);
            description.appendValue(expectedJsonAsPrettyJson);
        } catch (final JSONException e) {
            throw new RuntimeException(String.format("Error parsing expected JSON string %s. Got Exception %s",
                                                     expectedJson, e.toString()));
        }
    }

    /**
     * Compares the item (which represents the actual result) against the expected value to see if they are the same JSON.  These two
     * values
     * may not be equal as Strings, but the JSON comparison should ignore unnecessary whitespace and the differences between " and '.
     *
     * @param item a String containing valid JSON to compare against the expected value
     * @return true if the expected and actual values are equivalent JSON values
     */
    @Override
    public boolean matchesSafely(final String item) {
        try {
            return JSONCompare.compareJSON(expectedJson, item, JSONCompareMode.STRICT).passed();
        } catch (final JSONException e) {
            Assert.fail(String.format("JSON compare threw exception when trying to compare %n %s %n with %n %s%n Exception was: %n%s ",
                                      expectedJson, item, e));
            return false;
        }
    }

    @Override
    protected void describeMismatchSafely(final String item, final Description mismatchDescription) {
        try {
            Object itemAsPrettyJson = JSONParser.parseJSON(item);
            mismatchDescription.appendText("was ").appendValue(itemAsPrettyJson);
        } catch (final JSONException e) {
            throw new RuntimeException(String.format("Error parsing expected JSON string %s. Got Exception %s",
                                                     item, e.toString()));
        }
    }
}
