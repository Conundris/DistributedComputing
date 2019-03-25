package sample;

import java.net.*;
import java.io.*;

/**
 * This class is a module which provides the application logic
 * for an FileTransferProtocol using connectionless datagram socket.
 * @author M. L. Liu
 */
public class ClientHelper {
   private MyClientDatagramSocket mySocket;
   private InetAddress serverHost;
   private int serverPort;
   ClientHelper(String hostName, String portNum)
      throws SocketException, UnknownHostException {
      this.serverHost = InetAddress.getByName(hostName);
      this.serverPort = Integer.parseInt(portNum);
      // instantiates a datagram socket for both sending
      // and receiving data
      this.mySocket = new MyClientDatagramSocket();
   }
   public String send(String message)
           throws SocketException, IOException {
      String response = "";
      mySocket.sendMessage( serverHost, serverPort, message);
      // now receive the echo
      response = mySocket.receiveMessage();
      return response;
   }
   public void done( ) throws SocketException {
      mySocket.close( );
   }  //end done
} //end class
