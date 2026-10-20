package com.example.healthkitdemo.healthkit.auth

import android.net.Uri
import com.example.healthkitdemo.healthkit.errors.LoginFreeException

object AuthorizationRedirect {

    fun parseCode(uri: Uri, expectedState: String?): Result<String> {
        val error = uri.getQueryParameter("error") ?: uri.getQueryParameter("errorCode")
        if (error != null) {
            error.toIntOrNull()?.let { return Result.failure(LoginFreeException(it)) }
            val description = uri.getQueryParameter("error_description") ?: uri.getQueryParameter("errorMessage")
            return failure(listOfNotNull(error, description).joinToString(": "))
        }
        if (expectedState == null) return failure("No sign-in was started. Tap Connect again.")
        if (uri.getQueryParameter("state") != expectedState) return failure("The state value did not match the request.")

        val code = uri.getQueryParameter("code") ?: return failure("The redirect carried no authorization code.")
        return Result.success(code)
    }

    private fun failure(message: String): Result<String> = Result.failure(AuthorizationDeniedException(message))
}

class AuthorizationDeniedException(message: String) : Exception(message)
