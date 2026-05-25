package dev.morphia.test.kotlin

import dev.morphia.mapping.codec.pojo.TypeData
import dev.morphia.test.TestBase
import dev.morphia.test.kotlin.models.DelegatedNull
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField
import org.junit.jupiter.api.Test

class KotlinTypeDataTest : TestBase() {
    @Test
    fun testSubtypes() {
        withConfig(
            buildConfig(DelegatedNull::class.java),
            {
                try {
                    typeData(DelegatedNull::class, "status", ReadWriteProperty::class.java)
                } catch (e: ReflectiveOperationException) {
                    throw RuntimeException(e)
                }
            },
        )
    }

    @Throws(NoSuchFieldException::class)
    private fun typeData(
        owner: KClass<*>,
        propertyName: String,
        fieldType: Class<*>?,
        vararg parameterTypes: Class<*>,
    ) {
        val field = owner.declaredMemberProperties.first { it.name == propertyName }
        val typeData = TypeData.builder(field.javaField?.type).build()

        //        val typeData = TypeData.get(field)
        //        Assertions.assertEquals(fieldType, typeData.type)
        //        val typeParameters = typeData.typeParameters
        //        Assertions.assertEquals(parameterTypes.size, typeParameters.size)
        //        for (i in parameterTypes.indices) {
        //            Assertions.assertEquals(parameterTypes[i], typeParameters[i].type)
        //        }
    }
}
