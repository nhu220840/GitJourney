package com.oklab.gitjourney.asynctasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.oklab.gitjourney.R;
import com.oklab.gitjourney.data.HTTPConnectionResult;
import com.oklab.gitjourney.data.UserSessionData;
import com.oklab.gitjourney.parsers.Parser;
import com.oklab.gitjourney.services.FetchHTTPConnectionService;
import com.oklab.gitjourney.utils.Utils;
import com.oklab.gitjourney.data.GitHubUserProfileDataEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by olgakuklina on 2017-03-21.
 */

public class UserProfileAsyncTask<T> extends AsyncTask<String, Void, T> {

    private static final String TAG = UserProfileAsyncTask.class.getSimpleName();
    private final Context context;
    private final UserProfileAsyncTask.OnProfilesLoadedListener<T> listener;
    private final Parser<T> parser;
    private UserSessionData currentSessionData;

    public UserProfileAsyncTask(Context context, UserProfileAsyncTask.OnProfilesLoadedListener<T> listener, Parser<T> parser) {
        this.context = context;
        this.listener = listener;
        this.parser = parser;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        SharedPreferences prefs = context.getSharedPreferences(Utils.SHARED_PREF_NAME, 0);
        String sessionDataStr = prefs.getString("userSessionData", null);
        currentSessionData = UserSessionData.createUserSessionDataFromString(sessionDataStr);
    }

    @Override
    protected T doInBackground(String... args) {
//        String uri;
//        if (args.length > 0) {
//            String login = args[0];
//            uri = context.getString(R.string.url_users, login);
//        } else {
//            uri = context.getString(R.string.url_user);
//        }
//        FetchHTTPConnectionService connectionFetcher = new FetchHTTPConnectionService(uri, currentSessionData);
//        HTTPConnectionResult result = connectionFetcher.establishConnection();
//        Log.v(TAG, "responseCode = " + result.getResponceCode());
//        Log.v(TAG, "response = " + result.getResult());
//
//        try {
//            JSONObject jsonObject = new JSONObject(result.getResult());
//            return parser.parse(jsonObject);
//
//        } catch (JSONException e) {
//            Log.e(TAG, "", e);
//        }
//        return null;
        String login;
        if (args.length > 0) {
            login = args[0];
        } else {
            login = currentSessionData.getLogin();
        }

        if ("dummy_user".equals(login)) {
            // Trả về dữ liệu hồ sơ giả lập cho người dùng hiện tại
            String name = "Dummy User";
            String avatarUrl = "https://avatars.githubusercontent.com/u/999?v=4";
            String profileUri = "https://api.github.com/users/dummy_user";
            String location = "Ha Noi, Viet Nam";
            String company = "Dummy Inc.";
            String blogURI = "https://dummyblog.com";
            String email = "dummy@example.com";
            String bio = "This is a mock bio for the main user profile.";
            int publicRepos = 50;
            int publicGists = 10;
            int followers = 100;
            int following = 80;
            Calendar createdAt = Calendar.getInstance();

            return (T) new GitHubUserProfileDataEntry(
                    name, avatarUrl, profileUri, location, login, company, blogURI,
                    email, bio, publicRepos, publicGists, followers, following, createdAt
            );
        }

        return null;
    }

    @Override
    protected void onPostExecute(T entry) {
        super.onPostExecute(entry);
        listener.OnProfilesLoaded(entry);
    }

    public interface OnProfilesLoadedListener<T> {
        void OnProfilesLoaded(T dataEntry);
    }
}