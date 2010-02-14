package com.google.code.morphia.testmodel;

import com.google.code.morphia.annotations.MongoEmbedded;
import java.io.Serializable;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
@MongoEmbedded
public class Translation implements Serializable {

    private String title;
    private String body;

    public Translation() {
    }

    public Translation( String title, String body ) {
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
