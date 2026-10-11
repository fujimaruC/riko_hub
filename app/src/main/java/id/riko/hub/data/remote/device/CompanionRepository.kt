package id.riko.hub.data.remote.device

import android.content.Context
import android.net.*
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.InetAddress
import java.net.URL
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.net.ssl.*

data class Laptop(val id: String, val name: String, val address: InetAddress, val port: Int)
data class TrustedLaptop(val id: String, val name: String, val pin: String, val token: String, val phoneId: String)
data class CompanionSnapshot(val laptops: List<Laptop> = emptyList(), val trusted: TrustedLaptop? = null,
    val connected: Boolean = false, val busy: Boolean = false, val pairing: Boolean = false, val error: String? = null, val networkReady: Boolean = false, val scanning: Boolean = false)

internal class CompanionTrustEpoch {
    @Volatile private var value = 0
    fun snapshot(): Int = value
    fun invalidate(): Int = ++value
    fun isCurrent(snapshot: Int): Boolean = snapshot == value
}

class CompanionRepository(context: Context) {
    private val app = context.applicationContext
    private val connectivity = app.getSystemService(ConnectivityManager::class.java)
    private val nsd = app.getSystemService(NsdManager::class.java)
    private val wifi = app.getSystemService(WifiManager::class.java)
    private val prefs = app.getSharedPreferences("companion_trust", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(CompanionSnapshot())
    val state = mutable.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var active = false
    private var generation = 0
    private val trustEpoch = CompanionTrustEpoch()
    private var scanEpoch = 0
    private var network: Network? = null
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var lock: WifiManager.MulticastLock? = null
    private var discovery: NsdManager.DiscoveryListener? = null
    private var resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var resolveCount = 0
    private val discovered = linkedMapOf<String, Laptop>()
    private val peerCache = mutableMapOf<String, Pair<Laptop, Long>>()
    private val serviceIds = mutableMapOf<String, String>()
    private var scanJob: Job? = null
    private var heartbeatJob: Job? = null
    private var operation: Job? = null
    @Volatile private var activeConnection: HttpsURLConnection? = null
    private var storageBlocked = false
    private var scanning = false
    private val phoneId by lazy { prefs.getString("phone_id", null) ?: UUID.randomUUID().toString().also { check(prefs.edit().putString("phone_id", it).commit()) } }

    suspend fun load() {
        val epoch = trustEpoch.snapshot()
        operation?.join()
        if (!trustEpoch.isCurrent(epoch)) return
        try {
            val trust = withContext(Dispatchers.IO) { readTrust() }
            if (trustEpoch.isCurrent(epoch)) { storageBlocked = false; mutable.value = mutable.value.copy(trusted = trust, error = null); if (active && trust != null) startHeartbeat() }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { if (trustEpoch.isCurrent(epoch)) { storageBlocked = true; mutable.value = mutable.value.copy(trusted = null, connected = false, error = "Penyimpanan kepercayaan tidak dapat dibuka. Reset kepercayaan lokal untuk memasangkan ulang.") } }
    }
    suspend fun resetLocalTrust() {
        generation++; trustEpoch.invalidate(); operation?.cancelAndJoin(); heartbeatJob?.cancelAndJoin()
        mutable.value = mutable.value.copy(connected = false)
        withContext(NonCancellable) {
            val ok = withContext(Dispatchers.IO) { prefs.edit().remove("record").commit() }
            trustEpoch.invalidate()
            if (ok) { storageBlocked = false; mutable.value = mutable.value.copy(trusted = null, connected = false, error = null) }
            else mutable.value = mutable.value.copy(error = "Kepercayaan lokal gagal dihapus. Coba lagi.")
        }
    }

    @Synchronized fun setActive(value: Boolean) {
        if (active == value) return
        active = value; generation++
        if (value) {
            try {
                val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
                val cb = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(n: Network) { val owner = this; scope.launch { if (active && callback === owner) { generation++; operation?.cancel(); heartbeatJob?.cancel(); peerCache.clear(); network = n; mutable.value = mutable.value.copy(networkReady = true, connected = false, busy = false, pairing = false); startScan(); if (mutable.value.trusted != null) startHeartbeat() } } }
                    override fun onLost(n: Network) { val owner = this; scope.launch { if (active && callback === owner && network == n) { generation++; operation?.cancel(); heartbeatJob?.cancel(); activeConnection?.disconnect(); activeConnection = null; network = null; peerCache.clear(); stopScan(); mutable.value = mutable.value.copy(networkReady = false, connected = false, busy = false, pairing = false) } } }
                }
                callback = cb; connectivity.registerNetworkCallback(request, cb)
            } catch (_: SecurityException) { active = false; mutable.value = mutable.value.copy(error = "Izinkan akses jaringan lokal dan Wi-Fi untuk menemukan laptop.") }
        } else {
            generation++; stopScan(); heartbeatJob?.cancel(); operation?.cancel(); activeConnection?.disconnect(); activeConnection = null
            callback?.let { runCatching { connectivity.unregisterNetworkCallback(it) } }; callback = null
            lock?.let { runCatching { if (it.isHeld) it.release() } }; lock = null
            val t = mutable.value.trusted; val n = network
            if (t != null && n != null) scope.launch { runCatching { request(t, n, "POST", "/v1/disconnect", JSONObject().put("protocol", 1)) } }
            network = null; mutable.value = mutable.value.copy(connected = false, networkReady = false, laptops = emptyList(), busy = false, pairing = false)
        }
    }

    fun refresh() { if (active) startScan() }

    private fun startScan() {
        stopScan(); if (!active || network == null) return
        try { if (Build.VERSION.SDK_INT >= 26) lock = wifi.createMulticastLock("riko-companion").apply { setReferenceCounted(false); acquire() } }
        catch (_: SecurityException) { mutable.value = mutable.value.copy(error = "Akses Wi-Fi belum diizinkan."); return }
        discovered.clear(); serviceIds.clear(); resolveCount = 0; scanning = true; mutable.value = mutable.value.copy(laptops = emptyList(), scanning = true)
        val epoch = scanEpoch
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { scope.launch { if (epoch == scanEpoch) stopScan() } }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { scope.launch { if (epoch == scanEpoch) { discovery = null; scanning = false; lock?.let { runCatching { if (it.isHeld) it.release() } }; lock = null; mutable.value = mutable.value.copy(scanning = false, error = "Pencarian laptop berhenti dengan kesalahan.") } } }
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onServiceFound(service: NsdServiceInfo) { scope.launch {
                if (epoch != scanEpoch || resolveCount >= 32 || resolveQueue.size >= 32) return@launch
                resolveCount++; resolveQueue.addLast(service); resolveNext(epoch)
            } }
            override fun onServiceLost(service: NsdServiceInfo) { scope.launch { if (epoch == scanEpoch) serviceIds.remove(service.serviceName)?.let { id -> discovered.remove(id); peerCache.remove(id); publishDevices() } } }
        }
        discovery = listener
        try { nsd.discoverServices("_rikohub._tcp.", NsdManager.PROTOCOL_DNS_SD, listener) }
        catch (_: Exception) { stopScan(); mutable.value = mutable.value.copy(error = "Akses jaringan lokal belum diizinkan atau pencarian gagal."); return }
        scanJob = scope.launch { delay(20_000); if (epoch == scanEpoch) { stopScan(); discovered.clear(); serviceIds.clear(); publishDevices() } }
    }

    @Suppress("DEPRECATION") private fun resolveNext(epoch: Int) {
        if (resolving || resolveQueue.isEmpty() || epoch != scanEpoch) return
        resolving = true
        val item = resolveQueue.removeFirst()
        try { nsd.resolveService(item, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { scope.launch { if (epoch == scanEpoch) { resolving = false; resolveNext(epoch) } } }
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) { scope.launch {
                if (epoch != scanEpoch) return@launch
                resolving = false
                try {
                    val attrs = serviceInfo.attributes
                    val id = attrs["id"]?.toString(Charsets.UTF_8).orEmpty(); val version = attrs["v"]?.toString(Charsets.UTF_8)
                    val caps = attrs["caps"]?.toString(Charsets.UTF_8); val name = attrs["name"]?.toString(Charsets.UTF_8).orEmpty().take(48)
                    val addr = serviceInfo.host
                    if (epoch == scanEpoch && version == "1" && caps == "presence" && isUuid(id) && name.isNotBlank() && isPrivateIpv4(addr)) {
                        val peer = Laptop(id, name, addr, serviceInfo.port)
                        discovered[id] = peer
                        val now = android.os.SystemClock.elapsedRealtime()
                        peerCache.entries.removeAll { now - it.value.second >= 90_000 }
                        if (id !in peerCache && peerCache.size >= 32) peerCache.minByOrNull { it.value.second }?.key?.let(peerCache::remove)
                        peerCache[id] = peer to now
                        serviceIds[serviceInfo.serviceName] = id; publishDevices()
                    }
                } catch (_: Exception) { }
                resolveNext(epoch)
            } }
        }) } catch (_: SecurityException) { resolving = false; resolveQueue.clear(); stopScan(); mutable.value = mutable.value.copy(error = "Akses jaringan lokal belum diizinkan.") }
          catch (_: IllegalArgumentException) { resolving = false; resolveNext(epoch) }
    }

    private fun publishDevices() { mutable.value = mutable.value.copy(laptops = discovered.values.toList(), scanning = scanning) }
    private fun stopScan() {
        scanEpoch++
        scanning = false
        resolving = false
        scanJob?.cancel(); scanJob = null
        discovery?.let { runCatching { nsd.stopServiceDiscovery(it) } }; discovery = null
        lock?.let { runCatching { if (it.isHeld) it.release() } }; lock = null
        resolveQueue.clear()
        mutable.value = mutable.value.copy(scanning = false)
    }

    fun pair(laptop: Laptop, raw: String) {
        val p = parsePairingPayload(raw)
        if (p == null || p.laptopId != laptop.id) { mutable.value = mutable.value.copy(error = "Kode pairing tidak valid atau bukan untuk laptop terpilih."); return }
        if (storageBlocked || prefs.contains("record")) { mutable.value = mutable.value.copy(error = "Penyimpanan kepercayaan perlu direset sebelum pairing ulang."); return }
        val net = network ?: run { mutable.value = mutable.value.copy(error = "Sambungkan ponsel ke Wi-Fi yang sama dengan laptop."); return }
        if (mutable.value.trusted != null) { mutable.value = mutable.value.copy(error = "Lupakan laptop tersimpan sebelum memasangkan laptop lain."); return }
        val gen = ++generation
        trustEpoch.invalidate()
        val previous = operation
        previous?.cancel(); mutable.value = mutable.value.copy(busy = true, pairing = true, error = null)
        operation = scope.launch {
            previous?.join()
            if (gen != generation) return@launch
            var issuedTrust: TrustedLaptop? = null
            try {
                val hello = request(null, net, "GET", "/v1/hello", null, laptop, p.pin)
                verifyPeer(hello, laptop.id)
                if (gen != generation || !active || network != net) return@launch
                val deviceId = withContext(Dispatchers.IO) { phoneId }
                if (gen != generation || !active || network != net) return@launch
                val body = JSONObject().put("protocol", 1).put("pairing_secret", p.secret).put("device_id", deviceId).put("device_name", "Riko Hub Android")
                val response = request(null, net, "POST", "/v1/pair", body, laptop, p.pin)
                verifyPeer(response, laptop.id)
                val token = response.getString("token")
                if (!token.matches(Regex("[0-9a-f]{64}"))) error("Respons token tidak valid")
                val trust = TrustedLaptop(laptop.id, response.getString("name").take(48), p.pin, token, deviceId)
                issuedTrust = trust
                if (gen != generation || !active || network != net) {
                    withContext(NonCancellable) { runCatching { request(trust, net, "DELETE", "/v1/trust", null, laptop, p.pin) } }
                    mutable.value = mutable.value.copy(busy = false, pairing = false)
                    return@launch
                }
                try { withContext(Dispatchers.IO) { writeTrust(trust) } } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    trustEpoch.invalidate()
                    val cleared = withContext(NonCancellable + Dispatchers.IO) { prefs.edit().remove("record").commit() }
                    if (!cleared) storageBlocked = true
                    val revoked = withContext(NonCancellable) { runCatching { request(trust, net, "DELETE", "/v1/trust", null, laptop, p.pin).also { verifyPeer(it, trust.id) } }.isSuccess }
                    if (!revoked) throw TerminalCompanionException(if (cleared) "Pairing disetujui tetapi penyimpanan ponsel gagal. Jalankan `revoke $deviceId` di laptop sebelum mencoba lagi." else "Penyimpanan kepercayaan gagal dibersihkan. Reset lokal, lalu jalankan `revoke $deviceId` di laptop.")
                    if (!cleared) throw TerminalCompanionException("Penyimpanan kepercayaan gagal dibersihkan. Reset kepercayaan lokal sebelum mencoba lagi.")
                    throw TerminalCompanionException("Penyimpanan kepercayaan gagal; pairing laptop sudah dicabut. Coba lagi.")
                }
                if (gen != generation || !active || network != net) {
                    withContext(NonCancellable) { runCatching { request(trust, net, "DELETE", "/v1/trust", null, laptop, p.pin) } }
                    trustEpoch.invalidate()
                    withContext(Dispatchers.IO) { prefs.edit().remove("record").commit() }
                    mutable.value = mutable.value.copy(trusted = null, connected = false, busy = false, pairing = false)
                    return@launch
                }
                mutable.value = mutable.value.copy(trusted = trust, busy = false, pairing = false, error = "Menunggu koneksi tervalidasi…")
                startHeartbeat()
            } catch (e: CancellationException) {
                issuedTrust?.let { t -> withContext(NonCancellable) {
                    trustEpoch.invalidate()
                    val revoked = runCatching { request(t, net, "DELETE", "/v1/trust", null, laptop, p.pin).also { verifyPeer(it, t.id) } }.isSuccess
                    withContext(Dispatchers.IO) { prefs.edit().remove("record").commit() }
                    mutable.value = mutable.value.copy(trusted = null, connected = false, busy = false, pairing = false,
                        error = if (!revoked) "Pairing disetujui tetapi belum tersimpan. Jalankan `revoke ${t.phoneId}` di laptop sebelum mencoba lagi." else null)
                } }
                throw e
            }
            catch (e: Exception) { if (gen == generation) mutable.value = mutable.value.copy(busy = false, pairing = false, error = safeMessage(e)) }
        }
    }

    fun connect() { startHeartbeat() }
    fun disconnect() {
        heartbeatJob?.cancel(); mutable.value = mutable.value.copy(connected = false)
        val t = mutable.value.trusted; val n = network
        if (t != null && n != null) scope.launch { runCatching { request(t, n, "POST", "/v1/disconnect", JSONObject().put("protocol", 1)) } }
    }
    private fun startHeartbeat() {
        heartbeatJob?.cancel(); val gen = generation
        heartbeatJob = scope.launch {
            var failures = 0
            while (active && gen == generation) {
                val t = mutable.value.trusted ?: break; val net = network ?: break
                try {
                    val result = request(t, net, "GET", "/v1/status", null)
                    verifyPeer(result, t.id); if (result.optString("status") != "connected") error("Status laptop tidak valid")
                    if (gen != generation || !active || network != net || mutable.value.trusted != t) break
                    failures = 0; mutable.value = mutable.value.copy(connected = true, error = null)
                    delay(15_000)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    if (gen != generation || !active || network != net) break
                    mutable.value = mutable.value.copy(connected = false, error = safeMessage(e))
                    if (e is TerminalCompanionException || causeChain(e).any { it is SSLPeerUnverifiedException || it is CertificateException }) break
                    failures++; delay(retryDelaySeconds(failures) * 1000)
                }
            }
        }
    }

    suspend fun forget() {
        val t = mutable.value.trusted ?: return
        generation++; trustEpoch.invalidate(); operation?.cancelAndJoin(); heartbeatJob?.cancelAndJoin()
        mutable.value = mutable.value.copy(busy = true, connected = false, error = null)
        var remote = false
        try { val net = network; if (net != null) { val r = request(t, net, "DELETE", "/v1/trust", null); verifyPeer(r, t.id); remote = true } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { }
        finally {
            withContext(NonCancellable) {
                heartbeatJob?.cancel()
                val removed = withContext(Dispatchers.IO) { prefs.edit().remove("record").commit() }
                trustEpoch.invalidate()
                mutable.value = mutable.value.copy(trusted = if (removed) null else t, connected = false, busy = false,
                    error = when { !removed -> "Kepercayaan lokal gagal dihapus. Coba lagi."; remote -> null; else -> "Laptop belum mengonfirmasi pencabutan. Jalankan `revoke ${t.phoneId}` di laptop." })
            }
        }
    }

    private suspend fun request(trust: TrustedLaptop?, net: Network, method: String, path: String, body: JSONObject?, laptop: Laptop? = null, pin: String? = null): JSONObject {
        val target = laptop ?: trust?.let { t -> peerCache[t.id]?.takeIf { android.os.SystemClock.elapsedRealtime() - it.second < 90_000 }?.first }
            ?: run { if (active && !scanning) startScan(); error("Laptop sedang dicari kembali di jaringan Wi-Fi.") }
        return withContext(Dispatchers.IO) { requestBlocking(trust, net, method, path, body, target, pin) }
    }

    private fun requestBlocking(trust: TrustedLaptop?, net: Network, method: String, path: String, body: JSONObject?, target: Laptop, pin: String?): JSONObject {
        if (!isPrivateIpv4(target.address)) error("Alamat laptop bukan IPv4 privat.")
        val expectedPin = pin ?: trust?.pin ?: error("Fingerprint sertifikat tidak tersedia")
        val context = SSLContext.getInstance("TLSv1.2")
        context.init(null, arrayOf(object : X509TrustManager {
            override fun getAcceptedIssuers() = emptyArray<java.security.cert.X509Certificate>()
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                val cert = chain.firstOrNull() ?: throw SSLPeerUnverifiedException("Sertifikat kosong")
                if (!matchesPin(cert, expectedPin)) throw SSLPeerUnverifiedException("Fingerprint sertifikat berubah; pairing ulang diperlukan.")
            }
        }), java.security.SecureRandom())
        val conn = net.openConnection(URL("https://${target.address.hostAddress}:${target.port}$path"), java.net.Proxy.NO_PROXY) as HttpsURLConnection
        activeConnection = conn
        try {
            conn.connectTimeout = 8_000; conn.readTimeout = if (path == "/v1/pair") 65_000 else 8_000; conn.instanceFollowRedirects = false
            conn.sslSocketFactory = context.socketFactory; conn.hostnameVerifier = HostnameVerifier { _, session ->
                runCatching { matchesPin(session.peerCertificates.first() as java.security.cert.X509Certificate, expectedPin) }.getOrDefault(false)
            }
            conn.requestMethod = method; conn.setRequestProperty("Accept", "application/json")
            if (trust != null) { conn.setRequestProperty("X-Riko-Protocol", "1"); conn.setRequestProperty("Authorization", "Bearer ${trust.token}") }
            if (body != null) { val bytes = body.toString().toByteArray(); require(bytes.size <= 8192); conn.doOutput = true; conn.setRequestProperty("Content-Type", "application/json"); conn.outputStream.use { it.write(bytes) } }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val bytes = stream?.use { input ->
                val output = ByteArrayOutputStream(); val buffer = ByteArray(1024)
                while (output.size() <= 8192) {
                    val count = input.read(buffer, 0, minOf(buffer.size, 8193 - output.size()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: byteArrayOf()
            if (bytes.size > 8192) error("Respons laptop terlalu besar")
            if (code == 429 || code in 500..599) throw IOException("Laptop sementara tidak tersedia ($code).")
            if (code !in 200..299) throw TerminalCompanionException(when (code) { 401 -> "Akses dicabut; pairing ulang diperlukan."; 403 -> "Pairing ditolak atau kedaluwarsa."; 426 -> "Versi protokol laptop tidak cocok."; else -> "Laptop menolak permintaan ($code)." })
            return JSONObject(bytes.toString(Charsets.UTF_8))
        } finally { conn.disconnect(); if (activeConnection === conn) activeConnection = null }
    }

    private fun verifyPeer(json: JSONObject, id: String) {
        if (json.opt("protocol") != 1 || json.optString("peer_id") != id || !json.optString("name").isNotBlank() ||
            json.optJSONArray("capabilities")?.let { it.length() == 1 && it.optString(0) == "presence" } != true)
            throw TerminalCompanionException("Identitas atau protokol laptop tidak cocok.")
    }
    private fun safeMessage(e: Exception) = when (e) {
        is TerminalCompanionException -> e.message ?: "Koneksi laptop gagal."
        else -> if (causeChain(e).any { it is SSLPeerUnverifiedException || it is CertificateException }) "Sertifikat laptop berubah; lupakan laptop lalu pairing ulang." else "Tidak dapat menghubungi laptop melalui Wi-Fi. Periksa jaringan dan coba lagi."
    }
    private class TerminalCompanionException(message: String) : Exception(message)
    private fun matchesPin(cert: java.security.cert.X509Certificate, pin: String): Boolean {
        cert.checkValidity()
        val actual = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
        val expected = pin.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return MessageDigest.isEqual(actual, expected)
    }
    private fun causeChain(error: Throwable): Sequence<Throwable> = sequence {
        var current: Throwable? = error
        while (true) { val item = current ?: break; yield(item); current = item.cause }
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey("riko_companion_aes", null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        gen.init(android.security.keystore.KeyGenParameterSpec.Builder("riko_companion_aes", android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        return gen.generateKey()
    }
    private fun writeTrust(t: TrustedLaptop) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(JSONObject().put("id", t.id).put("name", t.name).put("pin", t.pin).put("token", t.token).put("phone", t.phoneId).toString().toByteArray())
        check(prefs.edit().putString("record", Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)).commit())
    }
    private fun readTrust(): TrustedLaptop? {
        val encoded = prefs.getString("record", null) ?: return null
        val data = Base64.decode(encoded, Base64.NO_WRAP); require(data.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key(), javax.crypto.spec.GCMParameterSpec(128, data.copyOfRange(0, 12)))
        val json = JSONObject(cipher.doFinal(data.copyOfRange(12, data.size)).toString(Charsets.UTF_8))
        val result = TrustedLaptop(json.getString("id"), json.getString("name"), json.getString("pin"), json.getString("token"), json.getString("phone"))
        require(isUuid(result.id) && isUuid(result.phoneId) && result.pin.matches(Regex("[0-9a-f]{64}")) && result.token.matches(Regex("[0-9a-f]{64}")))
        return result
    }
}
