package dev.morphia.query.filters;

import java.util.regex.Pattern;

import dev.morphia.annotations.internal.MorphiaInternal;

import static dev.morphia.mapping.codec.PatternCodec.RegexFlag.getByCharacter;

/**
 * Defines a regular expression filter
 *
 * @since 2.0
 */
@SuppressWarnings("unused")
public class RegexFilter extends Filter {

    Pattern pattern;

    /**
     * @param field   the field
     * @param pattern the pattern
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    RegexFilter(String field, Pattern pattern) {
        super("$regex", field, null);
        this.pattern = pattern;
    }

    /**
     * @hidden
     */
    public Pattern pattern() {
        return pattern;
    }

    /**
     * Optional options to apply to the regex
     *
     * @param options the options
     * @return this
     */
    public RegexFilter options(String options) {
        for (char c : options.toCharArray()) {
            add(c);
        }
        return this;
    }

    /**
     * Case insensitivity to match upper and lower cases.
     *
     * @return this
     */
    public RegexFilter caseInsensitive() {
        add('i');
        return this;
    }

    /**
     * “Extended” capability to ignore all white space characters in the $regex pattern unless escaped or included in a character class.
     * <p>
     * Additionally, it ignores characters in-between and including an un-escaped hash/pound (#) character and the next new line, so that
     * you may include comments in complicated patterns. This only applies to data characters; white space characters may never appear
     * within special character sequences in a pattern.
     * <p>
     * The x option does not affect the handling of the VT character (i.e. code 11).
     *
     * @return this
     */
    public RegexFilter extended() {
        add('x');
        return this;
    }

    /**
     * For patterns that include anchors (i.e. ^ for the start, $ for the end), match at the beginning or end of each line for strings
     * with multiline values. Without this option, these anchors match at beginning or end of the string.
     *
     * @return this
     */
    public RegexFilter multiline() {
        add('m');
        return this;
    }

    /**
     * Allows the dot character (i.e. {@code .}) to match all characters including newline characters.
     *
     * @return this
     */
    public RegexFilter special() {
        add('s');
        return this;
    }

    private void add(char option) {
        pattern = Pattern.compile(pattern.pattern(),
                pattern.flags() | getByCharacter(option).javaFlag);
    }
}
