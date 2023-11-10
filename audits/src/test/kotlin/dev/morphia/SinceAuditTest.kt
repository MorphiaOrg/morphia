package dev.morphia

import dev.morphia.audits.SinceAudit
import dev.morphia.audits.model.MorphiaMethod
import dev.morphia.audits.model.State
import dev.morphia.audits.model.Version
import org.testng.Assert

class SinceAuditTest {
    fun deprecations() {
        val since = SinceAudit()

        since.processApis()
        val method =
            since.methodHistory[
                    "dev.morphia.query.LegacyQuery#execute()Ldev/morphia/query/internal/MorphiaCursor;"]

        method as MorphiaMethod
        Assert.assertEquals(State.DEPRECATED, method.versions[Version.v2_1_0_SNAPSHOT])
        Assert.assertEquals(State.ABSENT, method.versions[Version.v1_6_0_SNAPSHOT])
    }
}
