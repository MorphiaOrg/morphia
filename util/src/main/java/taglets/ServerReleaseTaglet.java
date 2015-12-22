/*
 * Copyright (c) 2008-2015 MongoDB, Inc.
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
 * Provides a taglet for annotation minimum server version support for a feature.
 */
public class ServerReleaseTaglet extends DocTaglet {

    /**
     * Registers this taglet with the javadoc runtime
     *
     * @param tagletMap the map of taglets
     */
    public static void register(final Map<String, Taglet> tagletMap) {
        Taglet t = new taglets.ServerReleaseTaglet();
        tagletMap.put(t.getName(), t);
    }

    @Override
    public String getName() {
        return "mongodb.server.release";
    }

    @Override
    protected String getHeader() {
        return "Since server release";
    }

    @Override
    protected String getBaseDocURI() {
        return "http://docs.mongodb.org/manual/release-notes/";
    }
}
