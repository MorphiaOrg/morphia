package dev.morphia.test.validation;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

import jakarta.validation.constraints.Email;

@Entity
public class Data {
    @Id
    ObjectId id;
    @Email
    String email;
}
