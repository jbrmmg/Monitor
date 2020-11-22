package com.jbr.middletier.monitor.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/*
 * Class used to perform background tasks.
 */

@Component
public class Processor implements DisposableBean, Runnable {
    final static private Logger LOG = LoggerFactory.getLogger(Processor.class);

    private final PortManager portManager;
    private final WebLogManager webLogManager;
    private volatile boolean destroyed;
    private final String restartCommandFormat;

    @Value("${middle.tier.monitor.interval:20}")
    private long waitInterval;

    @Autowired
    public Processor(PortManager portManager, WebLogManager webLogManager, @Value("${middle.tier.monitor.restartCmdFormat:sudo systemctl restart %s}") String restartCommand) {
        this.portManager = portManager;
        this.webLogManager = webLogManager;
        this.destroyed = false;
        this.restartCommandFormat = restartCommand;

        // Start the thread.
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            while (!destroyed) {
                DoProcessing();

                // Sleep for time.
                //noinspection BusyWait
                Thread.sleep(waitInterval * 1000);
            }
        } catch (Exception ex) {
            LOG.error("Processor thread failed." + ex.getMessage());
        }
    }

    static private String convertInputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder reply = new StringBuilder();

        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        String nextPart = in.readLine();
        while (nextPart != null) {
            reply.append(nextPart);
            nextPart = in.readLine();
        }
        in.close();

        return reply.toString();
    }

    private void ProcessUrl(Url nextUrl) {
        try {
            LOG.info("Check: " + nextUrl.getUrlAddress());
            URL url = new URL(nextUrl.getUrlAddress());

            // Loop through the hosts.
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            String reply = "";
            int responseCode = 0;
            try {
                reply = convertInputStreamToString(connection.getInputStream());
                responseCode = 200;
            } catch (IOException ioe) {
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConn = (HttpURLConnection) connection;
                    responseCode = httpConn.getResponseCode();
                    if (responseCode != 200) {
                        reply = convertInputStreamToString(httpConn.getErrorStream());
                    }
                }
            }

            nextUrl.response(responseCode,reply);
        } catch(Exception e) {
            LOG.error("******************************************************************");
            LOG.error("Failed to read url " + nextUrl.getUrlAddress() + " " + e.getMessage());
        }
    }

    private void RestartPort(Port nextPort) {
        try {
            LOG.info("Restart service - " + nextPort.getServiceName());
            webLogManager.postWebLog(WebLogManager.webLogLevel.INFO,"Restart port - " + nextPort.getServiceName());
            String restartCommand = String.format(restartCommandFormat, nextPort.getServiceName());
            LOG.info(String.format("Command: %s", restartCommand));

            String[] cmd = new String[]{"bash", "-c", restartCommand};
            final Process restartProcess = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(1000L * 60L * 10L);
                    LOG.warn("Restart, taken too long.");
                    restartProcess.destroy();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });

            int result = restartProcess.waitFor();
            if(result == 0) {
                LOG.info("Command completed, result was zero.");
            } else {
                LOG.warn(String.format("Command completed, non zero result - %d", result));
            }

            Thread.sleep(3000);

            t.interrupt();
        } catch (Exception ex) {
            webLogManager.postWebLog(WebLogManager.webLogLevel.ERROR,"Failed to restart port " + nextPort.getServiceName());
            LOG.error("Start service failed.", ex);
        }

        nextPort.setNextRestartTime();
    }

    private void DoProcessing() {
        try {
            LOG.info("Running Processor Thread");

            // Get a list of URLs that need to be refreshed.
            List<Url> refreshUrl = portManager.getUrlsToRefresh();

            // Update the URLs
            for(Url nextUrl : refreshUrl) {
                ProcessUrl(nextUrl);
            }

            // Check for service restarts.
            List<Port> restartPorts = portManager.getPortsToRestart();

            for(Port nextPort : restartPorts) {
                RestartPort(nextPort);
            }

        } catch (Exception ex) {
            LOG.error("Processing failed." + ex.getMessage());
        }
    }

    @Override
    public void destroy() {
        destroyed = true;
    }
}
