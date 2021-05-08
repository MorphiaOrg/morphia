package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;

@Entity(useDiscriminator = false)
public class PhoneNumber {

    @Property
    private int countryCode;
    @Property
    private int localExtension;
    @Property
    private Type type;

    public PhoneNumber() {
        type = Type.PHONE;
    }

    public PhoneNumber(int countryCode, int localExtension, Type type) {
        this.countryCode = countryCode;
        this.localExtension = localExtension;
        this.type = type;
    }

    public int getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(int countryCode) {
        this.countryCode = countryCode;
    }

    public int getLocalExtension() {
        return localExtension;
    }

    public void setLocalExtension(int localExtension) {
        this.localExtension = localExtension;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
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
    public boolean equals(Object obj) {
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
