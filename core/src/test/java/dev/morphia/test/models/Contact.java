package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Validation;
import org.bson.types.ObjectId;

import static com.mongodb.client.model.ValidationLevel.MODERATE;

@Entity(value = "contacts", useDiscriminator = false)
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
                    "      }," +
                    "      age: {" +
                    "        bsonType: 'int'," +
                    "        minimum: 18," +
                    "        maximum: 35," +
                    "        description: 'must be an integer in [ 18, 35 ] and is required'" +
                    "      }" +
                    "    }" +
                    "  }" +
                    "}",
    level = MODERATE)
public class Contact {
    @Id
    ObjectId _id;
    String name;
    int age;
    String phone;
    String city;
    String status;
}
