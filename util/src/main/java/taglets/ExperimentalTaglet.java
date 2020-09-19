package taglets;

import com.sun.source.doctree.DocTree;

import javax.lang.model.element.Element;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ExperimentalTaglet extends DocTaglet {
    @Override
    public Set<Location> getAllowedLocations() {
        return EnumSet.allOf(Location.class);
    }

    @Override
    public String getName() {
        return "morphia.experimental";
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        if (tags.isEmpty()) {
            return null;
        }

        String text = "<div class=\"deprecationBlock\"><span class=\"deprecatedLabel\">%s</span>"
            + " <div class=\"deprecationComment\">%s</div> </div>";
        return String.format(text, getHeader(), getMessage());
    }

    private String getMessage() {
        return "This is an experimental item.  Its function and presence are subject to change.  Feedback on features and usability "
               + "extremely welcome.";
    }

    @Override
    protected String getHeader() {
        return "Developer note.";
    }

    @Override
    protected String getBaseDocURI() {
        return null;
    }
}
