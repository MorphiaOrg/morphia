package dev.morphia.critter.model

sealed class CritterClass(val name: String) {
    override fun toString(): String {
        return "CritterClass(name='$name')"
    }
}

