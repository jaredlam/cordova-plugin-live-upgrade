package com.appupdate.update;

import android.text.TextUtils;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.ConfigXmlParser;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaPreferences;
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

        ConfigXmlParser parser = new ConfigXmlParser();
        parser.parse(this.cordova.getActivity());
        CordovaPreferences preferences = parser.getPreferences();
        String version = preferences.getString("app-update-version", "1.0");

        Updater.init(this.cordova.getActivity());
        Downloader.init(this.cordova.getActivity());
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        if ("Update".equals(action)) {
            return execUpdate(callbackContext);
        }
        return super.execute(action, args, callbackContext);
    }

    public boolean execUpdate(CallbackContext callbackContext) {

        String url = Updater.getLaunchUrl(this.cordova.getActivity());
        if (!TextUtils.isEmpty(url)) {
            this.mWebView.loadUrlIntoView(url, false);
        }

        Downloader.download(this.cordova.getActivity(), "http://172.16.0.246:8082/labor_AppMsite_release_0.0.5.zip", "2.0");
        callbackContext.success();
        return true;
    }
}
