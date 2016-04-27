package org.graylog.plugin.zeromq;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Collection;
import java.util.Collections;

public class ZeroMQPlugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new ZeroMQPluginMetaData();
    }

    @Override
    public Collection<PluginModule> modules() {
        return Collections.<PluginModule>singleton(new ZeroMQPluginModule());
    }
}
