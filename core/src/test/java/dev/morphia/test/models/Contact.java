package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Validation;
import org.bson.types.ObjectId;

import static com.mongodb.client.model.ValidationLevel.MODERATE;

@Entity("contacts")
@Validation(value = "{$jsonSchema: {" +
                    "    bsonType: 'object'," +
                    "    required: ['phone', 'name']," +
                    "    properties: {" +
                    "      phone: {" +
                    "        bsonType: 'string'," +
                    "        description: 'must be a string and is required'" +
                    "      }," +
                    "      name: {" +
                    "        bsonType: 'string'," +
                    "        description: 'must be a string and is required'" +
                    "      }" +
                    "    }" +
                    "  }" +
                    "}",
    level = MODERATE)
public class Contact {
    @Id
    ObjectId _id;
    String name;
    String phone;
    String city;
    String status;
}
