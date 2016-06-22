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

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mWebView = webView;
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

            Updater.init(this.cordova.getActivity(), localVersion);
            Downloader.init(this.cordova.getActivity());

            redirectTo();

            Downloader.downloadManifest(this.cordova.getActivity(), manifestUrl);
            Downloader.setRestartListener(new Downloader.RestartListener() {
                @Override
                public void onRestart() {
                    redirectTo();
                }
            });
            callbackContext.success();
        } catch (JSONException e) {
            callbackContext.error("Cannot get local version or manifest url.");
        }
        return true;
    }

    public void redirectTo() {
        String url = Updater.getLaunchUrl(this.cordova.getActivity());
        if (!TextUtils.isEmpty(url)) {
            this.mWebView.loadUrlIntoView(url, false);
        }
    }
}
