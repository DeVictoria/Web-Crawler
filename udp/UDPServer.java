import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPServer implements Server {

    private Map<Integer, DatagramSocket> mapPortsSocket;
    private ExecutorService threadsService;
    private ExecutorService oneThread;

    private int bufferSize;

    private final AtomicBoolean isClose = new AtomicBoolean(false);


    private static final String errorMessage = "Incorrect input, try:   HelloUDPServer port [threads]\n";

    private static int parseInt(String[] args, int number, int defaultVal) {
        if (args.length > number) {
            try {
                return Integer.parseInt(args[number]);
            } catch (NumberFormatException e) {
                throw new NumberFormatException(errorMessage);
            }
        } else {
            return defaultVal;
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        int port = parseInt(args, 0, 8080);
        int threads = parseInt(args, 1, 4);
        try (UDPServer server = new UDPServer()) {
            server.start(port, threads);
        }
    }

    @Override
    public void start(int threads, Map<Integer, String> ports) {
        List<Integer> listPorts = new ArrayList<>();
        mapPortsSocket = new HashMap<>();
        for (Integer port : ports.keySet()) {
            try {
                DatagramSocket socket = new DatagramSocket(port);
                listPorts.add(port);
                mapPortsSocket.put(port, socket);
            } catch (SocketException e) {
                System.err.printf("port %d die", port);
                System.err.println(e.getMessage());
            }
        }
        threadsService = Executors.newFixedThreadPool(threads);
        oneThread = Executors.newFixedThreadPool(Math.max(1, ports.size()));
        for (Integer port : listPorts) {
            oneThread.submit(() -> {
                while (!isClose.get()) {
                    DatagramSocket socket = mapPortsSocket.get(port);
                    try {
                        bufferSize = socket.getReceiveBufferSize();
                    } catch (SocketException e) {
                        System.err.println(e.getMessage());
                        continue;
                    }
                    try {
                        final DatagramPacket packetTake = new DatagramPacket(new byte[bufferSize], bufferSize);
                        socket.receive(packetTake);
                        threadsService.submit(() -> {
                            String stringData = ports.get(port).replaceAll(
                                    "\\$",
                                    new String(packetTake.getData(), packetTake.getOffset(), packetTake.getLength(),
                                            StandardCharsets.UTF_8
                                    )
                            );
                            packetTake.setData(stringData.getBytes());
                            try {
                                socket.send(packetTake);
                            } catch (IOException e) {
                                System.err.println("can't send packet ");
                                System.err.println(e.getMessage());
                            }
                        });

                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }

                }
            });
        }


    }

    @Override
    public void start(int port, int threads) {
        start(threads, Map.of(port, "Hello, $"));
    }

    @Override
    public void close() {

        for (DatagramSocket socket : mapPortsSocket.values()) {
            socket.close();
        }
        isClose.set(true);
        oneThread.close();
        threadsService.close();
    }
}
