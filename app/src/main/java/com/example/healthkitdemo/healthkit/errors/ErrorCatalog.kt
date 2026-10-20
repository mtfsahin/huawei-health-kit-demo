package com.example.healthkitdemo.healthkit.errors

import android.content.Context
import com.example.healthkitdemo.healthkit.auth.AuthorizationDeniedException
import com.example.healthkitdemo.healthkit.net.HealthKitHttpException
import org.json.JSONArray
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class ErrorInfo(
    val domain: String,
    val code: String,
    val title: String,
    val cause: String,
    val solution: String,
    val url: String? = null,
) {
    val label: String get() = if (code.isEmpty()) domain else "$domain $code"
}

class LoginFreeException(val code: Int) : Exception("Login-free SDK status $code")

class ErrorCatalog(private val context: Context) {

    val size: Int get() = entries.size

    private data class Entry(val domain: String, val httpStatus: Int?, val code: String, val subCode: String?, val info: ErrorInfo)

    private val entries: List<Entry> by lazy { load() }

    fun explain(error: Throwable): ErrorInfo = when (error) {
        is HealthKitHttpException -> fromHttp(error)
        is LoginFreeException -> find("LOGIN_FREE", code = error.code.toString())
            ?: ErrorInfo("LOGIN_FREE", error.code.toString(), "Huawei Health returned an error", "DEEPLINK_RESULT ${error.code}", "Check the Logs tab and the login-free error table in the README.")
        is AuthorizationDeniedException -> ErrorInfo("AUTH", "", "Authorization was not completed", error.message.orEmpty(), "Tap Connect and try again.")
        is UnknownHostException, is ConnectException -> ErrorInfo(
            "NETWORK", "", "No connection",
            "The phone could not reach the server: ${error.message}",
            "Check the internet connection and that backend.baseUrl points to a running backend.",
        )
        is SocketTimeoutException -> ErrorInfo("NETWORK", "", "The server did not answer in time", error.message.orEmpty(), "Try again in a moment.")
        else -> unknown("APP", error)
    }

    fun privacy(status: Int): ErrorInfo? = find("PRIVACY", code = PRIVACY_HEADER, subCode = status.toString())

    private fun fromHttp(error: HealthKitHttpException): ErrorInfo {
        val code = error.code.toString()
        val subCode = error.subError.takeIf { it != 0 }?.toString()
        return find("REST", code = code)
            ?: find("OAUTH", code = code, subCode = subCode)
            ?: find("OAUTH", code = code)
            ?: restByStatus(error)
            ?: ErrorInfo("HTTP", error.status.toString(), "The server answered ${error.status}", error.body.take(300), "Open the Logs tab for the full answer.")
    }

    private fun restByStatus(error: HealthKitHttpException): ErrorInfo? {
        val candidates = entries.filter { it.domain == "REST" && it.httpStatus == error.status }
        val matching = candidates.firstOrNull { error.body.contains(it.info.title.substringAfter(": ").trimEnd('.'), ignoreCase = true) }
        return (matching ?: candidates.firstOrNull())?.info
    }

    private fun find(domain: String, code: String, subCode: String? = null): ErrorInfo? =
        entries.firstOrNull { it.domain == domain && it.code == code && it.subCode == subCode }?.info

    private fun unknown(domain: String, error: Throwable) = ErrorInfo(
        domain, "", "Something went wrong", "${error.javaClass.simpleName}: ${error.message}", "Check the log for details.",
    )

    private fun load(): List<Entry> {
        val json = context.assets.open("health_kit_errors.json").bufferedReader().use { JSONArray(it.readText()) }
        return (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            val domain = item.getString("domain")
            val code = item.get("code").toString()
            Entry(
                domain = domain,
                httpStatus = item.optInt("httpStatus").takeIf { !item.isNull("httpStatus") },
                code = code,
                subCode = if (item.isNull("subCode")) null else item.get("subCode").toString(),
                info = ErrorInfo(
                    domain = domain,
                    code = listOfNotNull(code, item.opt("subCode")?.takeIf { !item.isNull("subCode") }).joinToString("/"),
                    title = item.getString("message"),
                    cause = item.getString("cause"),
                    solution = item.getString("solution"),
                    url = item.optString("url").takeIf { it.isNotBlank() },
                ),
            )
        }
    }

    private companion object {
        const val PRIVACY_HEADER = "x-health-app-privacy"
    }
}
