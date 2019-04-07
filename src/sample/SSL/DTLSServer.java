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

    public DTLSServer() {
        try {
            this.mySocket = new DatagramSocket(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SSLEngine getEngine() {
        return this.engine;
    }

    //public DatagramMessage

    /*
     * Define the server side of the test.
     */
    // TODO: remove parameters since they are for the specific client. RETARDED
    public DatagramMessage receive(String hostName, int portNum) {
        try {
            engine = DTLS.createSSLEngine(false);

            InetSocketAddress clientSocketAddr = new InetSocketAddress(
                    InetAddress.getByName(hostName), portNum);

            // handshaking
            DatagramMessage appData = DTLS.handshake(
                    engine, mySocket, clientSocketAddr, true);

            // write server application data
        /*DTLS.sendAppData(engine, mySocket, serverApp.duplicate(),
                clientSocketAddr, "Server");*/

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

    public String send(String hostName, int portNum) {
        return "";
    }

    public void sendMessage(SSLEngine engine, InetAddress address, int port, String message) {
        try {
            InetSocketAddress clientSocketAddr = new InetSocketAddress(
                    address, port);

            //DTLS.handshake(this.engine, mySocket, clientSocketAddr, false);

            DTLS.sendAppData(this.engine, mySocket, ByteBuffer.wrap(message.getBytes()), clientSocketAddr, "Server");
            System.out.println("SENT DATA");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(SSLEngine engine, InetAddress address, int port, byte[] message) {
        try {
            InetSocketAddress clientSocketAddr = new InetSocketAddress(
                    address, port);

            //DTLS.handshake(this.engine, mySocket, clientSocketAddr, false);

            DTLS.sendAppData(this.engine, mySocket, ByteBuffer.wrap(message), clientSocketAddr, "Server");
            System.out.println("SENT DATA");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
