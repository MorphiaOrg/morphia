package dev.morphia.test.models;

import dev.morphia.annotations.*;
import org.bson.types.ObjectId;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

@Entity
public class ModelWithSqlDates {

    @Id
    private ObjectId id;

    @Property
    private Timestamp timestamp;

    @Property
    private java.sql.Date sqlDate;

    @Property
    private java.sql.Time sqlTime;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Date getSqlDate() {
        return sqlDate;
    }

    public void setSqlDate(Date sqlDate) {
        this.sqlDate = sqlDate;
    }

    public Time getSqlTime() {
        return sqlTime;
    }

    public void setSqlTime(Time sqlTime) {
        this.sqlTime = sqlTime;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

}
