package sample;

import java.net.*;
import java.io.*;

/**
 * This class is a module which provides the application logic
 * for an FileTransferProtocol using connectionless datagram socket.
 * @author M. L. Liu
 */
public class ClientHelper extends DatagramSocket {
   private MyClientDatagramSocket mySocket;
   private InetAddress serverHost;
   private int serverPort;

   private static final int MAX_LEN = 1024;

   ClientHelper(String hostName, int portNum)
      throws SocketException, UnknownHostException {
      this.serverHost = InetAddress.getByName(hostName);
      this.serverPort = portNum;
      // instantiates a datagram socket for both sending
      // and receiving data
      this.mySocket = new MyClientDatagramSocket();
   }
   public String sendAndReceive(String message) throws SocketException, IOException {
      String response;

      mySocket.sendMessage( serverHost, serverPort, message);

      // now receive the echo
      response = mySocket.receiveMessage();

      return response;
   }
   public void done( ) throws SocketException {
      mySocket.close( );
   }

   public void send(String message) throws IOException {
      byte[] sendBuffer = message.getBytes();
      DatagramPacket datagram = new DatagramPacket(sendBuffer, sendBuffer.length, serverHost, serverPort);
      send(datagram);
   }

   public Object receiveConfirmationMessage() throws IOException {
      byte[] receiveBuffer = new byte[MAX_LEN];
      DatagramPacket datagram = new DatagramPacket(receiveBuffer, MAX_LEN);
      receive(datagram);
      return new String(receiveBuffer);
   }

   public Object receiveFilePacketsWithSender() throws IOException, ClassNotFoundException {

      byte[] incomingData = new byte[MAX_LEN * 1000 * 50];
      DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
      receive(incomingPacket);
      byte[] data = incomingPacket.getData();
      ByteArrayInputStream in = new ByteArrayInputStream(data);
      ObjectInputStream is = new ObjectInputStream(in);
      return is.readObject();
   }
}
