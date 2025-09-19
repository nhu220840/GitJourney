package com.oklab.gitjourney.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.oklab.gitjourney.BuildConfig;
import com.oklab.gitjourney.data.HTTPConnectionResult;
import com.oklab.gitjourney.data.UserSessionData;
import com.oklab.gitjourney.mock.MockDataSource;
import com.oklab.gitjourney.mock.MockHttpDispatcher;
import com.oklab.gitjourney.utils.Utils;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created by olgakuklina on 2017-03-22.
 */

public class FetchHTTPConnectionService {
    private static final String TAG = FetchHTTPConnectionService.class.getSimpleName();
    private final String uri;
    private final UserSessionData currentSessionData;

    public FetchHTTPConnectionService(String uri, UserSessionData currentSessionData) {
        this.uri = uri;
        this.currentSessionData = currentSessionData;
    }

    public FetchHTTPConnectionService(String uri, Context context) {
        this.uri = uri;
        SharedPreferences prefs = context.getSharedPreferences(Utils.SHARED_PREF_NAME, 0);
        String sessionDataStr = prefs.getString("userSessionData", null);
//        currentSessionData = UserSessionData.createUserSessionDataFromString(sessionDataStr);
        UserSessionData sessionData = UserSessionData.createUserSessionDataFromString(sessionDataStr);
        if (BuildConfig.USE_MOCK_DATA && sessionData == null) {
            sessionData = MockDataSource.ensureMockSession(context);
        }
        currentSessionData = sessionData;
    }

    public HTTPConnectionResult establishConnection() {
        try {
            if (BuildConfig.USE_MOCK_DATA) {
                HTTPConnectionResult result = MockHttpDispatcher.dispatch(uri, currentSessionData);
                if (result != null) {
                    return result;
                }
            }
            if (currentSessionData == null) {
                Log.w(TAG, "No session data available for network request in non-mock mode");
                return null;
            }
            HttpURLConnection connect = (HttpURLConnection) new URL(uri).openConnection();
            connect.setRequestMethod("GET");

            String authentication = "token " + currentSessionData.getToken();
            connect.setRequestProperty("Authorization", authentication);
            connect.setRequestProperty("User-Agent", Utils.USER_AGENT);

            connect.connect();
            int responseCode = connect.getResponseCode();

            Log.v(TAG, "responseCode = " + responseCode);
            InputStream inputStream = connect.getInputStream();
            Log.v(TAG, "inputStream " + inputStream);
            String response = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Log.v(TAG, "response = " + response);
            return new HTTPConnectionResult(response, responseCode);

        } catch (Exception e) {
            Log.e(TAG, "Get data failed", e);
            return null;
        }
    }
}
