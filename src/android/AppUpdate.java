package com.appupdate.update;

import android.text.TextUtils;

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
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        if ("Update".equals(action)) {
            return execUpdate(args, callbackContext);
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
            Updater.init(this.cordova.getActivity(), localVersion);
            Downloader.setForceCheck(forceCheck);
            Downloader.setCallbackContext(callbackContext);

            if (!mCheckedUpdate || forceCheck) {
                mCheckedUpdate = true;

                if(!forceCheck) {
                    redirectTo();
                }

                Downloader.downloadManifest(this.cordova.getActivity(), manifestUrl);
                Downloader.setCallbackContext(callbackContext);
                Downloader.setUpdateListener(new Downloader.UpdateListener() {
                    @Override
                    public void onUpdated() {
                        redirectTo();
                    }
                });
            }else {
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
                String url = Updater.getLaunchUrl(cordova.getActivity());
                if (!TextUtils.isEmpty(url)) {
                    mWebView.loadUrlIntoView(url, false);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Downloader.onDestory(this.cordova.getActivity());
    }
}
