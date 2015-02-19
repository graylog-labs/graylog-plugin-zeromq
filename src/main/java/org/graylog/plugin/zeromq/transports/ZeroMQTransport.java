package org.graylog.plugin.zeromq.transports;

import com.codahale.metrics.MetricSet;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class ZeroMQTransport implements Transport {
    private static final Logger log = LoggerFactory.getLogger(ZeroMQTransport.class);

    public static final String ZMQ_IOTHREADS = "zmq_iothreads";
    public static final String ZMQ_ADDRESS = "zqm_address";
    public static final String ZMQ_SHOULD_BIND = "zmq_bind";
    private final Configuration configuration;
    private final LocalMetricRegistry localRegistry;
    private ZMQ.Context context;
    private ZMQ.Socket socket;
    private AbstractExecutionThreadService pullDeviceService;

    @AssistedInject
    public ZeroMQTransport(@Assisted Configuration configuration, LocalMetricRegistry localRegistry) {
        this.configuration = configuration;
        this.localRegistry = localRegistry;
    }

    @Override
    public void setMessageAggregator(CodecAggregator ignored) {
    }

    @Override
    public void launch(final MessageInput input) throws MisfireException {
        final int ioThreads = configuration.getInt(ZMQ_IOTHREADS);
        final String address = configuration.getString(ZMQ_ADDRESS);
        final boolean shouldBind = configuration.getBoolean(ZMQ_SHOULD_BIND);

        log.debug("{} ZeroMQ PULL device to {} using {} IO threads", shouldBind ? "Binding": "Connecting", address, ioThreads);

        context = ZMQ.context(Math.max(1, ioThreads));
        socket = context.socket(ZMQ.PULL);
        socket.setReceiveTimeOut(500); // wake up at least every 500ms
        try {
            if (shouldBind) {
                socket.bind(address);
            } else {
                socket.connect(address);
            }
        } catch (ZMQException e) {
            context.close();
            throw new MisfireException("Could not " + (shouldBind ? "bind" : "connect") + " PULL device to " + address, e);
        }

        pullDeviceService = new AbstractExecutionThreadService() {
            @Override
            protected void run() throws Exception {
                while (isRunning()) {
                    // this wakes up periodically if there's no traffic to check for shutdown
                    final byte[] bytes = socket.recv();
                    if (bytes != null) {
                        input.processRawMessage(new RawMessage(bytes));
                    }
                }
            }
        };
        pullDeviceService.startAsync().awaitRunning();
    }

    @Override
    public void stop() {
        final String address = configuration.getString(ZMQ_ADDRESS);
        log.debug("Stopping ZeroMQ reader thread connected to {}", address);
        pullDeviceService.stopAsync().awaitTerminated();

        log.debug("Closing ZeroMQ socket and context connected to {}", address);
        socket.close();
        context.close();
    }

    @Override
    public MetricSet getMetricSet() {
        return localRegistry;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<ZeroMQTransport> {
        ZeroMQTransport create(Configuration configuration);
    }

    @ConfigClass
    public static class Config implements Transport.Config {

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest cr = new ConfigurationRequest();
            cr.addField(new TextField(
                    ZMQ_ADDRESS,
                    "Device address",
                    "",
                    "Address to connect or bind to, e.g. tcp://127.0.0.1:5555"
            ));
            cr.addField(new BooleanField(
                    ZMQ_SHOULD_BIND,
                    "Bind to address",
                    true,
                    "The PULL device can either listen to incoming connections or connect by itself. Check this " +
                            "option to bind and wait for incoming connections, uncheck for actively connecting " +
                            "to the given address."
            ));
            cr.addField(new NumberField(
                    ZMQ_IOTHREADS,
                    "IO Threads",
                    1,
                    "The number of IO threads to use, 1 usually is sufficient."
            ));

            return cr;
        }
    }
}
