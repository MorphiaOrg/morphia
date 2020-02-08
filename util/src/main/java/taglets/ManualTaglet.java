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
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import jdk.javadoc.doclet.Taglet;

import javax.lang.model.element.Element;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static jdk.javadoc.doclet.Taglet.Location.CONSTRUCTOR;
import static jdk.javadoc.doclet.Taglet.Location.FIELD;
import static jdk.javadoc.doclet.Taglet.Location.METHOD;
import static jdk.javadoc.doclet.Taglet.Location.OVERVIEW;
import static jdk.javadoc.doclet.Taglet.Location.PACKAGE;
import static jdk.javadoc.doclet.Taglet.Location.TYPE;

/**
 * Provides a taglet for linking to the MongoDB manual pages
 *
 * @see <a href="http://docs.mongodb.org/manual/">the MongoDB manual</a>
 */
public class ManualTaglet extends DocTaglet {

    @Override
    public Set<Location> getAllowedLocations() {
        return new HashSet<>(asList(CONSTRUCTOR, METHOD, FIELD, PACKAGE));
    }

    @Override
    public String getName() {
        return "mongodb.driver.manual";
    }

    @Override
    protected String getHeader() {
        return "MongoDB documentation";
    }

    @Override
    protected String getBaseDocURI() {
        return "http://docs.mongodb.org/manual/";
    }

}

