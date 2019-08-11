package com.jbr.middletier.monitor.manager;

import org.json.JSONObject;
import org.json.JSONPropertyName;

import java.util.Date;

/**
 * Created by jason on 01/05/17.
 */
class HealthUrl extends Url {
    private static final String STATUS = "status";
    private static final String STATUS_DB = "db";
    private static final String STATUS_SERVICE = "service";
    private static final String STATUS_DETAILS = "details";
    private static final int HEALTH_REFRESH_MS = 10 * 60 * 1000;
    private static final int HEALTH_REFRESH_FAIL_MS = 20 * 1000;

    private String dbHealth;
    private String serviceHealth;
    private String serviceName;
    private Date updateTime;

    public HealthUrl(String baseUrl) {
        super("http://" + baseUrl + "/actuator/health", HEALTH_REFRESH_MS, HEALTH_REFRESH_FAIL_MS);

        dbHealth = "?";
        serviceHealth = "?";
        serviceName = "";
        updateTime = null;
    }

    @Override
    public void processResonse(String response) {
        this.problem = false;

        boolean statusUp = false;
        JSONObject obj = new JSONObject(response);
        if(!obj.isNull(STATUS)) {
            if(!obj.getString(STATUS).equals("UP")) {
                this.problem = true;
            }
        }

        if(!obj.isNull(STATUS_DETAILS)) {
            JSONObject details = obj.getJSONObject(STATUS_DETAILS);

            if(!details.isNull(STATUS_DB)){
                if(!details.getJSONObject(STATUS_DB).isNull(STATUS)) {
                    this.dbHealth = details.getJSONObject(STATUS_DB).getString(STATUS);

                    // If this is 'DOWN' then there is a problem
                    if(this.dbHealth.equals("DOWN")) {
                        problem = true;
                    }
                }
            }

            if(!details.isNull(STATUS_SERVICE)){
                if(!details.getJSONObject(STATUS_SERVICE).isNull(STATUS)) {
                    this.serviceHealth = details.getJSONObject(STATUS_SERVICE).getString(STATUS);

                    // If this is 'DOWN' then there is a problem
                    if(this.serviceHealth.equals("DOWN")) {
                        problem = true;
                    }
                }
            }

            if(!details.isNull(STATUS_SERVICE)){
                JSONObject serviceDetails = details.getJSONObject(STATUS_SERVICE).getJSONObject(STATUS_DETAILS);

                if(serviceDetails.getString(STATUS_SERVICE).length() > 0) {
                    this.serviceName = serviceDetails.getString(STATUS_SERVICE);
                }
            }

            updateTime = new Date();
        } else {
            urlFailed();
            updateTime = null;
        }
    }

    @Override
    public void urlFailed() {
        dbHealth = "?";
        serviceHealth = "?";
    }

    @Override
    public void updateStatus(PortStatus status)
    {
        status.setDbStatus(this.dbHealth);
        status.setServiceStatus(this.serviceHealth);
        status.setLastUpdateTime(updateTime);
    }

    @Override
    public void command(boolean internal, String text) {
        // If the command text is RefreahHealth the set the update time to now.
        if(text.equals("RefreahHealth")) {
            setUpdateTime(UpdateTimeOffsetType.NONE);
        }
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }
}
