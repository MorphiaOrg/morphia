package dev.morphia.critter.sources

class KotlinDummyEntity {
    private var name = "DummyEntity"
    private var age = 21

    override fun toString(): String {
        return "KotlinDummyEntity(name='$name', age=$age)"
    }
}
