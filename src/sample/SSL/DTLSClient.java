package sample.SSL;

import sample.DatagramMessage;

import javax.net.ssl.SSLEngine;
import java.net.*;
import java.nio.ByteBuffer;

@SuppressWarnings("Duplicates")
public class DTLSClient {

    private SSLEngine engine;
    private DatagramSocket mySocket;

    public DTLSClient(int portNum) {
        try {
            this.mySocket = new DatagramSocket(portNum);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("DTLSClient couldn't be initialized");
        }
    }

    public DatagramMessage sendAndReceive(ByteBuffer message, String hostName, int portNum ) {
        try {
            engine = DTLS.createSSLEngine(true);

            InetSocketAddress serverSocketAddr = new InetSocketAddress(
                    InetAddress.getByName(hostName), portNum);

            DTLS.handshake(engine, mySocket, serverSocketAddr, false);

            DTLS.sendAppData(engine, mySocket, message.duplicate(), serverSocketAddr, "Client");

            System.out.println("SENT DATA");

            DatagramMessage receivedData = DTLS.receiveAppData(engine, mySocket, "Client");

            if (receivedData == null) {
                System.out.println("No Application data received on client side.");
            } else {
                System.out.println("GOT MESSAGE");
                System.out.println(receivedData.getMessage());
                return receivedData;
            }

            return null;

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void done() {
        mySocket.close();
    }
}
