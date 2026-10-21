package com.example.healthkitdemo

import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthkitdemo.healthkit.auth.AuthorizationDeniedException
import com.example.healthkitdemo.healthkit.errors.ErrorCatalog
import com.example.healthkitdemo.healthkit.errors.LoginFreeException
import com.example.healthkitdemo.healthkit.net.HealthKitHttpException
import com.example.healthkitdemo.healthkit.net.HttpResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class ErrorCatalogTest {

    private val catalog = ErrorCatalog(InstrumentationRegistry.getInstrumentation().targetContext)

    private fun http(status: Int, body: String) = HealthKitHttpException(HttpResponse(status, body, emptyMap()))

    @Test
    fun loadsEveryDomain() {
        assertTrue(catalog.size >= 100)
    }

    @Test
    fun missingScopeMatchesTheScopeEntry() {
        val info = catalog.explain(http(403, """{"error":{"code":403,"message":"Insufficient Permission: Request had insufficient authentication scopes of type com.huawei.continuous.steps.delta."}}"""))
        assertEquals("REST SC_FORBIDDEN", info.label)
        assertTrue(info.title.contains("insufficient authentication scopes"))
    }

    @Test
    fun crossSiteRequestMatchesItsCode() {
        assertEquals("REST 121001", catalog.explain(http(403, """{"error":{"code":121001,"message":"request forbidden due to site cross"}}""")).label)
    }

    @Test
    fun expiredRefreshTokenMatchesTheOAuthSubError() {
        assertEquals("OAUTH 1203/11205", catalog.explain(http(400, """{"error":1203,"sub_error":11205,"error_description":"token expired"}""")).label)
    }

    @Test
    fun loginFreeCodesAreExplained() {
        assertEquals("LOGIN_FREE 51011", catalog.explain(LoginFreeException(51011)).label)
    }

    @Test
    fun privacyHeaderIsExplained() {
        assertEquals("PRIVACY x-health-app-privacy/2", catalog.privacy(2)?.label)
    }

    @Test
    fun networkAndCancelErrorsHaveAnAnswer() {
        assertEquals("NETWORK", catalog.explain(UnknownHostException("health-api.cloud.huawei.com")).domain)
        assertEquals("AUTH", catalog.explain(AuthorizationDeniedException("closed")).domain)
    }

    @Test
    fun unknownStatusFallsBackToHttp() {
        assertEquals("HTTP 418", catalog.explain(http(418, "teapot")).label)
    }
}
