package org.graylog.plugin.zeromq.transports;

import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.commons.lang3.RandomStringUtils;
import org.zeromq.ZMQ;

import java.util.concurrent.TimeUnit;

/**
 * Pushes random strings to the specified zmq device until stopped.
 */
public class PushProducer implements Runnable {

    private final String address;

    public PushProducer(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: PushProducer <zmq address>");
            System.exit(1);
        }
        address = args[0];
    }

    public static void main(String[] args) {
        new PushProducer(args).run();
    }

    @Override
    public void run() {
        try (ZMQ.Context context = ZMQ.context(1)) {
            try (ZMQ.Socket socket = context.socket(ZMQ.PUSH)) {
                socket.connect(address);

                while (true) {
                    final String s = RandomStringUtils.randomAlphanumeric(100);
                    socket.send(s.getBytes());
                    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                }
            }
        }
    }
}
