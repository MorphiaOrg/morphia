/*
 * Copyright 2016 MongoDB, Inc.
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

package dev.morphia.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IndexTypeTest {
    @Test
    public void fromValue() {
        assertEquals(IndexType.ASC, IndexType.fromValue(1));

        assertEquals(IndexType.DESC, IndexType.fromValue(-1));

        assertEquals(IndexType.GEO2D, IndexType.fromValue("2d"));

        assertEquals(IndexType.GEO2DSPHERE, IndexType.fromValue("2dsphere"));

        assertEquals(IndexType.HASHED, IndexType.fromValue("hashed"));

        assertEquals(IndexType.TEXT, IndexType.fromValue("text"));
    }
}
