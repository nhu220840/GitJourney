package com.oklab.gitjourney.asynctasks;


import android.content.Context;
import androidx.loader.content.AsyncTaskLoader;
import android.util.Log;

import com.oklab.gitjourney.R;
import com.oklab.gitjourney.data.GitHubUsersDataEntry;
import com.oklab.gitjourney.data.HTTPConnectionResult;
import com.oklab.gitjourney.parsers.FollowersParser;
import com.oklab.gitjourney.services.FetchHTTPConnectionService;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

/**
 * Created by olgakuklina on 2017-03-29.
 */

public class FollowingLoader extends AsyncTaskLoader<List<GitHubUsersDataEntry>> {
    private static final String TAG = FollowingLoader.class.getSimpleName();
    private final int page;

    public FollowingLoader(Context context, int page) {
        super(context);
        this.page = page;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public List<GitHubUsersDataEntry> loadInBackground() {
        String uri = getContext().getString(R.string.url_following, page);
        FetchHTTPConnectionService fetchHTTPConnectionService = new FetchHTTPConnectionService(uri, getContext());
        HTTPConnectionResult result = fetchHTTPConnectionService.establishConnection();
        if (result == null) {
            return null;
        }
        Log.v(TAG, "responseCode = " + result.getResponceCode());
        Log.v(TAG, "result = " + result.getResult());

        try {
            JSONArray jsonArray = new JSONArray(result.getResult());
            return new FollowersParser().parse(jsonArray);

        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }
}
