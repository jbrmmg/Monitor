package com.jbr.middletier.monitor.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by jason on 23/09/18.
 */

@Component
public class ServiceHealthIndicator implements HealthIndicator {
    final static private Logger LOG = LoggerFactory.getLogger(ServiceHealthIndicator.class);

    @Value("${middle.tier.service.name}")
    private String serviceName;

    @Override
    public Health health() {
        try {
            LOG.info(String.format("Check Database"));

            return Health.up().withDetail("service", serviceName).withDetail("Backup Types",1).build();
        } catch (Exception e) {
            LOG.error("Failed to check health",e);
        }

        return Health.down().withDetail("service", serviceName).build();
    }
}
