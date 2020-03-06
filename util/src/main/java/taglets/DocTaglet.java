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

import com.sun.source.doctree.DocTree;
import jdk.javadoc.doclet.Taglet;

import javax.lang.model.element.Element;
import java.util.List;
import java.util.Set;

/**
 * Provides a general base class for Morphia taglets
 */
public abstract class DocTaglet implements Taglet {

    @Override
    public boolean isInlineTag() {
        return false;
    }

    @Override
    public Set<Location> getAllowedLocations() {
        return Set.of(Location.TYPE, Location.METHOD, Location.FIELD);
    }

    @Override
    public String toString(final List<? extends DocTree> tags, final Element element) {
        if (tags.isEmpty()) {
            return null;
        }

        StringBuilder buf = new StringBuilder(String.format("\n<dl><dt><span class=\"strong\">%s</span></dt>\n", getHeader()));
        for (DocTree t : tags) {
            buf.append("   <dd>").append(genLink(t.toString())).append("</dd>\n");
        }
        return buf.toString();
    }

    protected String genLink(final String text) {
        String relativePath = text;
        String display = text;

        int firstSpace = text.indexOf(' ');
        if (firstSpace != -1) {
            relativePath = text.substring(0, firstSpace);
            display = text.substring(firstSpace).trim();
        }

        return String.format("<a href='%s%s'>%s</a>", getBaseDocURI(), relativePath, display);
    }

    protected abstract String getHeader();

    protected abstract String getBaseDocURI();
}
