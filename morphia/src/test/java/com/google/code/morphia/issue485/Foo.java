package com.google.code.morphia.issue485;

import com.google.code.morphia.annotations.Entity;

@Entity("abstract_test")
public class Foo extends BaseFoo {
	private int value;

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
}
