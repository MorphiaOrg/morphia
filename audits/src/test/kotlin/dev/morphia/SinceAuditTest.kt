package dev.morphia

import dev.morphia.model.MorphiaMethod
import dev.morphia.model.State
import dev.morphia.model.Version
import org.junit.Assert
import org.junit.Test

class SinceAuditTest {
    @Test
    fun deprecations() {
        val since = SinceAudit()

        since.processApis()
        val method =
            since.methodHistory["dev.morphia.query.LegacyQuery#execute()Ldev/morphia/query/internal/MorphiaCursor;"]

        method as MorphiaMethod
        Assert.assertEquals(State.DEPRECATED, method.versions[Version.v2_1_0_SNAPSHOT])
        Assert.assertEquals(State.ABSENT, method.versions[Version.v1_6_0_SNAPSHOT])
    }
}