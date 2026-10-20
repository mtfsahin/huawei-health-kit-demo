package com.example.healthkitdemo.healthkit.net

import com.example.healthkitdemo.healthkit.HealthKitConfig
import com.example.healthkitdemo.log.AppLog
import com.example.healthkitdemo.log.LogCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class HttpResponse(val status: Int, val body: String, private val headers: Map<String, String>) {
    fun header(name: String): String? = headers[name.lowercase()]

    fun json(): JSONObject = JSONObject(body)
}

object Http {

    suspend fun get(url: String, accessToken: String): HttpResponse = send("GET", url, accessToken)

    suspend fun delete(url: String, accessToken: String): HttpResponse = send("DELETE", url, accessToken)

    suspend fun postJson(url: String, body: JSONObject, accessToken: String? = null): HttpResponse =
        send("POST", url, accessToken, body.toString(), "application/json; charset=UTF-8")

    suspend fun postForm(url: String, form: Map<String, String>): HttpResponse {
        val body = form.entries.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
        return send("POST", url, null, body, "application/x-www-form-urlencoded")
    }

    private suspend fun send(
        method: String,
        url: String,
        accessToken: String?,
        body: String? = null,
        contentType: String = "application/json; charset=UTF-8",
    ): HttpResponse = withContext(Dispatchers.IO) {
        val traceId = UUID.randomUUID().toString()
        val started = System.currentTimeMillis()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("x-caller-trace-id", traceId)
            if (accessToken != null) {
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("x-client-id", HealthKitConfig.clientId)
            }
        }
        try {
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val headers = connection.headerFields
                .filterKeys { it != null }
                .mapKeys { it.key.lowercase() }
                .mapValues { it.value.firstOrNull().orEmpty() }
            HttpResponse(status, text, headers).also { logCall(method, url, it, System.currentTimeMillis() - started, traceId) }
        } catch (error: IOException) {
            AppLog.error(LogCategory.API, "$method ${shortUrl(url)} failed", "${error.javaClass.simpleName}: ${error.message}\ntrace $traceId")
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun logCall(method: String, url: String, response: HttpResponse, durationMs: Long, traceId: String) {
        val message = "$method ${shortUrl(url)} -> ${response.status} (${durationMs} ms)"
        val detail = buildString {
            append("host ${URL(url).host}\ntrace $traceId\nbytes ${response.body.length}")
            response.header("x-health-app-privacy")?.let { append("\nx-health-app-privacy $it") }
            if (response.status !in 200..299) append("\nbody ${redact(response.body).take(500)}")
        }
        if (response.status in 200..299) AppLog.info(LogCategory.API, message, detail) else AppLog.warn(LogCategory.API, message, detail)
    }

    private fun shortUrl(url: String): String {
        val parsed = URL(url)
        return parsed.path + (parsed.query?.let { "?" + it.take(120) } ?: "")
    }

    private fun redact(text: String): String = SECRET_FIELD.replace(text) { "\"${it.groupValues[1]}\":\"***\"" }

    private val SECRET_FIELD = Regex("\"(access_token|refresh_token|id_token|client_secret|code_verifier)\"\\s*:\\s*\"[^\"]*\"")

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private const val TIMEOUT_MS = 15_000
}

val HttpResponse.privacyStatus: Int? get() = header("x-health-app-privacy")?.toIntOrNull()

fun HttpResponse.requireSuccess(): HttpResponse {
    if (status !in 200..299) throw HealthKitHttpException(this)
    return this
}

class HealthKitHttpException(private val response: HttpResponse) :
    Exception("HTTP ${response.status}: ${response.body.take(300)}") {

    private val json: JSONObject? = runCatching { JSONObject(response.body) }.getOrNull()

    val status: Int get() = response.status

    val body: String get() = response.body

    val code: Int = json?.optJSONObject("error")?.optInt("code")?.takeIf { it != 0 }
        ?: json?.optInt("code")?.takeIf { it != 0 }
        ?: json?.optInt("error")?.takeIf { it != 0 }
        ?: response.status

    val subError: Int = json?.optInt("sub_error") ?: 0

    val isSiteCross: Boolean get() = status == 403 && code == HealthKitConfig.SITE_CROSS_ERROR_CODE

    val redirectHost: String? get() = response.header("Location")?.let { runCatching { URL(it).host }.getOrNull() }

    val isInvalidCredentials: Boolean get() = status == 401

    val isRefreshTokenExpired: Boolean
        get() = code == HealthKitConfig.REFRESH_TOKEN_RESULT_CODE && subError in HealthKitConfig.REFRESH_TOKEN_EXPIRED_SUB_ERRORS
}
