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

import com.sun.tools.doclets.Taglet;

import java.util.Map;

/**
 * Provides a taglet for linking to the MongoDB manual pages
 * 
 * @see <a href="http://docs.mongodb.org/manual/">the MongoDB manual</a>
 */
public class ManualTaglet extends DocTaglet {

    /**
     * Registers this taglet with the javadoc runtime
     *
     * @param tagletMap the map of taglets
     */
    public static void register(final Map<String, Taglet> tagletMap) {
        ManualTaglet t = new ManualTaglet();
        tagletMap.put(t.getName(), t);
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
        return "http://docs.mongodb.org/manual";
    }

}
