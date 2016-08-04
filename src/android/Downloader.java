package com.appupdate.update;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jared.M.Luo on 16/6/16.
 */
public class Downloader {

    public static final String IGNORE_VERSION_KEY = "IGNORE_VERSION_KEY";

    private static CallbackContext mCallbackContext;

    private static long mCurrentDownloadID;
    private static String mNewVersion;
    private static boolean mIsRequired;
    private static ManifestEntity mManifestEntity;

    private static UpdateListener mUpdateListener;

    private static boolean mDialogShowed = false;
    private static boolean mForceCheck;

    public static void init(CallbackContext callbackContext, Context context, boolean forceCheck) {
        mCallbackContext = callbackContext;
        mIsRequired = false;
        mManifestEntity = null;
        mForceCheck = forceCheck;
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                long downloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                if (mCurrentDownloadID == downloadID) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadID);
                    DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    Cursor download = downloadManager.query(query);
                    if (download.moveToFirst()) {
                        int urlIndex = download.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                        int nameIndex = download.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                        String fileUrl = download.getString(urlIndex);
                        String fileName = download.getString(nameIndex);
                        if (!TextUtils.isEmpty(fileName)) {
                            if (fileName.endsWith(".zip") && !TextUtils.isEmpty(mNewVersion)) {
                                dealWithZip(context, fileUrl);
                            } else if (fileName.endsWith(".json")) {
                                //upgrade config file
                                try {
                                    checkUpdate(context, fileUrl);
                                } catch (JSONException e) {
                                    if (mCallbackContext != null) {
                                        mCallbackContext.error("解析升级文件错误");
                                    }
                                    Log.e(Updater.TAG, "Parse manifest json error.");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    download.close();
                }
            }
        };
        context.registerReceiver(receiver, intentFilter);
    }

    private static void dealWithZip(Context context, String fileUrl) {
        //new package
        decompressZip(fileUrl, mNewVersion);
        if (mDialogShowed) {
            didUpdated(context);
        } else {
            showDialog(context, true);
        }
        mDialogShowed = false;
    }

    private static void checkUpdate(Context context, String fileUrl) throws JSONException {
        mManifestEntity = getManifestEntity(fileUrl);
        if (mManifestEntity != null) {
            boolean isIgnore = isIgnore(context, mManifestEntity);
            if (!isIgnore) {
                String localVersion = Updater.getVersion(context);
                boolean isOptional = isOptional(mManifestEntity, localVersion);
                boolean isRequired = isRequired(mManifestEntity, localVersion);

                mIsRequired = isRequired;
                if (isOptional || isRequired) {
                    if (isWifi(context)) {
                        //WIFI is available, just start downloading then show alert dialog to user
                        Downloader.downloadPackage(context, mManifestEntity.getDownload_url(), mManifestEntity.getLatest_version());
                    } else {
                        //WIFI is down, so show alert dialog to user to making them decide whether downloading new package.
                        showDialog(context, false);
                    }
                } else {
                    Log.i(Updater.TAG, "There is no new version.");
                    mCallbackContext.success("当前已经是最新版本");
                    return;
                }
            }

            if (mCallbackContext != null) {
                mCallbackContext.success();
            }
        }
    }

    private static boolean isIgnore(Context context, ManifestEntity manifestEntity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return !mForceCheck && preferences.getString(IGNORE_VERSION_KEY, "").trim().equals(manifestEntity.getLatest_version().trim());
    }

    private static boolean isOptional(ManifestEntity manifestEntity, String localVersion) {
        boolean isOptional = false;
        if (!TextUtils.isEmpty(localVersion)) {
            List<String> optionalVersions = manifestEntity.getOptional_versions();
            if (optionalVersions != null) {
                for (String optionalVersion : optionalVersions) {
                    if (localVersion.trim().equals(optionalVersion.trim())) {
                        isOptional = true;
                        break;
                    }
                }
            }
        }

        return isOptional;
    }

    private static boolean isRequired(ManifestEntity manifestEntity, String localVersion) {
        boolean isRequired = false;
        if (!TextUtils.isEmpty(localVersion)) {
            List<String> requiredVersions = manifestEntity.getRequired_versions();
            if (requiredVersions != null) {
                for (String requiredVersion : requiredVersions) {
                    if (localVersion.trim().equals(requiredVersion.trim())) {
                        isRequired = true;
                        break;
                    }
                }
            }
        }

        return isRequired;
    }

    private static ManifestEntity getManifestEntity(String fileUrl) throws JSONException {

        ManifestEntity manifest = null;

        Uri jsonFileUri = Uri.parse(fileUrl);
        if (jsonFileUri != null) {
            File jsonFile = new File(jsonFileUri.getPath());
            if (jsonFile.exists()) {

                JSONObject jsonObject = FileUtils.readJsonFromFile(jsonFile);
                if (jsonObject != null) {


                    JSONArray reqVersionJsonArr = jsonObject.getJSONArray(ManifestEntity.JSON_KEY_REQUIRED_VERSIONS);
                    List<String> reqVersions = new ArrayList<String>();
                    for (int i = 0; i < reqVersionJsonArr.length(); i++) {
                        reqVersions.add(reqVersionJsonArr.getString(i));
                    }

                    JSONArray opVersionJsonArr = jsonObject.getJSONArray(ManifestEntity.JSON_KEY_OPTIONAL_VERSIONS);
                    List<String> opVersions = new ArrayList<String>();
                    for (int i = 0; i < opVersionJsonArr.length(); i++) {
                        opVersions.add(opVersionJsonArr.getString(i));
                    }
                    String latestVersion = jsonObject.getString(ManifestEntity.JSON_KEY_LATEST_VERSION);
                    String note = jsonObject.getString(ManifestEntity.JSON_KEY_RELEASE_NOTE);
                    String downloadUrl = jsonObject.getString(ManifestEntity.JSON_KEY_DOWNLOAD_URL);
                    String title = jsonObject.getString(ManifestEntity.JSON_KEY_TITLE);
                    String confirmTxt = jsonObject.getString(ManifestEntity.JSON_KEY_CONFIRM_TEXT);
                    String cancelTxt = jsonObject.getString(ManifestEntity.JSON_KEY_CANCEL_TEXT);

                    manifest = new ManifestEntity();
                    manifest.setRequired_versions(reqVersions);
                    manifest.setOptional_versions(opVersions);
                    manifest.setLatest_version(latestVersion);
                    manifest.setRelease_note(note);
                    manifest.setDownload_url(downloadUrl);
                    manifest.setTitle(title);
                    manifest.setConfirm_text(confirmTxt);
                    manifest.setCancel_text(cancelTxt);
                }
            }
        }

        return manifest;
    }

    public static void downloadPackage(Context context, String url, String newVersion) {
        mNewVersion = newVersion;
        mCurrentDownloadID = 0;
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle("更新包下载中");
        File destFile = new File(context.getExternalCacheDir() + "/" + uri.getLastPathSegment());
        if (destFile.exists()) {
            dealWithZip(context, Uri.fromFile(destFile).toString());
        } else {
            request.setDestinationUri(Uri.fromFile(destFile));
            mCurrentDownloadID = downloadManager.enqueue(request);
        }
    }

    public static void downloadManifest(Context context, String url) {
        mCurrentDownloadID = 0;
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        File destFile = new File(context.getExternalCacheDir() + "/" + uri.getLastPathSegment());
        boolean canDownload = true;
        if (destFile.exists()) {
            canDownload = destFile.delete();
        }

        if (canDownload) {
            request.setDestinationUri(Uri.fromFile(destFile));
            mCurrentDownloadID = downloadManager.enqueue(request);
        } else {
            mCallbackContext.error("升级文件已存在，并删除失败");
            Log.e(Updater.TAG, "Download manifest file is exist but can not be removed.");
        }
    }

    private static File decompressZip(String fileUri, String version) {
        File file = new File(URI.create(fileUri));
        String wwwRoot = Updater.getRootUrl();

        File destFile = new File(wwwRoot + "/" + version);
        try {
            FileUtils.deleteRecursive(destFile);
            Decompressor.unzip(file, destFile);
        } catch (IOException e) {
            e.printStackTrace();
            mCallbackContext.error("文件解压失败");
            Log.e(Updater.TAG, "decompressed IOException");
        }

        String fileNameWithOutExt = file.getName().replaceFirst("[.][^.]+$", "");
        File unzippedFile = new File(destFile.getAbsolutePath() + "/" + fileNameWithOutExt);
        try {
            FileUtils.copyDirectory(unzippedFile, destFile);
            FileUtils.deleteRecursive(unzippedFile);
        } catch (IOException e) {
            e.printStackTrace();
            mCallbackContext.error("拷贝解压文件目录失败");
            Log.e(Updater.TAG, "copy directory IOException");
        }

        return destFile;
    }

    private static void showDialog(final Context context, final boolean alreadyDownload) {
        mDialogShowed = true;
        if (mManifestEntity != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(mManifestEntity.getTitle())
                    .setMessage(mManifestEntity.getRelease_note())
                    .setPositiveButton(mManifestEntity.getConfirm_text(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (alreadyDownload) {
                                didUpdated(context);
                            } else {
                                Downloader.downloadPackage(context, mManifestEntity.getDownload_url(), mManifestEntity.getLatest_version());
                            }
                        }
                    });

            if (mIsRequired) {
                builder.setCancelable(false).show();
            } else {
                builder.setCancelable(false)
                        .setNegativeButton(mManifestEntity.getCancel_text(), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                                preferences.edit().putString(IGNORE_VERSION_KEY, mManifestEntity.getLatest_version()).commit();
                            }
                        }).show();
            }
        }
    }

    private static void didUpdated(Context context) {
        Updater.saveVersion(context, mNewVersion);
        if (mUpdateListener != null) {
            mUpdateListener.onUpdated();
            mUpdateListener = null;
        }
    }

    private static boolean isWifi(Context context) {
        ConnectivityManager mConnectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager mTelephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        // Skip if no connection, or background data disabled
        NetworkInfo info = mConnectivity.getActiveNetworkInfo();
        if (info == null || !mConnectivity.getBackgroundDataSetting()) {
            return false;
        }

        // Only update if WiFi or 3G is connected and not roaming
        int netType = info.getType();
        int netSubtype = info.getSubtype();
        if (netType == ConnectivityManager.TYPE_WIFI) {
            return info.isConnected();
        } else if (netType == ConnectivityManager.TYPE_MOBILE
                && netSubtype == TelephonyManager.NETWORK_TYPE_UMTS
                && !mTelephony.isNetworkRoaming()) {
            return info.isConnected();
        } else {
            return false;
        }
    }

    public static void setUpdateListener(UpdateListener updateListener) {
        mUpdateListener = updateListener;
    }

    public interface UpdateListener {
        void onUpdated();
    }

}
