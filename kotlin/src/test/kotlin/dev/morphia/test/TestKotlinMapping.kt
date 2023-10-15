package dev.morphia.test

import dev.morphia.test.models.DelegatedNull
import dev.morphia.test.models.MyClass
import dev.morphia.test.models.VersionedDataClass
import org.bson.types.ObjectId
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotNull
import org.testng.annotations.Test

@Test
open class TestKotlinMapping : TestBase() {
    @Test
    open fun dataClasses() {
        withTestConfig(listOf(MyClass::class.java)) {
            val myClass = MyClass(ObjectId(), 42)
            ds.save(myClass)
            val loaded = ds.find(MyClass::class.java).first()

            assertEquals(loaded, myClass)
        }
    }

    @Test
    fun versioning() {
        withTestConfig(listOf(VersionedDataClass::class.java)) {
            val versioned = VersionedDataClass(null, "temp")
            ds.save(versioned)
            val loaded = ds.find(VersionedDataClass::class.java).first()

            assertEquals(loaded, versioned)
            assertEquals(loaded?.version, 1L)
        }
    }

    @Test
    fun delegated() {
        withTestConfig(listOf(DelegatedNull::class.java)) {
            val delegated = DelegatedNull()
            delegated.status = "I'm all set"
            ds.save(delegated)
            val first = ds.find(DelegatedNull::class.java).first()

            assertNotNull(first)
            assertEquals(first?.status, delegated.status)
        }
    }
}
