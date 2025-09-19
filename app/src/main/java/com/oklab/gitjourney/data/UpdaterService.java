package com.oklab.gitjourney.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.oklab.gitjourney.BuildConfig;
import com.oklab.gitjourney.R;
import com.oklab.gitjourney.adapters.FirebaseAnalyticsWrapper;
import com.oklab.gitjourney.mock.MockDataSource;
import com.oklab.gitjourney.parsers.ContributionsParser;
import com.oklab.gitjourney.utils.Utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by olgakuklina on 2017-03-18.
 */

public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";
    private static final String fbAEvent = "Get contributions failed Stack Trace";
    private UserSessionData currentSessionData;
    private FirebaseAnalyticsWrapper firebaseAnalytics;

    public UpdaterService() {
        super(UpdaterService.TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = this.getSharedPreferences(Utils.SHARED_PREF_NAME, 0);
        String sessionDataStr = prefs.getString("userSessionData", null);
        currentSessionData = UserSessionData.createUserSessionDataFromString(sessionDataStr);
        if (BuildConfig.USE_MOCK_DATA && currentSessionData == null) {
            currentSessionData = MockDataSource.ensureMockSession(this);
        }
        firebaseAnalytics = new FirebaseAnalyticsWrapper(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (BuildConfig.USE_MOCK_DATA) {
            populateWithMockData();
            return;
        }
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Log.w(UpdaterService.TAG, "Not online, not refreshing.");
            return;
        }
        ArrayList<ContentProviderOperation> cpo = new ArrayList<ContentProviderOperation>();
        Uri dirUri = ActivityItemsContract.Items.buildDirUri();
        cpo.add(ContentProviderOperation.newDelete(dirUri).build());

        int pageIndex = 1;
        while (true) {
            Log.v(TAG, "reading page # = " + pageIndex);
            List<ContributionDataEntry> contributionsDataList = readActivityLog(pageIndex++);

            if (contributionsDataList == null || contributionsDataList.isEmpty()) {
                Log.v(TAG, "contributionsDataList number of elements = 0");

                break;
            }
            for (ContributionDataEntry entry : contributionsDataList) {
                ContentValues values = new ContentValues();
                values.put(ActivityItemsContract.Items._ID, entry.getEntryId());
                values.put(ActivityItemsContract.Items.ENTRY_URL, entry.getEntryURL());
                values.put(ActivityItemsContract.Items.ACTION_TYPE, entry.getActionType().name());
                values.put(ActivityItemsContract.Items.DESCRIPTION, entry.getDescription());
                values.put(ActivityItemsContract.Items.TITLE, entry.getTitle());
                values.put(ActivityItemsContract.Items.AUTHOR_ID, currentSessionData.getLogin());
                values.put(ActivityItemsContract.Items.PUBLISHED_DATE, entry.getDate().getTimeInMillis());
                cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
            }
        }
        try {
            this.getContentResolver().applyBatch(ActivityItemsContract.CONTENT_AUTHORITY, cpo);
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(UpdaterService.TAG, "Error updating content.", e);
            Bundle bundle = new Bundle();
            bundle.putString(TAG, Utils.getStackTrace(e));
            firebaseAnalytics.logEvent(fbAEvent, bundle);
        }
    }

    private void populateWithMockData() {
        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
        Uri dirUri = ActivityItemsContract.Items.buildDirUri();
        cpo.add(ContentProviderOperation.newDelete(dirUri).build());

        int pageIndex = 1;
        String login = currentSessionData != null ? currentSessionData.getLogin() : "mockuser";
        while (true) {
            List<ContributionDataEntry> contributionsDataList = MockDataSource.getContributions(login, pageIndex++);
            if (contributionsDataList == null || contributionsDataList.isEmpty()) {
                break;
            }
            for (ContributionDataEntry entry : contributionsDataList) {
                ContentValues values = new ContentValues();
                values.put(ActivityItemsContract.Items._ID, entry.getEntryId());
                values.put(ActivityItemsContract.Items.ENTRY_URL, entry.getEntryURL());
                values.put(ActivityItemsContract.Items.ACTION_TYPE, entry.getActionType().name());
                values.put(ActivityItemsContract.Items.DESCRIPTION, entry.getDescription());
                values.put(ActivityItemsContract.Items.TITLE, entry.getTitle());
                values.put(ActivityItemsContract.Items.AUTHOR_ID, login);
                values.put(ActivityItemsContract.Items.PUBLISHED_DATE, entry.getDate().getTimeInMillis());
                cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
            }
        }
        try {
            this.getContentResolver().applyBatch(ActivityItemsContract.CONTENT_AUTHORITY, cpo);
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(UpdaterService.TAG, "Error updating content.", e);
            Bundle bundle = new Bundle();
            bundle.putString(TAG, Utils.getStackTrace(e));
            firebaseAnalytics.logEvent(fbAEvent, bundle);
        }
    }

    private List<ContributionDataEntry> readActivityLog(int page) {
        try {
            HttpURLConnection connect = (HttpURLConnection) new URL(this.getString(R.string.url_events, page, currentSessionData.getLogin())).openConnection();
            connect.setRequestMethod("GET");

            Log.v(TAG, "current session data " + currentSessionData);
            String authentication = "token " + currentSessionData.getToken();
            connect.setRequestProperty("Authorization", authentication);
            connect.setRequestProperty("User-Agent", Utils.USER_AGENT);

            connect.connect();
            int responseCode = connect.getResponseCode();

            Log.v(TAG, "responseCode = " + responseCode);
            InputStream inputStream = connect.getInputStream();
            String response = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Log.v(TAG, "response = " + response);
            JSONArray jsonArray = new JSONArray(response);

            return new ContributionsParser(this).parse(jsonArray);

        } catch (Exception e) {
            Log.e(TAG, "Get contributions failed", e);
            Bundle bundle = new Bundle();
            bundle.putString(TAG, Utils.getStackTrace(e));
            firebaseAnalytics.logEvent(fbAEvent, bundle);
            return null;
        }
    }
}