package sample.SSL;

import sample.DatagramMessage;

import javax.net.ssl.SSLEngine;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class DTLSServer {
    private SSLEngine engine;
    private DatagramSocket mySocket;

    public DTLSServer(int portNum) {
        try {
            this.mySocket = new DatagramSocket(portNum);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DatagramMessage receive(String hostName, int portNum) {
        try {
            engine = DTLS.createSSLEngine(false);

            InetSocketAddress clientSocketAddr = new InetSocketAddress(
                    InetAddress.getByName(hostName), portNum);

            // handshaking
            DatagramMessage appData = DTLS.handshake(
                    engine, mySocket, clientSocketAddr, true);

            if (appData == null) {
                System.out.println("No Application data received on server side.");
            } else {
                System.out.println("GOT MESSAGE");
                System.out.println(appData.getMessage());
                return appData;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendMessage(InetAddress address, int port, String message) {
        try {
            InetSocketAddress clientSocketAddr = new InetSocketAddress(
                    address, port);

            DTLS.sendAppData(this.engine, mySocket, ByteBuffer.wrap(message.getBytes()), clientSocketAddr, "Server");
            System.out.println("SENT DATA");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
