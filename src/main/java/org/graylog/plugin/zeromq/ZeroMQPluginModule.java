package org.graylog.plugin.zeromq;

import org.graylog.plugin.zeromq.inputs.GELFZeroMQInput;
import org.graylog.plugin.zeromq.inputs.RawZeroMQInput;
import org.graylog.plugin.zeromq.inputs.SyslogZeroMQInput;
import org.graylog.plugin.zeromq.outputs.ZeroMQGelfOutput;
import org.graylog.plugin.zeromq.transports.ZeroMQTransport;
import org.graylog2.plugin.PluginModule;

public class ZeroMQPluginModule extends PluginModule {
    @Override
    protected void configure() {
        installTransport(transportMapBinder(), "zeromq", ZeroMQTransport.class);
        addMessageInput(GELFZeroMQInput.class);
        addMessageInput(RawZeroMQInput.class);
        addMessageInput(SyslogZeroMQInput.class);

        addMessageOutput(ZeroMQGelfOutput.class, ZeroMQGelfOutput.Factory.class);
    }
}
