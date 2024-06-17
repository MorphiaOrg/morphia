package dev.morphia.mapping.codec;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.mongodb.lang.Nullable;

import org.bson.BsonReader;
import org.bson.BsonRegularExpression;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Morphia-local codec based on code from the Java Driver.
 * 
 * @hidden
 */
public class PatternCodec implements Codec<Pattern> {
    @Override
    public Pattern decode(BsonReader reader, DecoderContext decoderContext) {
        BsonRegularExpression regularExpression = reader.readRegularExpression();
        return Pattern.compile(regularExpression.getPattern(), getOptionsAsInt(regularExpression));
    }

    @Override
    public void encode(BsonWriter writer, Pattern value, EncoderContext encoderContext) {
        writer.writeString("/%s/%s".formatted(value.pattern(), getOptionsAsString(value)));
    }

    @Override
    public Class<Pattern> getEncoderClass() {
        return Pattern.class;
    }

    private static String getOptionsAsString(final Pattern pattern) {
        int flags = pattern.flags();
        StringBuilder buf = new StringBuilder();

        for (final RegexFlag flag : RegexFlag.values()) {
            if ((pattern.flags() & flag.javaFlag) > 0) {
                buf.append(flag.flagChar);
                flags -= flag.javaFlag;
            }
        }

        if (flags > 0) {
            throw new IllegalArgumentException("some flags could not be recognized.");
        }

        return buf.toString();
    }

    private static int getOptionsAsInt(final BsonRegularExpression regularExpression) {
        int optionsInt = 0;

        String optionsString = regularExpression.getOptions();

        if (optionsString == null || optionsString.length() == 0) {
            return optionsInt;
        }

        optionsString = optionsString.toLowerCase();

        for (int i = 0; i < optionsString.length(); i++) {
            RegexFlag flag = RegexFlag.getByCharacter(optionsString.charAt(i));
            if (flag != null) {
                optionsInt |= flag.javaFlag;
                if (flag.unsupported != null) {
                    // TODO: deal with logging
                    // warnUnsupportedRegex( flag.unsupported );
                }
            } else {
                // TODO: throw a better exception here
                throw new IllegalArgumentException("unrecognized flag [" + optionsString.charAt(i) + "] " + (int) optionsString.charAt(i));
            }
        }
        return optionsInt;
    }

    private static final int GLOBAL_FLAG = 256;

    /**
     * @hidden
     */
    public enum RegexFlag {
        CANON_EQ(Pattern.CANON_EQ, 'c', "Pattern.CANON_EQ"),
        UNIX_LINES(Pattern.UNIX_LINES, 'd', "Pattern.UNIX_LINES"),
        GLOBAL(GLOBAL_FLAG, 'g', null),
        CASE_INSENSITIVE(Pattern.CASE_INSENSITIVE, 'i', null),
        MULTILINE(Pattern.MULTILINE, 'm', null),
        DOTALL(Pattern.DOTALL, 's', "Pattern.DOTALL"),
        LITERAL(Pattern.LITERAL, 't', "Pattern.LITERAL"),
        UNICODE_CASE(Pattern.UNICODE_CASE, 'u', "Pattern.UNICODE_CASE"),
        COMMENTS(Pattern.COMMENTS, 'x', null);

        private static final Map<Character, RegexFlag> BY_CHARACTER = new HashMap<>();

        public final int javaFlag;
        public final char flagChar;
        private final String unsupported;

        static {
            for (final RegexFlag flag : values()) {
                BY_CHARACTER.put(flag.flagChar, flag);
            }
        }

        public static RegexFlag getByCharacter(final char ch) {
            return BY_CHARACTER.get(ch);
        }

        RegexFlag(final int f, final char ch, @Nullable final String u) {
            javaFlag = f;
            flagChar = ch;
            unsupported = u;
        }
    }

}
