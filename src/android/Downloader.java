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

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Created by Jared.M.Luo on 16/6/16.
 */
public class Downloader {

    private static long mCurrentDownloadID;
    private static String mVersion;

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
                        if (!TextUtils.isEmpty(fileName) && fileName.endsWith(".zip") && !TextUtils.isEmpty(mVersion)) {
                            decompressZip(fileUrl, mVersion);
                            Updater.saveVersion(context, mVersion);
                        }
                    }

                    download.close();
                }
            }
        };
        context.registerReceiver(receiver, intentFilter);
    }

    public static void download(Context context, String url, String version) {
        mVersion = version;
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
            Log.e(Updater.TAG, "Download file is exist but can not be removed.");
        }
    }

    private static File decompressZip(String fileUri, String version) {
        File file = new File(URI.create(fileUri));
        String wwwRoot = Updater.getRootUrl();

        File destFile = new File(wwwRoot + "/" + version);
        try {
            Decompressor.unzip(file, destFile);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(Updater.TAG, "decompressed IOException");
        }

        String fileNameWithOutExt = file.getName().replaceFirst("[.][^.]+$", "");
        File unzippedFile = new File(destFile.getAbsolutePath() + "/" + fileNameWithOutExt);
        try {
            FileUtils.copyDirectory(unzippedFile, destFile);
            FileUtils.deleteFolder(unzippedFile);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(Updater.TAG, "copy directory IOException");
        }

        return destFile;
    }

}
