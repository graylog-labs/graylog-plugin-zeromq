package org.graylog.plugin.zeromq.outputs;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.MessageOutput;

import java.util.List;

public class ZeroMQOutput implements MessageOutput {
    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void write(Message message) throws Exception {

    }

    @Override
    public void write(List<Message> messages) throws Exception {

    }
}
