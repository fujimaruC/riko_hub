package id.riko.hub.data.remote.device

import java.net.Inet4Address
import java.net.InetAddress
import java.util.UUID

data class PairingPayload(val laptopId: String, val pin: String, val secret: String)

fun parsePairingPayload(value: String): PairingPayload? {
    val parts = value.trim().split(':')
    if (parts.size != 4 || parts[0] != "riko1" || !isUuid(parts[1]) ||
        !parts[2].matches(Regex("[0-9a-f]{64}")) || !parts[3].matches(Regex("[0-9a-f]{64}"))) return null
    return PairingPayload(parts[1], parts[2], parts[3])
}

fun isUuid(value: String) = runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)

fun isPrivateIpv4(address: InetAddress): Boolean {
    if (address !is Inet4Address) return false
    val b = address.address.map { it.toInt() and 255 }
    return b[0] == 10 || (b[0] == 172 && b[1] in 16..31) || (b[0] == 192 && b[1] == 168)
}

fun retryDelaySeconds(failures: Int): Long = when (failures.coerceAtLeast(1)) {
    1 -> 2; 2 -> 4; 3 -> 8; 4 -> 16; 5 -> 32; else -> 60
}
