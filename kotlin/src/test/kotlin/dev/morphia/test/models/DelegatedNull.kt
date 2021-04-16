package dev.morphia.test.models

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id
import org.bson.types.ObjectId
import kotlin.properties.Delegates

@Entity
class DelegatedNull {
    @Id
    lateinit var id: ObjectId
    var status: String by Delegates.notNull()
}
