package com.mongodb;

import java.io.Serializable;

public class DBRef implements Serializable {
	private static final long serialVersionUID = 1010362449129647598L;
    Serializable _id = null;
    String _ns;
    Serializable _db = null;

    protected DBRef() {}

    public DBRef(Serializable db, String coll, Serializable id) {
    	_db = db; _ns = coll; _id = id;
    }
    
    
    public Serializable getId() {
        return _id;
    }

    public String getRef() {
        return _ns;
    }

    public Serializable getDB() {
        return _db;
    }
}
