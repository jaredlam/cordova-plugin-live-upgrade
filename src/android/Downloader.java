package com.appupdate.update;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

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

    private static long mCurrentDownloadID;
    private static String mNewVersion;

    public static void init(Context context) {
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
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
                                //new package
                                decompressZip(fileUrl, mNewVersion);
                                Updater.saveVersion(context, mNewVersion);
                            } else if (fileName.endsWith(".json")) {
                                //upgrade config file
                                checkUpdate(context, fileUrl);
                            }
                        }
                    }

                    download.close();
                }
            }
        };
        context.registerReceiver(receiver, intentFilter);
    }

    private static void checkUpdate(Context context, String fileUrl) {
        ManifestEntity manifestEntity = getManifestEntity(fileUrl);
        if (manifestEntity != null) {
            String localVersion = Updater.getVersion(context);
            if (isOptional(manifestEntity, localVersion)) {
                Downloader.downloadPackage(context, manifestEntity.getDownload_url(), manifestEntity.getLatest_version());
            } else if (isRequired(manifestEntity, localVersion)) {
                Downloader.downloadPackage(context, manifestEntity.getDownload_url(), manifestEntity.getLatest_version());
            }
        } else {
            Log.e(Updater.TAG, "manifest entity deserialization failed.");
        }
    }

    private static boolean isOptional(ManifestEntity manifestEntity, String localVersion) {
        boolean isOptional = false;
        if (!TextUtils.isEmpty(localVersion)) {
            List<String> optionalVersions = manifestEntity.getOptional_versions();
            if (optionalVersions != null) {
                for (String optionalVersion : optionalVersions) {
                    if (localVersion.equals(optionalVersion)) {
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
                    if (localVersion.equals(requiredVersion)) {
                        isRequired = true;
                        break;
                    }
                }
            }
        }

        return isRequired;
    }

    private static ManifestEntity getManifestEntity(String fileUrl) {

        ManifestEntity manifest = null;

        Uri jsonFileUri = Uri.parse(fileUrl);
        if (jsonFileUri != null) {
            File jsonFile = new File(jsonFileUri.getPath());
            if (jsonFile.exists()) {
                try {
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

                        manifest = new ManifestEntity();
                        manifest.setRequired_versions(reqVersions);
                        manifest.setOptional_versions(opVersions);
                        manifest.setLatest_version(latestVersion);
                        manifest.setRelease_note(note);
                        manifest.setDownload_url(downloadUrl);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(Updater.TAG, "Parse manifest json error.");
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
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        File destFile = new File(context.getExternalCacheDir() + "/" + uri.getLastPathSegment());
        boolean canDownload = true;
        if (destFile.exists()) {
            canDownload = destFile.delete();
        }

        if (canDownload) {
            request.setDestinationUri(Uri.fromFile(destFile));
            mCurrentDownloadID = downloadManager.enqueue(request);
        } else {
            Log.e(Updater.TAG, "Download package file is exist but can not be removed.");
        }
    }

    public static void downloadManifest(Context context, String url) {
        mCurrentDownloadID = 0;
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        File destFile = new File(context.getExternalCacheDir() + "/" + uri.getLastPathSegment());
        boolean canDownload = true;
        if (destFile.exists()) {
            canDownload = destFile.delete();
        }

        if (canDownload) {
            request.setDestinationUri(Uri.fromFile(destFile));
            mCurrentDownloadID = downloadManager.enqueue(request);
        } else {
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
            Log.e(Updater.TAG, "decompressed IOException");
        }

        String fileNameWithOutExt = file.getName().replaceFirst("[.][^.]+$", "");
        File unzippedFile = new File(destFile.getAbsolutePath() + "/" + fileNameWithOutExt);
        try {
            FileUtils.copyDirectory(unzippedFile, destFile);
            FileUtils.deleteRecursive(unzippedFile);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(Updater.TAG, "copy directory IOException");
        }

        return destFile;
    }

}
