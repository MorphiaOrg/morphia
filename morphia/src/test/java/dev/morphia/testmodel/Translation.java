package dev.morphia.testmodel;


import dev.morphia.annotations.Embedded;

import java.io.Serializable;


/**
 * @author Olafur Gauti Gudmundsson
 */
@Embedded
public class Translation implements Serializable {
    private String title;
    private String body;

    public Translation() {
    }

    public Translation(final String title, final String body) {
        this.title = title;
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

}
