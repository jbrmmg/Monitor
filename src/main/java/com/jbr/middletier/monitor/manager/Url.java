package com.jbr.middletier.monitor.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jason on 01/05/17.
 */
public abstract class Url {
    final static private Logger LOG = LoggerFactory.getLogger(Url.class);

    protected enum UpdateTimeOffsetType { NONE, OK, FAIL}

    private final String urlAddress;
    private volatile UrlStateType status;
    protected volatile boolean problem;
    private volatile Date nextRequestTime;
    private final Object lockObject = new Object();
    private final int requestFrequencyOkMS;
    private final int requestFrequencyFailMS;

    protected void setUpdateTime(UpdateTimeOffsetType useOffset) {
        Calendar calendar = Calendar.getInstance();

        switch(useOffset) {
            case OK:
                calendar.add(Calendar.MILLISECOND,requestFrequencyOkMS);
                break;
            case FAIL:
                calendar.add(Calendar.MILLISECOND,requestFrequencyFailMS);
                break;
        }

        synchronized (lockObject) {
            this.nextRequestTime = calendar.getTime();
        }

        // Log the time.
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        LOG.info("URL: " + urlAddress + " update after " + sdf.format(nextRequestTime));
    }

    public enum UrlStateType { UNKNOWN, WAITING, OK, PROBLEM, FAIL }

    Url(String urlAddress, int requestFrequencyOkMS, int requestFrequencyFailMS) {
        this.urlAddress = urlAddress;
        this.status = UrlStateType.UNKNOWN;
        this.requestFrequencyOkMS = requestFrequencyOkMS;
        this.requestFrequencyFailMS = requestFrequencyFailMS;
        this.nextRequestTime = new Date();
        this.problem = false;
        setUpdateTime(UpdateTimeOffsetType.NONE);
    }

    public UrlStateType getStatus() {
        if(status == UrlStateType.OK && problem)
            return UrlStateType.PROBLEM;

        return this.status;
    }

    public String getUrlAddress() {
        return this.urlAddress;
    }

    public abstract void processResonse(String response);

    public abstract void urlFailed();

    public abstract void updateStatus(PortStatus status);

    public abstract void command(boolean internal, String text);

    public abstract String getServiceName();

    public void response(int code, String response){
        // Set the status of this URL.
        if(code == 0) {
            this.status = UrlStateType.FAIL;
            urlFailed();
            setUpdateTime(UpdateTimeOffsetType.FAIL);
        } else {
            this.status = UrlStateType.OK;
            processResonse(response);
            setUpdateTime(UpdateTimeOffsetType.OK);
        }
    }

    public boolean requiresRefresh() {
        // Does this require a refresh?
        synchronized (lockObject) {
            // If the status is waiting, then there is no need to request again.
            if(status == UrlStateType.WAITING) {
                return false;
            }
        }

        synchronized (lockObject) {
            Date date = new Date();

            // If there time is in the future, then no need to request.
            if(nextRequestTime.after(date)) {
                return false;
            }
        }

        return true;
    }
}
