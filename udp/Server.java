import java.util.Map;

public interface Server extends AutoCloseable {
    /**
     * Starts a new Hello server.
     * This method should return immediately.
     *
     * @param threads number of working threads.
     * @param ports port no to response format mapping.
     */
    void start(int threads, Map<Integer, String> ports);
    /**
     * Starts a new Hello server.
     * This method should return immediately.
     *
     * @param port server port.
     * @param threads number of working threads.
     */
    void start(int port, int threads);

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    void close();
}
