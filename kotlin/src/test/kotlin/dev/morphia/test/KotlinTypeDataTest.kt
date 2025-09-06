package dev.morphia.test

import dev.morphia.mapping.codec.pojo.TypeData
import dev.morphia.test.models.DelegatedNull
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField
import org.testng.annotations.Test

class KotlinTypeDataTest : TestBase() {
    @Test
    fun testSubtypes() {
        withConfig(
            MorphiaTestSetup.buildConfig(DelegatedNull::class.java),
            Runnable {
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

        //        val typeData = TypeData.newInstance(field)
        //        Assert.assertEquals(typeData.type, fieldType)
        //        val typeParameters = typeData.typeParameters
        //        Assert.assertEquals(typeParameters.size, parameterTypes.size)
        //        for (i in parameterTypes.indices) {
        //            Assert.assertEquals(typeParameters[i].type, parameterTypes[i])
        //        }
    }
}
