package org.graylog.plugin.zeromq.inputs;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.plugin.zeromq.transports.ZeroMQTransport;
import org.graylog2.inputs.codecs.RawCodec;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

import javax.inject.Inject;

public class RawZeroMQInput extends MessageInput {

    private static final String NAME = "Raw/Plaintext ZeroMQ PULL device";

    @AssistedInject
    public RawZeroMQInput(MetricRegistry metricRegistry,
                          @Assisted Configuration configuration,
                          ZeroMQTransport.Factory transportFactory,
                          RawCodec.Factory codecFactory,
                          LocalMetricRegistry localRegistry,
                          Config config,
                          Descriptor descriptor, ServerStatus serverStatus) {
        super(metricRegistry, configuration, transportFactory.create(configuration), localRegistry, codecFactory.create(configuration),
              config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<RawZeroMQInput> {
        @Override
        RawZeroMQInput create(Configuration configuration);

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
        public Config(ZeroMQTransport.Factory transport, RawCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
