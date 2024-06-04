package dev.morphia.critter.conventions

import dev.morphia.mapping.Mapper
import io.github.dmlloyd.classfile.ClassBuilder
import io.github.dmlloyd.classfile.ClassModel

interface CritterConvention {
    fun apply(mapper: Mapper, builder: ClassBuilder, model: ClassModel)
}
