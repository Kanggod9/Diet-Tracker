package io.github.kanggod9.diettracker.integration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.net.ssl.HttpsURLConnection

data class GatewayConnection(
    val endpoint: String,
    val accessToken: String,
) {
    init {
        val uri = URI(endpoint)
        require(uri.scheme.equals("https", ignoreCase = true)) { "Gateway endpoint must use HTTPS" }
        require(!uri.host.isNullOrBlank()) { "Gateway endpoint must include a host" }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Gateway endpoint cannot contain credentials, query parameters, or a fragment"
        }
        require(accessToken.length in 16..512 && accessToken.none(Char::isWhitespace)) {
            "Gateway access token is invalid"
        }
    }

    val baseUri: URI get() = URI(if (endpoint.endsWith('/')) endpoint else "$endpoint/")
}

class GatewayHttpException(
    val statusCode: Int?,
    message: String,
) : Exception(message)

class GatewayHttpClient(
    private val connectionProvider: () -> GatewayConnection?,
) {
    suspend fun postJson(path: String, json: String): String = withContext(Dispatchers.IO) {
        require(path.matches(Regex("[a-zA-Z0-9/_-]{1,120}"))) { "Invalid gateway path" }
        val body = json.toByteArray(Charsets.UTF_8)
        require(body.size <= MAX_REQUEST_BYTES) { "Gateway request is too large" }
        val connection = connectionProvider() ?: throw GatewayHttpException(
            null,
            "Configure the private HTTPS gateway before using online features.",
        )
        val uri = connection.baseUri.resolve(path.trimStart('/'))
        require(
            uri.scheme.equals(connection.baseUri.scheme, true) &&
                uri.host.equals(connection.baseUri.host, true) &&
                uri.port == connection.baseUri.port,
        ) { "Gateway path escaped the configured origin" }

        val http = (uri.toURL().openConnection() as? HttpsURLConnection)
            ?: throw GatewayHttpException(null, "Gateway is not an HTTPS connection")
        try {
            http.requestMethod = "POST"
            http.connectTimeout = CONNECT_TIMEOUT_MS
            http.readTimeout = READ_TIMEOUT_MS
            http.doOutput = true
            http.instanceFollowRedirects = false
            http.setFixedLengthStreamingMode(body.size)
            http.setRequestProperty("Authorization", "Bearer ${connection.accessToken}")
            http.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            http.setRequestProperty("Accept", "application/json")
            http.setRequestProperty("X-Diet-Tracker-Client", "android/1.2.1")
            http.outputStream.use { it.write(body) }

            val status = http.responseCode
            if (status !in 200..299) {
                http.errorStream?.use { readLimited(it, ERROR_RESPONSE_LIMIT_BYTES) }
                throw GatewayHttpException(status, "Gateway request failed with HTTP $status")
            }
            http.inputStream.use { input ->
                readLimited(input, MAX_RESPONSE_BYTES).toString(Charsets.UTF_8)
            }
        } catch (error: GatewayHttpException) {
            throw error
        } catch (error: Exception) {
            throw GatewayHttpException(null, "The private gateway could not be reached securely.")
        } finally {
            http.disconnect()
        }
    }

    private fun readLimited(input: java.io.InputStream, maximumBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maximumBytes) throw GatewayHttpException(null, "Gateway response was too large")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 20_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val MAX_REQUEST_BYTES = 12 * 1024 * 1024
        private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
        private const val ERROR_RESPONSE_LIMIT_BYTES = 8 * 1024
    }
}
