package sample.SSL;

public class SSLTestServer {
    public static void main(String[] args) {
        SSLServer server = new SSLServer();

        while(true) {
            server.receive("localhost", 3001);
        }
    }
}
