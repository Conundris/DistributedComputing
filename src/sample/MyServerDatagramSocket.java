package sample;

import java.net.*;
import java.io.*;

/**
 * A subclass of DatagramSocket which contains 
 * methods for sending and receiving messages
 * @author M. L. Liu
 */
public class MyServerDatagramSocket extends DatagramSocket {
static final int MAX_LEN = 1024;
   MyServerDatagramSocket(int portNo) throws SocketException {
     super(portNo);
   }

   public void sendMessage(InetAddress receiverHost, int receiverPort, String message) throws IOException {
    byte[ ] sendBuffer = message.getBytes( );
    DatagramPacket datagram = new DatagramPacket(sendBuffer, sendBuffer.length, receiverHost, receiverPort);
    this.send(datagram);
   }

    public void sendMessage(InetAddress receiverHost, int receiverPort, byte[] message) throws IOException {
        DatagramPacket datagram = new DatagramPacket(message, message.length, receiverHost, receiverPort);
        this.send(datagram);
    }

   public String receiveMessage( ) throws IOException {
     byte[ ] receiveBuffer = new byte[MAX_LEN];
     DatagramPacket datagram = new DatagramPacket(receiveBuffer, MAX_LEN);
     this.receive(datagram);
     String message = new String(receiveBuffer);
     return message;
   }

   public DatagramMessage receiveMessageAndSender( )
		throws IOException {		
         byte[ ] receiveBuffer = new byte[MAX_LEN];
         DatagramPacket datagram =
            new DatagramPacket(receiveBuffer, MAX_LEN);
         this.receive(datagram);
         // create a DatagramMessage object, to contain message
         //   received and sender's address
         DatagramMessage returnVal = new DatagramMessage( );
         returnVal.putVal(new String(receiveBuffer),
                          datagram.getAddress( ),
                          datagram.getPort( ));
         return returnVal;
   } //end receiveMessage

    public void sendFilePackets(Object data, InetAddress host, int port) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(outputStream);
        os.writeObject(data);
        byte[] byteData = outputStream.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(byteData, byteData.length, host, port);
        send(sendPacket);
    }
} //end class
