package com.jbr.middletier.monitor.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.jbr.middletier.monitor.manager.config.MonitoredItemType;

@Component
public class PortFactory {
    final static private Logger LOG = LoggerFactory.getLogger(PortStatus.class);

    private int nextId;

    public PortFactory() {
        nextId = 0;
    }

    private int getNextId() {
        return nextId++;
    }

    public Port createHealthAndInfoMonitor(MonitoredItemType itemConfig, boolean primary) {
        String baseUrl = itemConfig.getHost() + ":" + itemConfig.getPort();
        String name = itemConfig.getName();

        // Primary can only be true if the host name is 'localhost'
        if(primary) {
            if(!itemConfig.getHost().equalsIgnoreCase("localhost")) {
                primary = false;
            }
        }

        // Create a new Port object, with an Info and a Health URL.
        Port newPort = new Port(getNextId(),baseUrl,primary);

        HealthUrl healthUrl = new HealthUrl(baseUrl);
        newPort.addUrl(healthUrl);

        InfoUrl infoUrl = new InfoUrl(baseUrl,name);
        newPort.addUrl(infoUrl);

        // Create a new monitor
        return newPort;
    }
}
