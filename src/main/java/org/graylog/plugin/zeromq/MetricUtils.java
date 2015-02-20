package org.graylog.plugin.zeromq;

import com.codahale.metrics.Gauge;

public class MetricUtils {

    public static Gauge<Long> constantGauge(final long constant) {
        return new Gauge<Long>() {
            @Override
            public Long getValue() {
                return constant;
            }
        };
    }

}
