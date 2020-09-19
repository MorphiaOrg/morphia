package dev.morphia.testmodel;


import dev.morphia.annotations.Embedded;

import java.io.Serializable;


@Embedded
public class Translation implements Serializable {
    private String title;
    private String body;

    public Translation() {
    }

    public Translation(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
