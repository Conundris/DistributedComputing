package sample.SSL;

import sample.DatagramMessage;
import sample.SSLStuff;

import javax.net.ssl.SSLEngine;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SSLServer {
    private SSLEngine engine;
    private DatagramSocket mySocket;

    public SSLServer() {
        try {
            engine = SSLStuff.createSSLEngine(false);
            this.mySocket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //public DatagramMessage

    /*
     * Define the server side of the test.
     */
    void doServerSide(String hostName, int portNum) {
        try {
            InetSocketAddress clientSocketAddr = new InetSocketAddress(
                    InetAddress.getByName(hostName), portNum);

            // handshaking
            ByteBuffer appData = SSLStuff.handshake(
                    engine, mySocket, clientSocketAddr, true);

            // write server application data
        /*SSLStuff.sendAppData(engine, socket, serverApp.duplicate(),
                clientSocketAddr, "Server");*/

            if (appData == null) {
                System.out.println("No Application data received on server side.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
