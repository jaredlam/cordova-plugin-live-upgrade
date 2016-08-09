package com.appupdate.update;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONException;

/**
 * Created by Jared.M.Luo on 16/6/20.
 */
public class AppUpdate extends CordovaPlugin {
    private CordovaWebView mWebView;
    private boolean mCheckedUpdate = false;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mWebView = webView;
        Downloader.init(this.cordova.getActivity());
    }

    @Override
    public boolean execute(String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if ("Update".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    execUpdate(args, callbackContext);
                }
            });
            return true;
        }
        return super.execute(action, args, callbackContext);
    }

    public boolean execUpdate(CordovaArgs args, CallbackContext callbackContext) {

        try {
            String localVersion = args.getString(0);
            String manifestUrl = args.getString(1);
            boolean forceCheck = false;
            if (!args.isNull(2)) {
                forceCheck = args.getBoolean(2);
            }

            Updater.setDefaultVersion(localVersion);
            Downloader.setForceCheck(forceCheck);
            Downloader.setCallbackContext(callbackContext);

            if (!mCheckedUpdate || forceCheck) {
                mCheckedUpdate = true;

                Downloader.downloadManifest(this.cordova.getActivity(), manifestUrl);
                Downloader.setCallbackContext(callbackContext);
                Downloader.setUpdateListener(new Downloader.UpdateListener() {
                    @Override
                    public void onUpdated() {
                        redirectTo();
                    }
                });
            } else {
                callbackContext.success("");
            }
        } catch (JSONException e) {
            callbackContext.error("升级插件获取参数不正确");
        }
        return true;
    }

    public void redirectTo() {
        final CordovaInterface cordova = this.cordova;
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = cordova.getActivity();
                PendingIntent RESTART_INTENT = PendingIntent.getActivity(activity, 0, new Intent(activity.getIntent()), activity.getIntent().getFlags());
                AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, RESTART_INTENT);
                System.exit(2);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Downloader.onDestory(this.cordova.getActivity());
    }
}
