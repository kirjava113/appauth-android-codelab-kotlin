package com.google.codelabs.appauth.listeners

import android.view.View
import com.google.codelabs.appauth.MainActivity

class SignOutListener(private val mMainActivity: MainActivity) : View.OnClickListener {

    override fun onClick(view: View) {
        mMainActivity.mAuthState = null
        mMainActivity.clearAuthState()
        mMainActivity.enablePostAuthorizationFlows()
    }
}
