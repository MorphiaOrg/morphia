package taglets;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

import java.util.Map;

public class InlineTaglet extends DocTaglet {
    /**
     * Register this Taglet.
     *
     * @param tagletMap the map to register this tag to.
     */
    public static void register(Map<String, Taglet> tagletMap) {
        InlineTaglet tag = new InlineTaglet();
        tagletMap.put(tag.getName(), tag);
    }

    @Override
    public String getName() {
        return "morphia.inline";
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
        return "Inline this method to update to the new usage";
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
