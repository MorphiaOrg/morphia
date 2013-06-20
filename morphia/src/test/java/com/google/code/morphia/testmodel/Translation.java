package com.google.code.morphia.testmodel;


import java.io.Serializable;

import com.google.code.morphia.annotations.Embedded;


/**
 * @author Olafur Gauti Gudmundsson
 */
@Embedded
public class Translation implements Serializable {
  private static final long serialVersionUID = 1L;

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
