package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity
public class UsesCustomIdObject {
    @Id
    private CustomId id;
    private String text;

    public UsesCustomIdObject() {
    }

    public UsesCustomIdObject(CustomId id, String text) {
        this.id = id;
        this.text = text;
    }

    public CustomId getId() {
        return id;
    }

    public void setId(CustomId id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
