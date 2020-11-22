package com.jbr.middletier.monitor.manager;

import org.json.JSONObject;

/**
 * Created by jason on 01/05/17.
 */
class InfoUrl extends Url {
    private static final String APP = "app";
    private static final String APP_NAME = "name";
    private static final String APP_DESCRIPTION = "description";
    private static final String APP_VERSION = "version";
    private static final int INFO_REFRESH_MS = 60 * 60 * 1000;
    private static final int INFO_FAIL_REFRESH_MS = 60 * 1000;


    private String name;
    private String description;
    private String version;
    private final boolean constantName;

    public InfoUrl(String baseUrl, String name) {
        super("http://" + baseUrl + "/actuator/info", INFO_REFRESH_MS, INFO_FAIL_REFRESH_MS);

        this.constantName = name.length() > 0;

        this.name = ( constantName ) ? name : "?";
        this.description = "?";
        this.version = "?";
    }

    @Override
    public void processResonse(String response) {
        if(response != null && response.length() > 0) {
            JSONObject obj = new JSONObject(response);
            if (!obj.isNull(APP)) {

                // Get the name.
                if (!obj.getJSONObject(APP).isNull(APP_NAME)) {
                    if(!this.constantName) {
                        this.name = obj.getJSONObject(APP).getString(APP_NAME);
                    }
                }

                // Get the description.
                if (!obj.getJSONObject(APP).isNull(APP_DESCRIPTION)) {
                    this.description = obj.getJSONObject(APP).getString(APP_DESCRIPTION);
                }

                // Get the version.
                if (!obj.getJSONObject(APP).isNull(APP_VERSION)) {
                    this.version = obj.getJSONObject(APP).getString(APP_VERSION);
                }
            }
        }
    }

    @Override
    public void urlFailed() {
    }

    @Override
    public void updateStatus(PortStatus status)
    {
        status.setName(this.name);
        status.setDescription(this.description);
        status.setVersion(this.version);
    }

    @Override
    public void command(boolean internal, String text) {
        // If the command text is RefreahInfo the set the update time to now.
        if(text.equals("RefreshInfo")) {
            setUpdateTime(UpdateTimeOffsetType.NONE);
        }
    }

    @Override
    public String getServiceName() {
        return "";
    }
}
