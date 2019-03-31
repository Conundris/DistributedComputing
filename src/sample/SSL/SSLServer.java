package sample.SSL;

import sample.DatagramMessage;
import sample.SSLStuff;

import javax.net.ssl.SSLEngine;
import javax.print.DocFlavor;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SSLServer {
    private SSLEngine engine;
    private DatagramSocket mySocket;

    public SSLServer() {
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
            engine = SSLStuff.createSSLEngine(false);

            InetSocketAddress clientSocketAddr = new InetSocketAddress(
                    InetAddress.getByName(hostName), portNum);

            // handshaking
            DatagramMessage appData = SSLStuff.handshake(
                    engine, mySocket, clientSocketAddr, true);

            // write server application data
        /*SSLStuff.sendAppData(engine, mySocket, serverApp.duplicate(),
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

    public void sendMessage(SSLEngine engine, InetAddress address, int port, String loginResp) {
        try {
            InetSocketAddress clientSocketAddr = new InetSocketAddress(
                    address, port);

            //SSLStuff.handshake(this.engine, mySocket, clientSocketAddr, false);

            SSLStuff.sendAppData(this.engine, mySocket, ByteBuffer.wrap(loginResp.getBytes()), clientSocketAddr, "Server");
            System.out.println("SENT DATA");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
