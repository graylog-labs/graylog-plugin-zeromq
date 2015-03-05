package org.graylog.plugin.zeromq.transports;

import org.zeromq.ZMQ;

public class PullConsumer implements Runnable {
    private final String address;

    public PullConsumer(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: PullConsumer <zmq address>");
            System.exit(1);
        }
        address = args[0];
    }

    public static void main(String[] args) {
        new PullConsumer(args).run();
    }

    @Override
    public void run() {
        try (ZMQ.Context context = ZMQ.context(1)) {
            try (ZMQ.Socket socket = context.socket(ZMQ.PULL)) {
                socket.bind(address);
                int i = 0;
                while (true) {
                    final String str = socket.recvStr();
                    if (str != null) {
                        System.out.println(i++ + ": " + str);
                    }
                }
            }
        }
    }
}
