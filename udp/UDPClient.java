import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPClient implements Client {

    private static final String errorMessage = "Incorrect input, try:   HelloUDPClient host [port [prefix [threads [requests]]]]\n";

    private static int parseInt(String[] args, int number, int defaultVal) {
        if (args.length > number) try {
            return Integer.parseInt(args[number]);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(errorMessage);
        }
        else return defaultVal;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) throw new IllegalArgumentException(errorMessage);
        String host = args[0];
        int port = parseInt(args, 1, 443);
        String prefix = args[2];
        int threads = parseInt(args, 3, 4);
        int requests = parseInt(args, 4, 4);
        new UDPClient().run(host, port, prefix, threads, requests);
    }

    private String message(DatagramPacket packet) {
        return new String(packet.getData());
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try (final ExecutorService threadsService = Executors.newFixedThreadPool(threads)) {
            final InetSocketAddress address = new InetSocketAddress(host, port);
            for (int i = 1; i <= threads; i++) {
                final int threadNumber = i;
                threadsService.submit(() -> {
                    try (DatagramSocket socket = new DatagramSocket()) {
                        socket.setSoTimeout(50);
                        for (int requestNumber = 1; requestNumber <= requests; requestNumber++) {
                            byte[] data = String.format("%s%d_%d", prefix, threadNumber, requestNumber).getBytes();
                            final DatagramPacket packetSend = new DatagramPacket(data, data.length, address);
                            final DatagramPacket packetTake = new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                            while (true) {
                                try {
                                    socket.send(packetSend);
                                } catch (IOException e) {
                                    continue;
                                }
                                String req = message(packetSend);
                                System.out.printf("request: %s\n", req);

                                try {
                                    socket.receive(packetTake);
                                } catch (IOException e) {
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                }
                                String res = message(packetTake);
                                if (res.contains(req)) {
                                    System.out.printf("the server returned: %s\n\n", message(packetTake));
                                    break;
                                }
                            }
                        }
                    } catch (SocketException e) {
                        System.err.printf("thread №%d died, because the socket was not created\n", threadNumber);
                        System.err.println(e.getMessage());
                    }
                });
            }
        }
    }
}

