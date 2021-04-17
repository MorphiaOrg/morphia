package dev.morphia.test.models

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id
import org.bson.types.ObjectId

@Entity
data class MyClass(@Id val id: ObjectId, val value: Int = 0)
