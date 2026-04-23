package dev.morphia.critter.it.kotlin

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id

@Entity
class CompanionEntity {
    @Id
    private var id: String = ""

    companion object {
        const val ENTITY_NAME = "CompanionEntity"
    }
}
