package dev.morphia.test.kotlin.models

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id
import kotlin.properties.Delegates
import org.bson.types.ObjectId

@Entity
class DelegatedNull {
    @Id lateinit var id: ObjectId
    var status: String by Delegates.notNull()
}
