package com.smartbi.engine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "engine")
public class EngineProperties {

    private final Redis redis = new Redis();
    private final Consumer consumer = new Consumer();

    public Redis getRedis() {
        return redis;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public static class Redis {
        private String traceListKey = "kylin_audit_trace";
        private int brpopTimeoutSeconds = 5;
        /** RPOP polling interval when not using dedicated BRPOP connection */
        private long pollIntervalMs = 500;

        public String getTraceListKey() {
            return traceListKey;
        }

        public void setTraceListKey(String traceListKey) {
            this.traceListKey = traceListKey;
        }

        public int getBrpopTimeoutSeconds() {
            return brpopTimeoutSeconds;
        }

        public void setBrpopTimeoutSeconds(int brpopTimeoutSeconds) {
            this.brpopTimeoutSeconds = brpopTimeoutSeconds;
        }

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }
    }

    public static class Consumer {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
