package com.oklab.gitjourney.asynctasks;

import android.content.Context;
import androidx.loader.content.AsyncTaskLoader;
import android.util.Log;

import com.oklab.gitjourney.R;
import com.oklab.gitjourney.data.HTTPConnectionResult;
import com.oklab.gitjourney.data.ReposDataEntry;
import com.oklab.gitjourney.parsers.ReposParser;
import com.oklab.gitjourney.services.FetchHTTPConnectionService;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

/**
 * Created by olgakuklina on 2017-04-01.
 */

public class GitHubUserRepositoriesLoader extends AsyncTaskLoader<List<ReposDataEntry>> {
    private static final String TAG = GitHubUserRepositoriesLoader.class.getSimpleName();
    private final int page;
    private final String userName;
    private final boolean owner;

    public GitHubUserRepositoriesLoader(Context context, int page, String userName, boolean owner) {
        super(context);
        this.page = page;
        this.userName = userName;
        this.owner = owner;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public List<ReposDataEntry> loadInBackground() {
        String uri;
        if (owner) {
            uri = getContext().getString(R.string.url_repos, page);
        } else {
            uri = getContext().getString(R.string.url_user_repos, page, userName);
        }
        FetchHTTPConnectionService fetchHTTPConnectionService = new FetchHTTPConnectionService(uri, getContext());
        HTTPConnectionResult result = fetchHTTPConnectionService.establishConnection();
        Log.v(TAG, "responseCode = " + result.getResponceCode());
        Log.v(TAG, "result = " + result.getResult());

        try {
            JSONArray jsonArray = new JSONArray(result.getResult());
            return new ReposParser().parse(jsonArray);

        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }
}
