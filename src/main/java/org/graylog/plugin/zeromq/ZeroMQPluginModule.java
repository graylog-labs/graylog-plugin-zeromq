package org.graylog.plugin.zeromq;

import org.graylog.plugin.zeromq.inputs.GELFZeroMQInput;
import org.graylog.plugin.zeromq.inputs.RawZeroMQInput;
import org.graylog.plugin.zeromq.inputs.SyslogZeroMQInput;
import org.graylog.plugin.zeromq.transports.ZeroMQTransport;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

public class ZeroMQPluginModule extends PluginModule {

    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {
        installTransport(transportMapBinder(),
                         "zeromq",
                         ZeroMQTransport.class);
        addMessageInput(GELFZeroMQInput.class);
        addMessageInput(RawZeroMQInput.class);
        addMessageInput(SyslogZeroMQInput.class);

        //addMessageOutput(ZeroMQOutput.class);

    }
}
