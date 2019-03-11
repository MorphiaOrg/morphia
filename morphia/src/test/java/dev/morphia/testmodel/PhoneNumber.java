/*
  Copyright (C) 2010 Olafur Gauti Gudmundsson
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
  obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
  and limitations under the License.
 */


package dev.morphia.testmodel;


import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Property;

import java.io.Serializable;


/**
 * @author Olafur Gauti Gudmundsson
 */
@Embedded
public class PhoneNumber implements Serializable {

    @Property
    private int countryCode;
    @Property
    private int localExtension;
    @Property
    private Type type;

    public PhoneNumber() {
        type = Type.PHONE;
    }

    public PhoneNumber(final int countryCode, final int localExtension, final Type type) {
        this.countryCode = countryCode;
        this.localExtension = localExtension;
        this.type = type;
    }

    public int getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(final int countryCode) {
        this.countryCode = countryCode;
    }

    public int getLocalExtension() {
        return localExtension;
    }

    public void setLocalExtension(final int localExtension) {
        this.localExtension = localExtension;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + countryCode;
        hash = 43 * hash + localExtension;
        hash = 43 * hash + type.hashCode();
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PhoneNumber other = (PhoneNumber) obj;
        if (countryCode != other.countryCode) {
            return false;
        }
        if (localExtension != other.localExtension) {
            return false;
        }
        return type == other.type;
    }

    public enum Type {
        PHONE,
        FAX
    }

}
