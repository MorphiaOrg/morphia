package dev.morphia.test.models

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id
import dev.morphia.annotations.Version
import org.bson.types.ObjectId

@Entity
data class VersionedDataClass(@Id val id: ObjectId?, val name: String, @Version val version: Long = 0)
