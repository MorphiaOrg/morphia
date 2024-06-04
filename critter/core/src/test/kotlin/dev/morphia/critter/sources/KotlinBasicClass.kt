package dev.morphia.critter.sources

import dev.morphia.annotations.Entity

@Entity(value = "basicKotlin ", useDiscriminator = false)
class KotlinBasicClass {
    var count = 42
}
