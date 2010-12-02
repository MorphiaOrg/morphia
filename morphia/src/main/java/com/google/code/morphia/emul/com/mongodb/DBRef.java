package com.mongodb;

import java.io.Serializable;

public class DBRef implements Serializable {
	private static final long serialVersionUID = 1010362449129647598L;
	byte[] data;
	
	DBRef(byte[] data){
		this.data = data;
	}
}
