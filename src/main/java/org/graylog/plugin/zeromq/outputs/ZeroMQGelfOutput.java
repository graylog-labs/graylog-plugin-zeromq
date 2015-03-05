package org.graylog.plugin.zeromq.outputs;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZeroMQGelfOutput implements MessageOutput {
    private static final Logger log = LoggerFactory.getLogger(ZeroMQGelfOutput.class);

    public static final String ZMQ_IOTHREADS = "zmq_iothreads";
    public static final String ZMQ_ADDRESS = "zqm_address";
    public static final String ZMQ_SHOULD_BIND = "zmq_bind";

    private final ZMQ.Context context;
    private final ZMQ.Socket socket;
    private final Configuration configuration;
    private final JsonFactory jsonFactory;
    private final AtomicBoolean isRunning = new AtomicBoolean();

    @AssistedInject
    public ZeroMQGelfOutput(@Assisted Stream stream, @Assisted Configuration configuration) throws MessageOutputConfigurationException {
        this.configuration = configuration;
        final int ioThreads = configuration.getInt(ZMQ_IOTHREADS);
        final String address = configuration.getString(ZMQ_ADDRESS);
        final boolean shouldBind = configuration.getBoolean(ZMQ_SHOULD_BIND);

        log.debug("{} ZeroMQ PUSH device to {} using {} IO threads",
                  shouldBind ? "Binding" : "Connecting",
                  address,
                  ioThreads);

        context = ZMQ.context(Math.max(1, ioThreads));
        socket = context.socket(ZMQ.PUSH);
        socket.setLinger(1000); // wait up to 1s for outgoing messages to be processed when closing socket
        socket.setSendTimeOut(500); // block up to 500ms when trying to send

        try {
            if (shouldBind) {
                socket.bind(address);
            } else {
                socket.connect(address);
            }
        } catch (ZMQException e) {
            context.close();
            log.error("Unable to " + (shouldBind ? "bind" : "connect") + " PUSH device: " + e.getMessage());
            throw new MessageOutputConfigurationException("Could not " + (shouldBind ? "bind" : "connect") + " PUSH device to " + address);
        }
        jsonFactory = new JsonFactory();
        isRunning.set(true);
    }

    @Override
    public void stop() {
        isRunning.set(false);
        final String address = configuration.getString(ZMQ_ADDRESS);
        log.info("Closing ZeroMQ socket and context connected to {}", address);
        socket.close();
        context.close();
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        final byte[] gelfJson = toJson(toGELFMessage(message));
        socket.send(gelfJson);
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message message : messages) {
            write(message);
        }
    }

    // blatantly copied/adapted from GelfOutput and GelfClient
    private byte[] toJson(final GelfMessage message) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (final JsonGenerator jg = jsonFactory.createGenerator(out, JsonEncoding.UTF8)) {
            jg.writeStartObject();

            jg.writeStringField("version", message.getVersion().toString());
            jg.writeNumberField("timestamp", message.getTimestamp());
            jg.writeStringField("host", message.getHost());
            jg.writeStringField("short_message", message.getMessage());
            jg.writeNumberField("level", message.getLevel().getNumericLevel());

            if(null != message.getFullMessage()) {
                jg.writeStringField("full_message", message.getFullMessage());
            }

            for (Map.Entry<String, Object> field : message.getAdditionalFields().entrySet()) {
                final String realKey = field.getKey().startsWith("_") ? field.getKey() : ("_" + field.getKey());

                if (field.getValue() instanceof Number) {
                    // Let Jackson figure out how to write Number values.
                    jg.writeObjectField(realKey, field.getValue());
                } else if (field.getValue() == null) {
                    jg.writeNullField(realKey);
                } else {
                    jg.writeStringField(realKey, field.getValue().toString());
                }
            }

            jg.writeEndObject();
        }

        return out.toByteArray();
    }
    private GelfMessageLevel extractLevel(Object rawLevel) {
        if (rawLevel != null)
            if (rawLevel instanceof Number)
                return GelfMessageLevel.fromNumericLevel(((Number) rawLevel).intValue());
        if (rawLevel instanceof String)
            try {
                return GelfMessageLevel.fromNumericLevel(Integer.getInteger(rawLevel.toString()));
            } catch(NumberFormatException e) {
                return null;
            }
        return null;
    }

    protected GelfMessage toGELFMessage(final Message message) {
        final DateTime timestamp;
        if (message.getField("timestamp") != null || message.getField("timestamp") instanceof DateTime) {
            timestamp = (DateTime) message.getField("timestamp");
        } else {
            timestamp = Tools.iso8601();
        }

        final GelfMessageLevel messageLevel = extractLevel(message.getField("level"));
        final String fullMessage = (String) message.getField("message");
        final String facility = (String) message.getField("facility");
        final String forwarder = ZeroMQGelfOutput.class.getCanonicalName();

        final GelfMessageBuilder builder = new GelfMessageBuilder(message.getMessage(), message.getSource())
                .timestamp(timestamp.getMillis() / 1000.0d)
                .additionalField("_forwarder", forwarder)
                .additionalFields(message.getFields());

        if (messageLevel != null)
            builder.level(messageLevel);

        if (fullMessage != null) {
            builder.fullMessage(fullMessage);
        }

        if (facility != null) {
            builder.additionalField("_facility", facility);
        }

        return builder.build();
    }

    public interface Factory extends MessageOutput.Factory<ZeroMQGelfOutput> {
        @Override
        ZeroMQGelfOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("ZeroMQ PUSH output", false, "", "An output publishing every message via PUSH device.");
        }
    }


    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest cr = new ConfigurationRequest();
            cr.addField(new TextField(
                    ZMQ_ADDRESS,
                    "Device address",
                    "",
                    "Address to connect or bind to, e.g. tcp://127.0.0.1:4444"
            ));
            cr.addField(new BooleanField(
                    ZMQ_SHOULD_BIND,
                    "Bind to address",
                    true,
                    "The PUSH device can either listen to incoming connections or connect by itself. Check this " +
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
