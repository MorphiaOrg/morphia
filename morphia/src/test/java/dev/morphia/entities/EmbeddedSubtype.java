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

package dev.morphia.entities;

public class EmbeddedSubtype extends EmbeddedType {
    private Boolean flag;

    public Boolean getFlag() {
        return flag;
    }

    public void setFlag(final Boolean flag) {
        this.flag = flag;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmbeddedSubtype)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final EmbeddedSubtype that = (EmbeddedSubtype) o;

        return flag != null ? flag.equals(that.flag) : that.flag == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (flag != null ? flag.hashCode() : 0);
        return result;
    }
}
