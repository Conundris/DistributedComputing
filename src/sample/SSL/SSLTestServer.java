package sample.SSL;

import java.nio.ByteBuffer;

public class SSLTestServer {
    public static void main(String[] args) {
        SSLServer server = new SSLServer();

        server.doServerSide("localhost", 3000);
    }
}
