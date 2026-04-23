package dev.morphia.critter.it.kotlin

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id

@Entity
data class KotlinEntity(
    @Id private var id: String = "",
    private var name: String = "",
    private var count: Int = 0,
    private var description: String? = null
)
