/*
 *  Copyright 2010 Olafur Gauti Gudmundsson.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package com.google.code.morphia;

import java.util.Iterator;
import java.util.List;

/**
 * Represents results returned from a find() request.
 * Values can be iterated through, or transformed to a java.util.List.
 *
 * @author Olafur Gauti Gudmundsson
 */
public interface Results<T> extends Iterable<T>, Iterator<T> {

    /**
     * Takes all the values in the results and adds to a List.
     *
     * @return a List containing all the results
     */
    public List<T> asList();
}
