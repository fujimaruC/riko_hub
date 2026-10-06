package id.riko.hub.core.time

import java.time.Instant
import java.time.ZoneId

interface Clock {
    fun now(): Long
    fun zone(): ZoneId
}

object SystemClock : Clock {
    override fun now() = Instant.now().toEpochMilli()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
