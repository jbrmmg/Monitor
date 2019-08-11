package com.jbr.middletier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by jason on 21/04/17.
 */
@SpringBootApplication
@EnableScheduling
public class MiddleTier {
    public static void main(String[] args) {
        SpringApplication.run(MonitorConfig.class, args);
    }
}
