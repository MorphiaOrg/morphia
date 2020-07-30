/*
 * Copyright (c) 2008-2016 MongoDB, Inc.
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

package dev.morphia.aggregation.zipcode;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Property;

@Embedded
public class CityPopulation {
    @Property("name")
    private String name;
    @Property("pop")
    private Long population;

    public String getName() {
        return name;
    }

    public Long getPopulation() {
        return population;
    }

    @Override
    public String toString() {
        return String.format("CityPopulation{name='%s', population=%d}", name, population);
    }
}
