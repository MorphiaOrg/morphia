package dev.morphia.audits

import dev.morphia.audits.model.MorphiaMethod
import dev.morphia.audits.model.State
import dev.morphia.audits.model.Version
import org.junit.jupiter.api.Assertions

class SinceAuditTest {
    fun deprecations() {
        val since = SinceAudit()

        since.processApis()
        val method =
            since.methodHistory[
                    "dev.morphia.query.LegacyQuery#execute()Ldev/morphia/query/internal/MorphiaCursor;"]

        method as MorphiaMethod
        Assertions.assertEquals(method.versions[Version.v2_1_0_SNAPSHOT], State.DEPRECATED)
        Assertions.assertEquals(method.versions[Version.v1_6_0_SNAPSHOT], State.ABSENT)
    }
}
