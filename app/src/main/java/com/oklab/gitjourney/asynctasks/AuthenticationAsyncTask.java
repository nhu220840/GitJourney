package com.oklab.gitjourney.asynctasks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.oklab.gitjourney.R;
import com.oklab.gitjourney.activities.MainActivity;
import com.oklab.gitjourney.data.UserSessionData;
import com.oklab.gitjourney.utils.Utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * Created by olgakuklina on 2017-01-08.
 */

public class AuthenticationAsyncTask extends AsyncTask<String, Integer, UserSessionData> {

    private static final String TAG = AuthenticationAsyncTask.class.getSimpleName();
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String USER_AGENT_VALUE = "GitJourney/1.6";
    private final Context context;

    public AuthenticationAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected UserSessionData doInBackground(String... args) {
        try {
            if (args != null && args.length == 1) {
                return authenticateWithPersonalAccessToken(args[0]);
            } else if (args != null && args.length >= 2) {
                return authenticateWithLoginAndPassword(args[0], args[1]);
            }
            return null;
        } catch (UnauthorizedException e) {
            Log.e(TAG, "Authentication failed due to unauthorized response", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Login failed", e);
            return null;
        }
    }

    private UserSessionData authenticateWithLoginAndPassword(String login, String password) throws Exception {
        HttpURLConnection connect = (HttpURLConnection) new URL(context.getString(R.string.url_connect)).openConnection();
        connect.setRequestMethod("POST");
        connect.setDoOutput(true);
        String inputString = login + ":" + password;
        String credentials = Base64.encodeToString(inputString.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        String authentication = "basic " + credentials;
        connect.setRequestProperty("Authorization", authentication);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("client_id", context.getString(R.string.client_id));
        jsonObject.put("client_secret", context.getString(R.string.client_secret));
        jsonObject.put("note", context.getString(R.string.note));
        JSONArray jsonArray = new JSONArray();
        jsonArray.put("repo");
        jsonArray.put("user");
        jsonObject.put("scopes", jsonArray);

        Log.v(TAG, "request body = " + jsonObject.toString());
        try (OutputStream outputStream = connect.getOutputStream()) {
            outputStream.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
        }
        connect.connect();
        int responseCode = connect.getResponseCode();
        Log.v(TAG, "responseCode = " + responseCode);
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            return null;
        }
        String response;
        try (InputStream inputStream = connect.getInputStream()) {
            response = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
        Log.v(TAG, "response = " + response);
        JSONObject authorizationResponse = new JSONObject(response);
        String token = authorizationResponse.getString("token");

        JSONObject userProfile = requestUserProfile("token", token);
        String loginName = userProfile.getString("login");
        return new UserSessionData(authorizationResponse.getString("id"), credentials, token, loginName);
    }

    private UserSessionData authenticateWithPersonalAccessToken(String personalAccessToken) throws Exception {
        if (personalAccessToken == null || personalAccessToken.isEmpty()) {
            return null;
        }
        try {
            JSONObject userProfile = requestUserProfile("token", personalAccessToken);
            return createSessionDataFromProfile(userProfile, personalAccessToken);
        } catch (UnauthorizedException unauthorizedException) {
            Log.w(TAG, "Retrying personal access token authentication with Bearer scheme", unauthorizedException);
            JSONObject userProfile = requestUserProfile("Bearer", personalAccessToken);
            return createSessionDataFromProfile(userProfile, personalAccessToken);
        }
    }

    private UserSessionData createSessionDataFromProfile(JSONObject userProfile, String token) throws JSONException {
        String login = userProfile.getString("login");
        String baseCredentials = Base64.encodeToString((login + ":" + token).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        String id = userProfile.optString("id", "");
        return new UserSessionData(id, baseCredentials, token, login);
    }

    private JSONObject requestUserProfile(String scheme, String token) throws IOException, JSONException, UnauthorizedException {
        HttpURLConnection reconnect = (HttpURLConnection) new URL(context.getString(R.string.url_login_data)).openConnection();
        reconnect.setRequestMethod("GET");
        reconnect.setRequestProperty("Authorization", scheme + " " + token);

        reconnect.connect();
        int responseCode = reconnect.getResponseCode();
        Log.v(TAG, "responseCode = " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw new UnauthorizedException("Unauthorized when requesting user profile with scheme " + scheme);
        }
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to retrieve user profile. HTTP status: " + responseCode);
        }

        String response;
        try (InputStream inputStream = reconnect.getInputStream()) {
            response = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
        Log.v(TAG, "response = " + response);
        return new JSONObject(response);
    }

    private static class UnauthorizedException extends IOException {
        UnauthorizedException(String message) {
            super(message);
        }
    }

    @Override
    protected void onPostExecute(UserSessionData userSessionData) {
        super.onPostExecute(userSessionData);
        if (userSessionData != null) {
            SharedPreferences prefs = context.getSharedPreferences(Utils.SHARED_PREF_NAME, 0);
            SharedPreferences.Editor e = prefs.edit();
            e.putString("userSessionData", userSessionData.toString()); // save "value" to the SharedPreferences
            e.apply();
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, context.getString(R.string.login_failed), LENGTH_SHORT).show();
        }
    }
}
