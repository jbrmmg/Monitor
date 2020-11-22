package com.jbr.middletier.monitor.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PortStatus {
    final static private Logger LOG = LoggerFactory.getLogger(PortStatus.class);

    public enum StatusType { UNKNOWN, RED, AMBER, GREEN }

    private static final String TIME_FORMAT = "dd MMM yyyy HH:mm:ss";

    private final int id;
    private final StatusType status;
    private String name;
    private String description;
    private String version;
    private String serviceStatus;
    private String dbStatus;
    private Date lastUpdateTime;
    private Date lastGreenTime;

    public PortStatus(int id, StatusType status) {
        LOG.info("Port Status" + id + " - " + status);
        this.id = id;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public StatusType getStatus() {
        return this.status;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) { this.name = name; }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) { this.description = description; }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) { this.version = version; }

    public String getDbStatus() {
        return dbStatus;
    }

    public void setDbStatus(String dbStatus) { this.dbStatus = dbStatus; }

    public String getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(String serviceStatus) { this.serviceStatus = serviceStatus; }

    public String getLastUpdateTime() {
        if(this.lastUpdateTime == null) {
            return "";
        }

        DateFormat sdf = new SimpleDateFormat(TIME_FORMAT);

        return sdf.format(this.lastUpdateTime);
    }

    public void setLastUpdateTime(Date lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    public String getLastGreenTime() {
        if(this.lastGreenTime == null) {
            return "";
        }

        DateFormat sdf = new SimpleDateFormat(TIME_FORMAT);

        return sdf.format(this.lastGreenTime);
    }

    public void setLastGreenTime(Date lastGreenTime) { this.lastGreenTime = lastGreenTime; }
}
