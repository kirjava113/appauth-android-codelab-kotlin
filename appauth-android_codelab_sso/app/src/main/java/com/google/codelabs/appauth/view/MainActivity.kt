package com.google.codelabs.appauth.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatTextView
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.google.codelabs.appauth.MainApplication
import com.google.codelabs.appauth.MainApplication.Companion.LOG_TAG
import com.google.codelabs.appauth.R
import com.google.codelabs.appauth.listeners.AuthorizeListener
import com.google.codelabs.appauth.listeners.MakeApiCallListener
import com.google.codelabs.appauth.listeners.SignOutListener
import com.google.codelabs.appauth.presenter.MainPresenter
import kotlinx.android.synthetic.main.activity_main.*
import net.openid.appauth.*
import org.json.JSONException


class MainActivity : AppCompatActivity() {

    companion object {

        private const val SHARED_PREFERENCES_NAME = "AuthStatePreference"
        private const val AUTH_STATE = "AUTH_STATE"
        private const val USED_INTENT = "USED_INTENT"
    }

    private lateinit var mMainApplication: MainApplication

    private lateinit var presenter: MainPresenter

    // state
    var mAuthState: AuthState? = null

    // views
    fun getmProfileView(): ImageView {
        return profileImage
    }

    fun getmGivenName(): AppCompatTextView {
        return givenName
    }

    fun getmFamilyName(): AppCompatTextView {
        return familyName
    }

    fun getmFullName(): AppCompatTextView {
        return fullName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mMainApplication = application as MainApplication

        enablePostAuthorizationFlows()

        // wire click listeners
        authorize.setOnClickListener(AuthorizeListener())
        presenter = MainPresenter()
    }

    override fun onNewIntent(intent: Intent) {
        checkIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        checkIntent(intent)
    }

    private fun checkIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            when (action) {
                "com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE" -> if (!intent.hasExtra(USED_INTENT)) {
                    handleAuthorizationResponse(intent)
                    intent.putExtra(USED_INTENT, true)
                }
            }// do nothing
        }
    }

    fun enablePostAuthorizationFlows() {
        mAuthState = restoreAuthState()
        if (mAuthState != null && mAuthState!!.isAuthorized) {
            if (makeApiCall.visibility == View.GONE) {
                makeApiCall.visibility = View.VISIBLE
                makeApiCall.setOnClickListener(MakeApiCallListener(this, mAuthState!!, AuthorizationService(this)))
            }
            if (signOut.visibility == View.GONE) {
                signOut.visibility = View.VISIBLE
                signOut.setOnClickListener(SignOutListener(this))
            }
        } else {
            makeApiCall.visibility = View.GONE
            signOut.visibility = View.GONE
        }
    }

    /**
     * Exchanges the code, for the [TokenResponse].
     *
     * @param intent represents the [Intent] from the Custom Tabs or the System Browser.
     */
    private fun handleAuthorizationResponse(intent: Intent) {
        val response = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)
        val authState = AuthState(response, error)

        if (response != null) {
            Log.i(LOG_TAG, String.format("Handled Authorization Response %s ", authState.toJsonString()))
            val service = AuthorizationService(this)
            service.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, exception ->
                if (exception != null) {
                    Log.w(LOG_TAG, "Token Exchange failed", exception)
                } else {
                    if (tokenResponse != null) {
                        authState.update(tokenResponse, exception)
                        persistAuthState(authState)
                        Log.i(LOG_TAG, String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken))
                    }
                }
            }
        }
    }

    private fun persistAuthState(authState: AuthState) {
        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putString(AUTH_STATE, authState.toJsonString())
                .apply()
        enablePostAuthorizationFlows()
    }

    fun clearAuthState() {
        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(AUTH_STATE)
                .apply()
    }

    private fun restoreAuthState(): AuthState? {
        val jsonString = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(AUTH_STATE, null)
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.fromJson(jsonString!!)
            } catch (jsonException: JSONException) {
                // should never happen
            }

        }
        return null
    }

}
