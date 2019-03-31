package sample.SSL;

import java.nio.ByteBuffer;

public class SSLTestClient {
    public static void main(String[] args) {
        SSLClient client = new SSLClient();

        client.sendAndReceive(ByteBuffer.wrap("Hi Client, I'm Server".getBytes()), "localhost", 3000);
    }
}
