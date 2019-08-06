package taglets;

import com.sun.source.doctree.DocTree;

import javax.lang.model.element.Element;
import java.util.List;
import java.util.Set;

import static jdk.javadoc.doclet.Taglet.Location.METHOD;

public class InlineTaglet extends DocTaglet {

    @Override
    public Set<Location> getAllowedLocations() {
        return Set.of(METHOD);
    }

    @Override
    public String getName() {
        return "morphia.inline";
    }

    @Override
    public String toString(final List<? extends DocTree> tags, final Element element) {
        if (tags.isEmpty()) {
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
