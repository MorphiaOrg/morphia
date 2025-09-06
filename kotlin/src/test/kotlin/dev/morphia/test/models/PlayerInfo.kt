package dev.morphia.test.models

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id
import java.util.UUID

@Entity(value = "playerinfo", useDiscriminator = false)
data class PlayerInfo(
    @Id val uuid: UUID,
    val firstJoin: Long = System.currentTimeMillis(),
    var lastJoin: Long = System.currentTimeMillis(),
)
