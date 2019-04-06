package sample.SSL;

import sample.DatagramMessage;
import sample.SSLStuff;

import javax.net.ssl.SSLEngine;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.nio.ByteBuffer;

@SuppressWarnings("Duplicates")
public class SSLClient {

    private SSLEngine engine;
    private DatagramSocket mySocket;

    public SSLClient() {
        try {
            this.mySocket = new DatagramSocket(3001);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] send(ByteBuffer message, String hostName, int portNum) {
        try {
            engine = SSLStuff.createSSLEngine(true);

            InetSocketAddress serverSocketAddr = new InetSocketAddress(
                    InetAddress.getByName(hostName), portNum);
            SSLStuff.handshake(engine, mySocket, serverSocketAddr, false);

            SSLStuff.sendAppData(engine, mySocket, message.duplicate(), serverSocketAddr, "Client");

            /*ByteBuffer receivedData = SSLStuff.receiveAppData(engine, mySocket, "Client");

            if (receivedData == null) {
                System.out.println("No Application data received on client side.");
            } else {
                System.out.println("GOT MESSAGE");
                System.out.println(new String(receivedData.array()));
            }*/

            //return receivedData.array();
            System.out.println(engine.getSession().isValid());
            return null;

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public DatagramMessage receive(String hostName, int portNum) {
        try {
            engine = SSLStuff.createSSLEngine(true);

            InetSocketAddress serverSocketAddr = new InetSocketAddress(
                    InetAddress.getByName(hostName), portNum);

            // handshaking
            //SSLStuff.handshake(engine, mySocket, serverSocketAddr, false);

            DatagramMessage receivedData = SSLStuff.receiveAppData(engine, mySocket, "Client");

            if (receivedData == null) {
                System.out.println("No Application data received on client side.");
            } else {
                System.out.println("GOT MESSAGE");
                System.out.println(receivedData.getMessage());
                return receivedData;
            }


        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    public DatagramMessage sendAndReceive(ByteBuffer message, String hostName, int portNum ) {
        try {
            engine = SSLStuff.createSSLEngine(true);

            InetSocketAddress serverSocketAddr = new InetSocketAddress(
                    InetAddress.getByName(hostName), portNum);

            SSLStuff.handshake(engine, mySocket, serverSocketAddr, false);

            SSLStuff.sendAppData(engine, mySocket, message.duplicate(), serverSocketAddr, "Client");

            System.out.println("SENT DATA");

            DatagramMessage receivedData = SSLStuff.receiveAppData(engine, mySocket, "Client");

            if (receivedData == null) {
                System.out.println("No Application data received on client side.");
            } else {
                System.out.println("GOT MESSAGE");
                System.out.println(receivedData.getMessage());
                return receivedData;
            }

            //return receivedData.array();
            return null;

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
