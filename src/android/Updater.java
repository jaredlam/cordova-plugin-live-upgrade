package com.appupdate.update;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;

/**
 * Created by Jared.M.Luo on 16/6/16.
 */
public class Updater {
    public static final String TAG = "APP_UPDATER";
    public static final String PREF_VERSION_KEY = "PREF_VERSION_KEY";

    private static Uri sRootUri;
    private static Uri sVersionRootUri;

    public static void init(Context context) {

        String version = getVersion(context);

        String rootPathStr = Environment.getExternalStorageDirectory() + "/LaborApp";
        sRootUri = Uri.fromFile(new File(rootPathStr));
        String wwwRootPathStr = rootPathStr + "/" + version;
        sVersionRootUri = Uri.fromFile(new File(wwwRootPathStr));
        File file = new File(wwwRootPathStr);
        if (!file.isDirectory()) {
            if (file.mkdirs()) {
                Log.i(TAG, "Folder has been created successfully");
            } else {
                Log.e(TAG, "Folder can't be created for some reason.");
            }
        } else {
            Log.i(TAG, "Folder already exist.");
        }

    }

    public static String getLaunchUrl(Context context) {
        String wwwRootPathStr = sRootUri.getPath() + "/" + getVersion(context);
        File launchFile = new File(wwwRootPathStr);
        if (launchFile.isDirectory() && launchFile.list() != null && launchFile.list().length > 0) {
            sVersionRootUri = Uri.fromFile(launchFile);
            return sVersionRootUri + "/index.html";
        }
        return null;
    }

    public static String getRootUrl() {
        return sRootUri.getPath();
    }

    public static void saveVersion(Context context, String version) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(PREF_VERSION_KEY, version).commit();
    }

    public static String getVersion(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(PREF_VERSION_KEY, null);
    }


}
