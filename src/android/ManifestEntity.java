package com.appupdate.update;

import java.util.List;

/**
 * Created by Jared.M.Luo on 16/6/21.
 */
public class ManifestEntity {

    public static final String JSON_KEY_REQUIRED_VERSIONS = "required_versions";
    public static final String JSON_KEY_OPTIONAL_VERSIONS = "optional_versions";
    public static final String JSON_KEY_LATEST_VERSION = "latest_version";
    public static final String JSON_KEY_RELEASE_NOTE = "release_note";
    public static final String JSON_KEY_DOWNLOAD_URL = "download_url";

    private List<String> required_versions;
    private List<String> optional_versions;
    private String latest_version;
    private String release_note;
    private String download_url;

    public String getLatest_version() {
        return latest_version;
    }

    public void setLatest_version(String latest_version) {
        this.latest_version = latest_version;
    }

    public List<String> getOptional_versions() {
        return optional_versions;
    }

    public void setOptional_versions(List<String> optional_versions) {
        this.optional_versions = optional_versions;
    }

    public List<String> getRequired_versions() {
        return required_versions;
    }

    public void setRequired_versions(List<String> required_versions) {
        this.required_versions = required_versions;
    }

    public String getRelease_note() {
        return release_note;
    }

    public void setRelease_note(String release_note) {
        this.release_note = release_note;
    }

    public String getDownload_url() {
        return download_url;
    }

    public void setDownload_url(String download_url) {
        this.download_url = download_url;
    }
}
