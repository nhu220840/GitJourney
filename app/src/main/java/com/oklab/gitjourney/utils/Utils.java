package com.oklab.gitjourney.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by olgakuklina on 2017-01-09.
 */

public final class Utils {
    public static final String SHARED_PREF_NAME = "com.ok.lab.GitJourney";
    public static final String DMY_DATE_FORMAT_PATTERN = "dd-MM-yyyy";
    public static final String USER_AGENT = "GitJourney";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Utils() {

    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Log.e("TAG", "verifyStoragePermissions");
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public static DateFormat createDateFormatterWithTimeZone(Context context, String pattern) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean timeZone = sharedPref.getBoolean("timezone_switch", true);
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.getDefault());
        if (timeZone) {
            formatter.setTimeZone(TimeZone.getDefault());
        } else {
            String customTimeZone = sharedPref.getString("timezone_list", TimeZone.getDefault().getID());
            formatter.setTimeZone(TimeZone.getTimeZone(customTimeZone));
        }
        return formatter;
    }

    public static String getStackTrace(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        e.printStackTrace(printer);
        printer.close();
        return writer.toString();
    }
}
