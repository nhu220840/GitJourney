package com.oklab.gitjourney.mock;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.oklab.gitjourney.data.HTTPConnectionResult;
import com.oklab.gitjourney.data.UserSessionData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Routes HTTP requests to in-memory mock data when mock mode is enabled.
 */
public final class MockHttpDispatcher {
    private static final String TAG = MockHttpDispatcher.class.getSimpleName();

    private MockHttpDispatcher() {
    }

    public static HTTPConnectionResult dispatch(String uriString, UserSessionData sessionData) {
        try {
            Uri uri = Uri.parse(uriString);
            if (MockDataSource.MOCK_HOST.equals(uri.getHost())) {
                return handleMockHost(uri);
            }
            if (!"api.github.com".equals(uri.getHost())) {
                return new HTTPConnectionResult("", 404);
            }
            List<String> segments = uri.getPathSegments();
            if (segments.isEmpty()) {
                return new HTTPConnectionResult("{}", 200);
            }
            int page = parsePage(uri);
            String currentLogin = sessionData != null ? sessionData.getLogin() : "mockuser";
            String first = segments.get(0);
            if ("user".equals(first)) {
                return handleUserScope(segments, page, currentLogin);
            }
            if ("users".equals(first)) {
                return handleUsersScope(segments, page);
            }
            if ("repos".equals(first)) {
                return handleReposScope(segments, uri);
            }
            return new HTTPConnectionResult("{}", 200);
        } catch (Exception exception) {
            Log.e(TAG, "Failed to serve mock response for " + uriString, exception);
            return new HTTPConnectionResult("", 500);
        }
    }

    private static HTTPConnectionResult handleMockHost(Uri uri) {
        List<String> segments = uri.getPathSegments();
        if (segments.size() < 5) {
            return new HTTPConnectionResult("", 404);
        }
        String owner = segments.get(1);
        String repo = segments.get(2);
        String path = joinSegments(segments, 4);
        String content = MockDataSource.getFileContent(owner, repo, path);
        if (content == null) {
            return new HTTPConnectionResult("", 404);
        }
        return new HTTPConnectionResult(content, 200);
    }

    private static HTTPConnectionResult handleUserScope(List<String> segments, int page, String currentLogin)
            throws JSONException {
        if (segments.size() == 1) {
            JSONObject profile = MockDataSource.buildUserProfileJson(currentLogin);
            return new HTTPConnectionResult(profile.toString(), 200);
        }
        String second = segments.get(1);
        switch (second) {
            case "repos": {
                JSONArray array = MockDataSource.buildRepositoriesJson(currentLogin, page);
                return new HTTPConnectionResult(array.toString(), 200);
            }
            case "starred": {
                JSONArray array = MockDataSource.buildStarredRepositoriesJson(page);
                return new HTTPConnectionResult(array.toString(), 200);
            }
            case "followers": {
                JSONArray array = MockDataSource.buildFollowersJson(page);
                return new HTTPConnectionResult(array.toString(), 200);
            }
            case "following": {
                JSONArray array = MockDataSource.buildFollowingJson(page);
                return new HTTPConnectionResult(array.toString(), 200);
            }
            default:
                return new HTTPConnectionResult("{}", 200);
        }
    }

    private static HTTPConnectionResult handleUsersScope(List<String> segments, int page) throws JSONException {
        if (segments.size() == 2) {
            String login = segments.get(1);
            JSONObject profile = MockDataSource.buildUserProfileJson(login);
            return new HTTPConnectionResult(profile.toString(), 200);
        }
        if (segments.size() >= 3) {
            String login = segments.get(1);
            String third = segments.get(2);
            if ("repos".equals(third)) {
                JSONArray array = MockDataSource.buildRepositoriesJson(login, page);
                return new HTTPConnectionResult(array.toString(), 200);
            }
            if ("events".equals(third)) {
                JSONArray array = MockDataSource.buildEventsJson(login, page);
                return new HTTPConnectionResult(array.toString(), 200);
            }
        }
        return new HTTPConnectionResult("{}", 200);
    }

    private static HTTPConnectionResult handleReposScope(List<String> segments, Uri uri) throws JSONException {
        if (segments.size() < 3) {
            return new HTTPConnectionResult("{}", 200);
        }
        String owner = segments.get(1);
        String repo = segments.get(2);
        if (segments.size() == 4 && "readme".equals(segments.get(3))) {
            JSONObject json = MockDataSource.buildRepoReadmeJson(owner, repo);
            return new HTTPConnectionResult(json.toString(), 200);
        }
        if (segments.size() >= 4 && "contents".equals(segments.get(3))) {
            String path = "";
            if (segments.size() > 4) {
                path = joinSegments(segments, 4);
            }
            JSONArray array = MockDataSource.buildRepoContentJson(owner, repo, path);
            return new HTTPConnectionResult(array.toString(), 200);
        }
        return new HTTPConnectionResult("{}", 200);
    }

    private static int parsePage(Uri uri) {
        String pageValue = uri.getQueryParameter("page");
        if (TextUtils.isEmpty(pageValue)) {
            return 1;
        }
        try {
            return Integer.parseInt(pageValue);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private static String joinSegments(List<String> segments, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < segments.size(); i++) {
            if (builder.length() > 0) {
                builder.append('/');
            }
            builder.append(segments.get(i));
        }
        return builder.toString();
    }
}