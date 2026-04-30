package dev.morphia.critter.it.kotlin

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id

@Entity
class PropertyEntity {
    @Id
    private var id: String = ""
    private var firstName: String = ""
    private var lastName: String = ""

    val fullName get() = "$firstName $lastName"
}
