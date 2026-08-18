package com.firstclub.membership.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "membership")
public record MembershipProperties(boolean seedDemoData, Scheduler scheduler) {

    public record Scheduler(String lifecycleCron, String tierEvaluationCron, int batchSize) {

        public int batchSizeOrDefault() {
            return batchSize > 0 ? batchSize : 100;
        }
    }
}
