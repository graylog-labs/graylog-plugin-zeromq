package org.graylog.plugin.zeromq.inputs;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.plugin.zeromq.transports.ZeroMQTransport;
import org.graylog2.inputs.codecs.SyslogCodec;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

import javax.inject.Inject;

public class SyslogZeroMQInput extends MessageInput {

    private static final String NAME = "Syslog ZeroMQ PULL device";

    @AssistedInject
    public SyslogZeroMQInput(MetricRegistry metricRegistry,
                             @Assisted Configuration configuration,
                             ZeroMQTransport.Factory transportFactory,
                             SyslogCodec.Factory codecFactory,
                             LocalMetricRegistry localRegistry,
                             Config config,
                             Descriptor descriptor, ServerStatus serverStatus) {
        super(metricRegistry, configuration, transportFactory.create(configuration), localRegistry, codecFactory.create(configuration),
              config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<SyslogZeroMQInput> {
        @Override
        SyslogZeroMQInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(ZeroMQTransport.Factory transport, SyslogCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
