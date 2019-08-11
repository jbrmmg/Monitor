package com.jbr.middletier.monitor.control;

import com.jbr.middletier.monitor.manager.PortManager;
import com.jbr.middletier.monitor.manager.Port;
import com.jbr.middletier.monitor.manager.PortStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jason on 21/04/17.
 */
@Controller
@RequestMapping("/jbr")
public class MonitorController {
    final static private Logger LOG = LoggerFactory.getLogger(MonitorController.class);

    private final PortManager hostManager;
    private final int longRefresh;
    private final int shortRefresh;
    private int shortRefreshCount;

    private class Command {

        private boolean ok;
        private int id;
        private String commandText;

        public Command(String requestBody) {
            try {
                ok = false;
                id = -1;
                commandText = "";

                String[] parameters = requestBody.split("&");
                for(String nextParameter : parameters) {
                    String[] nameValuePair = nextParameter.split("=");

                    if(nameValuePair.length == 2) {
                        if(nameValuePair[0].equalsIgnoreCase("id")) {
                            id = Integer.parseInt(nameValuePair[1]);
                        }
                        if(nameValuePair[0].equalsIgnoreCase("action")) {
                            commandText = nameValuePair[1];
                        }
                    }
                }

                ok = true;
            } catch (Exception ex) {
            }
        }

        public boolean OK() {
            return this.ok;
        }

        public String getText() {
            return this.commandText;
        }

        public int getId() {
            return this.id;
        }
    }

    @Autowired
    public MonitorController(PortManager hostManager) {
        this.hostManager = hostManager;
        this.shortRefreshCount = 0;
        this.shortRefresh = 25;
        this.longRefresh = 120;
    }

    @ModelAttribute("allMonitorStatus")
    public List<PortStatus> populateSeedStarters() {
        return hostManager.getStatus();
    }

    @ModelAttribute("refreshRate")
    public int refreshRate() {
        if(shortRefreshCount == 0) {
            return longRefresh;
        }

        shortRefreshCount--;
        return shortRefresh;
    }

    @GetMapping(path="/int/monitor")
    public String greetingInternal(Model model) {
        return "monitorstatus";
    }

    @GetMapping(path="/ext/monitor")
    public String greetingExternal(Model model) {
        return "monitorstatusext";
    }

    @RequestMapping(value="/int/monitoraction")
    public String monitorAction(
            @RequestBody String commandString, final BindingResult bindingResult, final ModelMap model) {
        if (bindingResult.hasErrors()) {
            return "monitorstatus";
        }
        LOG.info("-------------------------------------");
        LOG.info("post monitor (2)");
        LOG.info("Command - " + commandString);

        this.shortRefreshCount++;
        Command command = new Command(commandString);

        if(command.OK()) {
            LOG.info("ID     - " + command.getId());
            LOG.info("Text   - " + command.getText());

            hostManager.urlCommand(true, command.getId(), command.getText());
        }

        model.clear();
        return "redirect:/jbr/int/monitor";
    }

    @RequestMapping(value="/ext/monitoraction", params={"id","action"})
    public String monitorActionExternal(
            @RequestBody String commandString, final BindingResult bindingResult, final ModelMap model) {
        if (bindingResult.hasErrors()) {
            return "monitorstatus";
        }
        LOG.info("-------------------------------------");
        LOG.info("post monitor (2)");
        LOG.info("service name - " + commandString);

        this.shortRefreshCount++;
        Command command = new Command(commandString);

        if(command.OK()) {
            LOG.info("ID     - " + command.getId());
            LOG.info("Text   - " + command.getText());

            hostManager.urlCommand(false, command.getId(), command.getText());
        }

        model.clear();
        return "redirect:/jbr/ext/monitor";
    }
}
