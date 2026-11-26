/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package taglets;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownBlockTagTree;

/**
 * Provides a taglet for linking to the MongoDB manual pages
 *
 * @see <a href="http://docs.mongodb.org/manual/">the MongoDB manual</a>
 */
public class UpdateOperatorTaglet extends ManualTaglet {

    /** Creates a new instance. */
    public UpdateOperatorTaglet() {
    }

    @Override
    public String getName() {
        return "update.operator";
    }

    /**
     * Also allows the type, for the positional operators, which any update operator's field path can use.
     */
    @Override
    public Set<Location> getAllowedLocations() {
        Set<Location> locations = new HashSet<>(super.getAllowedLocations());
        locations.add(Location.TYPE);
        return locations;
    }

    @Override
    protected String getBaseDocURI() {
        return super.getBaseDocURI() + "reference/operator/update/";
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        if (tags.size() == 0) {
            return null;
        }

        StringBuilder buf = new StringBuilder(String.format("<dl><dt><span class=\"strong\">%s</span></dt>", getHeader()));
        for (DocTree tag : tags) {
            // The whole content, since an escaped operator such as $[&lt;identifier&gt;] parses as several trees.
            String text = ((UnknownBlockTagTree) tag).getContent().stream()
                    .map(DocTree::toString)
                    .collect(Collectors.joining())
                    .trim();
            buf.append("<dd>").append(genLink(text)).append("</dd>");
        }
        return buf.toString();
    }

    protected String genLink(String text) {
        return String.format("<a href='%s%s'>Update Operator:  %s</a>", getBaseDocURI(), page(text), text);
    }

    /**
     * @return the manual page of an operator; the positional operators' pages are named for what they do
     */
    static String page(String text) {
        if (text.equals("$")) {
            return "positional";
        }
        if (text.equals("$[]")) {
            return "positional-all";
        }
        if (text.startsWith("$[")) {
            return "positional-filtered";
        }
        return text.replace("$", "");
    }

}
