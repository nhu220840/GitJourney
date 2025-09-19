package com.oklab.gitjourney.asynctasks;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.loader.content.AsyncTaskLoader;
import android.util.Log;

import com.oklab.gitjourney.BuildConfig;
import com.oklab.gitjourney.R;
import com.oklab.gitjourney.data.UserSessionData;
import com.oklab.gitjourney.mock.MockDataSource;
import com.oklab.gitjourney.parsers.AtomParser;
import com.oklab.gitjourney.utils.Utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by olgakuklina on 2017-03-30.
 */

public class FeedListLoader<T> extends AsyncTaskLoader<List<T>> {
    private static final String TAG = FeedListLoader.class.getSimpleName();
    private final int page;
    private final AtomParser<T> feedAtomParser;
    private UserSessionData currentSessionData;
    private Context context;

    public FeedListLoader(Context context, int page, AtomParser<T> feedAtomParser) {
        super(context);
        this.context = context;
        this.page = page;
        this.feedAtomParser = feedAtomParser;
        SharedPreferences prefs = context.getSharedPreferences(Utils.SHARED_PREF_NAME, 0);
        String sessionDataStr = prefs.getString("userSessionData", null);
        currentSessionData = UserSessionData.createUserSessionDataFromString(sessionDataStr);
        if (BuildConfig.USE_MOCK_DATA && currentSessionData == null) {
            currentSessionData = MockDataSource.ensureMockSession(context);
        }
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> loadInBackground() {
        if (BuildConfig.USE_MOCK_DATA) {
            return (List<T>) MockDataSource.getFeedEntries(page);
        }
        if (currentSessionData == null) {
            Log.w(TAG, "Session data missing while fetching feeds");
            return null;
        }
        try {
            HttpURLConnection connect = (HttpURLConnection) new URL(context.getString(R.string.url_feeds, page)).openConnection();
            connect.setRequestMethod("GET");

            String authentication = "basic " + currentSessionData.getCredentials();
            connect.setRequestProperty("Authorization", authentication);
            connect.setRequestProperty("User-Agent", Utils.USER_AGENT);

            connect.connect();
            int responseCode = connect.getResponseCode();

            Log.v(TAG, "responseCode = " + responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }
            InputStream inputStream = connect.getInputStream();
            String response = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Log.v(TAG, "response = " + response);
            JSONObject jObj = new JSONObject(response);

            String currentUserURL = jObj.getString("current_user_url") + "&page=" + page;
            Log.v(TAG, "currentUserURL = " + currentUserURL);
            return feedAtomParser.parse(currentUserURL);

        } catch (Exception e) {
            Log.e(TAG, "Get user feeds failed", e);
            return null;
        }
    }
}