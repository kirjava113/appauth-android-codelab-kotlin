// Copyright 2016 Google Inc.
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//      http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.codelabs.appauth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.codelabs.appauth.listeners.AuthorizeListener;
import com.google.codelabs.appauth.listeners.MakeApiCallListener;
import com.google.codelabs.appauth.listeners.SignOutListener;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import static com.google.codelabs.appauth.MainApplication.LOG_TAG;

public class MainActivity extends AppCompatActivity {

  private static final String SHARED_PREFERENCES_NAME = "AuthStatePreference";
  private static final String AUTH_STATE = "AUTH_STATE";
  private static final String USED_INTENT = "USED_INTENT";

  MainApplication mMainApplication;

  // state
  public AuthState mAuthState;

  // views
  AppCompatButton mAuthorize;
  AppCompatButton mMakeApiCall;
  AppCompatButton mSignOut;

  public ImageView getmProfileView() {
    return mProfileView;
  }

  public AppCompatTextView getmGivenName() {
    return mGivenName;
  }

  public AppCompatTextView getmFamilyName() {
    return mFamilyName;
  }

  public AppCompatTextView getmFullName() {
    return mFullName;
  }

  AppCompatTextView mGivenName;
  AppCompatTextView mFamilyName;
  AppCompatTextView mFullName;
  ImageView mProfileView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mMainApplication = (MainApplication) getApplication();
    mAuthorize = (AppCompatButton) findViewById(R.id.authorize);
    mMakeApiCall = (AppCompatButton) findViewById(R.id.makeApiCall);
    mSignOut = (AppCompatButton) findViewById(R.id.signOut);
    mGivenName = (AppCompatTextView) findViewById(R.id.givenName);
    mFamilyName = (AppCompatTextView) findViewById(R.id.familyName);
    mFullName = (AppCompatTextView) findViewById(R.id.fullName);
    mProfileView = (ImageView) findViewById(R.id.profileImage);

    enablePostAuthorizationFlows();

    // wire click listeners
    mAuthorize.setOnClickListener(new AuthorizeListener());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    checkIntent(intent);
  }

  private void checkIntent(@Nullable Intent intent) {
    if (intent != null) {
      String action = intent.getAction();
      switch (action) {
        case "com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE":
          if (!intent.hasExtra(USED_INTENT)) {
            handleAuthorizationResponse(intent);
            intent.putExtra(USED_INTENT, true);
          }
          break;
        default:
          // do nothing
      }
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    checkIntent(getIntent());
  }

  public void enablePostAuthorizationFlows() {
    mAuthState = restoreAuthState();
    if (mAuthState != null && mAuthState.isAuthorized()) {
      if (mMakeApiCall.getVisibility() == View.GONE) {
        mMakeApiCall.setVisibility(View.VISIBLE);
        mMakeApiCall.setOnClickListener(new MakeApiCallListener(this, mAuthState, new AuthorizationService(this)));
      }
      if (mSignOut.getVisibility() == View.GONE) {
        mSignOut.setVisibility(View.VISIBLE);
        mSignOut.setOnClickListener(new SignOutListener(this));
      }
    } else {
      mMakeApiCall.setVisibility(View.GONE);
      mSignOut.setVisibility(View.GONE);
    }
  }

  /**
   * Exchanges the code, for the {@link TokenResponse}.
   *
   * @param intent represents the {@link Intent} from the Custom Tabs or the System Browser.
   */
  private void handleAuthorizationResponse(@NonNull Intent intent) {
    AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
    AuthorizationException error = AuthorizationException.fromIntent(intent);
    final AuthState authState = new AuthState(response, error);

    if (response != null) {
      Log.i(LOG_TAG, String.format("Handled Authorization Response %s ", authState.toJsonString()));
      AuthorizationService service = new AuthorizationService(this);
      service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
        @Override
        public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
          if (exception != null) {
            Log.w(LOG_TAG, "Token Exchange failed", exception);
          } else {
            if (tokenResponse != null) {
              authState.update(tokenResponse, exception);
              persistAuthState(authState);
              Log.i(LOG_TAG, String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
            }
          }
        }
      });
    }
  }

  private void persistAuthState(@NonNull AuthState authState) {
    getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
        .putString(AUTH_STATE, authState.toJsonString())
        .commit();
    enablePostAuthorizationFlows();
  }

  public void clearAuthState() {
    getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(AUTH_STATE)
        .apply();
  }

  @Nullable
  private AuthState restoreAuthState() {
    String jsonString = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getString(AUTH_STATE, null);
    if (!TextUtils.isEmpty(jsonString)) {
      try {
        return AuthState.fromJson(jsonString);
      } catch (JSONException jsonException) {
        // should never happen
      }
    }
    return null;
  }

}
