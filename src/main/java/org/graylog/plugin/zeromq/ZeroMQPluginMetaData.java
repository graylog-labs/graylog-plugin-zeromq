package org.graylog.plugin.zeromq;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class ZeroMQPluginMetaData implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return "org.graylog.plugin.zeromq.ZeroMQPluginPlugin";
    }

    @Override
    public String getName() {
        return "ZeroMQ Plugin";
    }

    @Override
    public String getAuthor() {
        return "Graylog, Inc.";
    }

    @Override
    public URI getURL() {
        return URI.create("https://www.graylog.org/");
    }

    @Override
    public Version getVersion() {
        return new Version(1, 1, 2);
    }

    @Override
    public String getDescription() {
        return "ZeroMQ support for Graylog";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(2, 0, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
