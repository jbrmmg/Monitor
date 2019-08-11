package com.jbr.middletier.monitor.manager;

import com.jbr.middletier.monitor.manager.config.MachineType;
import com.jbr.middletier.monitor.manager.config.MachinesType;
import com.jbr.middletier.monitor.manager.config.MonitorType;
import com.jbr.middletier.monitor.manager.config.MonitoredItemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.net.NetworkInterface;
import java.util.*;

/**
 * Created by jason on 21/04/17.
 */

@Component
public class PortManager {
    final static private Logger LOG = LoggerFactory.getLogger(PortManager.class);

    private final Map<Integer,Port> monitorData;
    private final PortFactory portFactory;

    private static String formatMacAddress(byte[] mac) {
        if(mac == null)
            return "";

        StringBuilder sb = new StringBuilder(18);
        for (byte b : mac) {
            if (sb.length() > 0)
                sb.append(':');
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Autowired
    public PortManager(@Value("${server.port}") String port, PortFactory portFactory) {
        this.portFactory = portFactory;
        this.monitorData = new HashMap<>();

        // New, determine the hosts to monitor using XML configuration
        LOG.info("Starting up with new configuration");
        LOG.info("Port - " + port);

        try {
            // Get the configuration XML file
            LOG.info("Loading configuration file.");
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream("MonitorConfig.xml");

            // Load up the configuration.
            JAXBContext jaxbContext = JAXBContext.newInstance("com.jbr.middletier.monitor.manager.config");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            @SuppressWarnings("unchecked")
            JAXBElement<MachinesType> machinesElement = (JAXBElement<MachinesType>)unmarshaller.unmarshal(is);
            MachinesType machines = machinesElement.getValue();
            LOG.info("Loaded config " + machines.getMachine().size() + " machines found.");

            boolean foundConfig = false;

            // Get the network cards on this machine
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface nextInterface : Collections.list(interfaces)) {
                LOG.info("Name - " + nextInterface.getDisplayName());

                String nextMacAddress = formatMacAddress(nextInterface.getHardwareAddress());
                LOG.info("Addr - " + nextMacAddress);

                // Is there a definition for this machine?
                for(MachineType nextMachine : machines.getMachine()) {
                    if(nextMachine.getId().equalsIgnoreCase(nextMacAddress)) {
                        LOG.info("Found definition for " + nextMachine.getId());

                        // Is there configuration for the monitor with my port?
                        for(MonitorType nextMonitor : nextMachine.getMonitors().getMonitor()) {
                            if(nextMonitor.getPort().equalsIgnoreCase(port)) {
                                LOG.info("Found definition for " + nextMonitor.getPort());

                                // Setup monitors.
                                for(MonitoredItemType nextItem : nextMonitor.getMonitoredItems().getMonitoredItem()) {
                                    Port nextResult = portFactory.createHealthAndInfoMonitor(nextItem,nextMonitor.getPrimary().equalsIgnoreCase("Yes") ? true : false);
                                    monitorData.put(nextResult.getId(),nextResult);
                                }

                                foundConfig = true;

                                // Completed
                                break;
                            }
                        }

                        // No need to look at other machines.

                        // If configuration is complete, exit loop.
                        if(foundConfig) {
                            break;
                        }
                    }
                }

                // If configuration is complete, exit loop.
                if(foundConfig) {
                    break;
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to startup host manager " + ex.getMessage());
        }
    }

    public List<PortStatus> getStatus() {
        List<PortStatus> result = new ArrayList();

        for(Port nextResult : monitorData.values()) {
            result.add(nextResult.getStatus());
        }

        return result;
    }

    public List<Url> getUrlsToRefresh() {
        List<Url> result = new ArrayList();

        for(Port nextResult : monitorData.values()) {
            result.addAll(nextResult.getUrlsToRefresh());
        }

        return result;
    }

    public List<Port> getPortsToRestart() {
        List<Port> result = new ArrayList();

        for(Port nextResult : monitorData.values()) {
            if(nextResult.restartRequired()) {
                result.add(nextResult);
            }
        }

        return result;
    }

    public void urlCommand(boolean internal, int id, String text) {
        // Pass the command to the port with the id specified.
        if(monitorData.containsKey(id)) {
            monitorData.get(id).urlCommand(internal,text);
        }
    }
}
