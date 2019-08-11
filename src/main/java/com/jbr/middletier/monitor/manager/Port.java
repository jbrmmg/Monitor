package com.jbr.middletier.monitor.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jason on 25/04/17.
 */
public class Port {
    final static private Logger LOG = LoggerFactory.getLogger(Port.class);

    private final int id;
    private final List<Url> urlMonitors;
    private final boolean primary;
    private Date greenTime;
    private Date serviceRestart;
    private String serviceName;

    public Port(int id, String host, boolean primary) {
        LOG.info(host + ": Created");

        this.id = id;
        this.primary = primary;
        this.urlMonitors = new ArrayList<>();
        this.greenTime = null;
        this.serviceRestart = null;
        this.serviceName = "";

        // Set the service restart time.
        setNextRestartTime();
    }

    public void addUrl(Url newUrl) {
        this.urlMonitors.add(newUrl);
    }

    // Return status.
    public PortStatus getStatus() {
        PortStatus result =  new PortStatus(id, getPortStatus());

        for(Url nextUrl : urlMonitors) {
            nextUrl.updateStatus(result);
        }

        // Set the green status time.
        if(this.greenTime != null) {
            result.setLastGreenTime(this.greenTime);
        }

        return result;
    }

    public int getId(){
        return id;
    }

    private PortStatus.StatusType getPortStatus() {
        // Overall status; each URL can be Unknown, Waiting, OK or Failed.
        //
        // If any are Unknown or Waiting then the port is unknown
        //
        // If all are failed then the status is Red
        // If all are OK then status is Green
        // If some are failed then status is Amber.

        boolean greenPossible = true;
        boolean redPossible = true;

        for(Url nextUrl : urlMonitors) {
            // IF this Url is unknown or waiting then the whole thing is unknown.
            if( (nextUrl.getStatus() == Url.UrlStateType.UNKNOWN) || (nextUrl.getStatus() == Url.UrlStateType.WAITING) ) {
                return PortStatus.StatusType.UNKNOWN;
            }

            // If Failed, then green is no longer possible.
            if(nextUrl.getStatus() == Url.UrlStateType.FAIL) {
                greenPossible = false;
            }

            // If OK, then red is no longer possible.
            if(nextUrl.getStatus() == Url.UrlStateType.OK) {
                redPossible = false;
            }

            // If there is a problem, then it means green is no longer possible.
            if(nextUrl.getStatus() == Url.UrlStateType.PROBLEM) {
                greenPossible = false;
            }
        }

        // If red is not possible, and green is then the result must be green.
        if((redPossible == false) && (greenPossible == true)) {
            if(this.greenTime == null) {
                this.greenTime = new Date();
            }
            return PortStatus.StatusType.GREEN;
        }

        // If green is not possible and red is then the result must be red.
        if((redPossible == true) && (greenPossible == false)) {
            this.greenTime = null;
            return PortStatus.StatusType.RED;
        }

        // Otherwise it must be amber.
        this.greenTime = null;
        return PortStatus.StatusType.AMBER;
    }

    public List<Url> getUrlsToRefresh() {
        // Get the status (this updates the status time).
        getStatus();

        // Get a list of Urls that need updating.
        List<Url> result = new ArrayList();

        for(Url nextUrl : urlMonitors) {
            if(nextUrl.requiresRefresh()) {
                result.add(nextUrl);
            }
        }

        return result;
    }

    public void urlCommand(boolean internal, String text) {
        // If the text is Restart then set the service restart time.
        if(text.equalsIgnoreCase("Restart")) {
            if(this.primary) {
                if(hasServiceName()) {
                    serviceRestart = new Date();
                }
            }
            return;
        }

        // Pass the command to each URL
        for(Url nextUrl : urlMonitors) {
            nextUrl.command(internal, text);
        }
    }

    private boolean hasServiceName() {
        // If the service name is known, return true.
        if(serviceName.length() > 0) {
            return true;
        }

        // Service name comes to one of the urls.
        for(Url nextUrl : urlMonitors) {
            serviceName = nextUrl.getServiceName();

            if (serviceName.length() > 0) {
                return true;
            }
        }

        return false;
    }

    public String getServiceName() {
        if(this.primary) {
            if(serviceName.length() <= 0) {
                if (!hasServiceName()) {
                    return "";
                }

                return serviceName;
            }

            return serviceName;
        }

        return "";
    }

    public boolean restartRequired() {
        if(this.primary) {
            if(serviceRestart != null) {
                // Get the service name (if not done already)
                if(serviceName.length() <= 0) {
                    if( !hasServiceName() ) {
                        return false;
                    }
                }

                Date date = new Date();

                if(date.after(serviceRestart)) {
                    LOG.info("Restart required for " + serviceName);
                    return true;
                }
            }
        }

        return false;
    }

    public void setNextRestartTime() {
        if(this.primary) {
            Calendar calendar = Calendar.getInstance();

            calendar.add(Calendar.DATE, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 15);

            serviceRestart = calendar.getTime();

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
            LOG.info("Service restart time: " + sdf.format(serviceRestart));

            // Set all URL's that they need to refresh.
            for(Url nextUrl : urlMonitors) {
                nextUrl.setUpdateTime(Url.UpdateTimeOffsetType.NONE);
            }
        }
    }
}
