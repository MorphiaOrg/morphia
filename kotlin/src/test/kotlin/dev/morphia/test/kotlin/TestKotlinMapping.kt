package dev.morphia.test.kotlin

import dev.morphia.config.ManualMorphiaConfig.configure
import dev.morphia.mapping.CritterMapper
import dev.morphia.mapping.MapperType
import dev.morphia.mapping.ReflectiveMapper
import dev.morphia.test.TestBase
import dev.morphia.test.kotlin.models.DelegatedNull
import dev.morphia.test.kotlin.models.MyClass
import dev.morphia.test.kotlin.models.VersionedDataClass
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

open class TestKotlinMapping :
    TestBase(configure().packages(listOf(DelegatedNull::class.java.packageName))) {
    @Test
    open fun dataClasses() {
        val myClass = ds.save(MyClass(ObjectId(), 42))
        val loaded = ds.find(MyClass::class.java).first()

        assertEquals(myClass, loaded)
    }

    @Test
    fun versioning() {
        val versioned = ds.save(VersionedDataClass(null, "temp"))
        val loaded = ds.find(VersionedDataClass::class.java).first()

        assertEquals(versioned, loaded)
        assertEquals(1L, loaded?.version)
    }

    @Test
    fun delegated() {
        val delegated = DelegatedNull()
        delegated.status = "I'm all set"
        ds.save(delegated)
        val first = ds.find(DelegatedNull::class.java).first()

        assertNotNull(first)
        assertEquals(delegated.status, first?.status)
    }

    @Test
    fun delegatedMappedOnce() {
        // a fresh mapper, not a copy: copying a model used to hide a property registered under two
        // names
        val config = ds.mapper.config
        val mapper =
            if (config.mapper() == MapperType.CRITTER) CritterMapper(config)
            else ReflectiveMapper(config)
        val model = mapper.getEntityModel(DelegatedNull::class.java)

        assertEquals(listOf("id", "status"), model.properties.map { it.name })
        assertEquals(listOf("_id", "status"), model.properties.map { it.mappedName })

        val delegated = DelegatedNull()
        delegated.status = "once"
        ds.save(delegated)
        val document = ds.database.getCollection(model.collectionName()).find().first() as Document
        assertEquals(setOf("_id", "_t", "status"), document.keys)
    }
}
