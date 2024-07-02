package dev.morphia.critter.sources

class KotlinDummyEntity {
    private var name = "DummyEntity"
    private var age = 21
    private var salary: Long? = 2L

    override fun toString(): String {
        return "KotlinDummyEntity(name='$name', age=$age, salary=$salary)"
    }
}
