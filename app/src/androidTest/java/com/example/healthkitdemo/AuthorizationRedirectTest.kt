package com.example.healthkitdemo

import android.net.Uri
import com.example.healthkitdemo.healthkit.auth.AuthorizationRedirect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizationRedirectTest {

    private fun redirect(query: String) = Uri.parse("healthkitdemo://oauth/callback?$query")

    @Test
    fun returnsTheCodeWhenTheStateMatches() {
        val code = AuthorizationRedirect.parseCode(redirect("code=abc&state=s1"), expectedState = "s1")
        assertEquals("abc", code.getOrThrow())
    }

    @Test
    fun rejectsAnotherState() {
        val code = AuthorizationRedirect.parseCode(redirect("code=abc&state=other"), expectedState = "s1")
        assertTrue(code.isFailure)
    }

    @Test
    fun rejectsARedirectWhenNoSignInWasStarted() {
        val code = AuthorizationRedirect.parseCode(redirect("code=abc&state=s1"), expectedState = null)
        assertTrue(code.isFailure)
    }

    @Test
    fun reportsTheErrorHuaweiSent() {
        val code = AuthorizationRedirect.parseCode(redirect("error=access_denied&state=s1"), expectedState = "s1")
        assertEquals("access_denied", code.exceptionOrNull()?.message)
    }
}
