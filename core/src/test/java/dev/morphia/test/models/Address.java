package dev.morphia.test.models;

import java.io.Serializable;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Transient;

@Entity
public class Address implements Serializable {

    @Property("address_street")
    private String street;

    @Property
    private String postCode;

    @Transient
    private String secretWord;

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getSecretWord() {
        return secretWord;
    }

    public void setSecretWord(String secretWord) {
        this.secretWord = secretWord;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }
}
