package sample.SSL;

import java.nio.ByteBuffer;

public class SSLTestClient {
    public static void main(String[] args) {
        SSLClient client = new SSLClient();

        client.send(ByteBuffer.wrap("Hi Server, I'm Client".getBytes()), "localhost", 3000);
    }
}
