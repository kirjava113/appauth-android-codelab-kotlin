package com.google.codelabs.appauth.listeners

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.view.View
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration

/**
 * Kicks off the authorization flow.
 */
class AuthorizeListener : View.OnClickListener {

    private val authEndpoint = "https://accounts.google.com/o/oauth2/v2/auth"
    private val tokenEndpoint = "https://www.googleapis.com/oauth2/v4/token"
    private val clientId = "511828570984-fuprh0cm7665emlne3rnf9pk34kkn86s.apps.googleusercontent.com"
    private val redirectUri = Uri.parse("com.google.codelabs.appauth:/oauth2callback")

    private val scope = "profile"

    override fun onClick(view: View) {
        val serviceConfiguration = AuthorizationServiceConfiguration(
                Uri.parse(authEndpoint) /* auth endpoint */,
                Uri.parse(tokenEndpoint) /* token endpoint */
        )

        val clientId = clientId
        val redirectUri = redirectUri
        val builder = AuthorizationRequest.Builder(
                serviceConfiguration,
                clientId,
                AuthorizationRequest.RESPONSE_TYPE_CODE,
                redirectUri
        )
        builder.setScopes(scope)
        val request = builder.build()

        val authorizationService = AuthorizationService(view.context)

        val action = "com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE"
        val postAuthorizationIntent = Intent(action)
        val pendingIntent = PendingIntent.getActivity(view.context, request.hashCode(), postAuthorizationIntent, 0)
        authorizationService.performAuthorizationRequest(request, pendingIntent)
    }
}
