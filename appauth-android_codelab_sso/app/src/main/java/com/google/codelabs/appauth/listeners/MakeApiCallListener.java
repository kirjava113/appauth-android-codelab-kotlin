package com.google.codelabs.appauth.listeners;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.codelabs.appauth.R;
import com.google.codelabs.appauth.view.MainActivity;
import com.squareup.picasso.Picasso;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.google.codelabs.appauth.MainApplication.LOG_TAG;

public class MakeApiCallListener implements Button.OnClickListener {

    private final MainActivity mMainActivity;
    private AuthState mAuthState;
    private AuthorizationService mAuthorizationService;

    private String onClickURL = "https://www.googleapis.com/oauth2/v3/userinfo";

    public MakeApiCallListener(@NonNull MainActivity mainActivity, @NonNull AuthState authState, @NonNull AuthorizationService authorizationService) {
        mMainActivity = mainActivity;
        mAuthState = authState;
        mAuthorizationService = authorizationService;
    }

    @Override
    public void onClick(View view) {
        mAuthState.performActionWithFreshTokens(mAuthorizationService, new AuthState.AuthStateAction() {
            @Override
            public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException exception) {
                new AsyncTask<String, Void, JSONObject>() {
                    @Override
                    protected JSONObject doInBackground(String... tokens) {
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(onClickURL)
                                .addHeader("Authorization", String.format("Bearer %s", tokens[0]))
                                .build();

                        try {
                            Response response = client.newCall(request).execute();
                            String jsonBody = response.body().string();
                            Log.i(LOG_TAG, String.format("User Info Response %s", jsonBody));
                            return new JSONObject(jsonBody);
                        } catch (Exception exception) {
                            Log.w(LOG_TAG, exception);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(JSONObject userInfo) {
                        if (userInfo != null) {
                            String fullName = userInfo.optString("name", null);
                            String givenName = userInfo.optString("given_name", null);
                            String familyName = userInfo.optString("family_name", null);
                            String imageUrl = userInfo.optString("picture", null);
                            if (!TextUtils.isEmpty(imageUrl)) {
                                Picasso.with(mMainActivity)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_account_circle_black_48dp)
                                        .into(mMainActivity.getmProfileView());
                            }
                            if (!TextUtils.isEmpty(fullName)) {
                                mMainActivity.getmFullName().setText(fullName);
                            }
                            if (!TextUtils.isEmpty(givenName)) {
                                mMainActivity.getmGivenName().setText(givenName);
                            }
                            if (!TextUtils.isEmpty(familyName)) {
                                mMainActivity.getmFamilyName().setText(familyName);
                            }

                            String message;
                            if (userInfo.has("error")) {
                                message = String.format("%s [%s]", mMainActivity.getString(R.string.request_failed), userInfo.optString("error_description", "No description"));
                            } else {
                                message = mMainActivity.getString(R.string.request_complete);
                            }
                            Snackbar.make(mMainActivity.getmProfileView(), message, Snackbar.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }.execute(accessToken);
            }
        });
    }
}