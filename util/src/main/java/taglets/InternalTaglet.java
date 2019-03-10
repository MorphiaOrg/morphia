package taglets;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

import java.util.Map;

public class InternalTaglet extends DocTaglet {
    public static void register(Map<String, Taglet> tagletMap) {
        InternalTaglet tag = new InternalTaglet();
        tagletMap.put(tag.getName(), tag);
    }

    @Override
    public String getName() {
        return "morphia.internal";
    }

    @Override
    public String toString(final Tag[] tags) {
        if (tags.length == 0) {
            return null;
        }

        String text = "<div class=\"block\"><span class=\"deprecatedLabel\">%s.</span>&nbsp;<span "
                   + "class=\"deprecationComment\">%s</span></div>";

        return String.format(text, getHeader(), getMessage());
    }

    private String getMessage() {
        return "This is an internal item.  Its function and presence are subject to change without warning.  Its use is highly "
               + "discouraged.";
    }

    @Override
    protected String getHeader() {
        return "Developer note";
    }

    @Override
    protected String getBaseDocURI() {
        return null;
    }
}
