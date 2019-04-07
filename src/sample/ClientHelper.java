package sample;

import sample.SSL.DTLSClient;

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

/**
 * This class is a module which provides the application logic
 * for an FileTransferProtocol using connectionless datagram socket.
 * @author M. L. Liu
 */
public class ClientHelper extends DatagramSocket {
   private String serverHost;
   private int serverPort;

   private DTLSClient client;

   ClientHelper(int portNum, String serverHost)
           throws SocketException {
      this.serverHost = serverHost;
      this.serverPort = portNum;
      client = new DTLSClient(serverPort);
   }


   public void download(String username, String fileName, File path) throws IOException {
      String message = RequestCode.DDL + "§ " + username + "§ " + fileName;
      String result = client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), "localhost", 3000).getMessage().trim();

      System.out.println("Response received" + result);

      String[] splitresult = result.split(":");

      if(Short.parseShort(splitresult[0].trim()) == ResponseCode.DOWNLOAD_SUCCESSFUL) {
         FileOutputStream fos = new FileOutputStream(path);
         fos.write(Base64.getDecoder().decode(splitresult[1].trim()));
         fos.close();

         System.out.println("File Downloaded to this destination: " + path);
      } else {
         System.out.println("File Has not been downloaded successfully, ERROR");
      }
   }

   public String upload(String username, File file) throws IOException {
      String encodedString = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));

      String message = RequestCode.UPL + "§" +  username + "§" + file.getName() + "§" + encodedString;

      return client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), serverHost, serverPort).getMessage();
   }

   public String logout(String username, String password) {
      String message = RequestCode.LOGOUT + "§ " + username + "§ " + password;
      return client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), serverHost, serverPort).getMessage();
   }

   public String login(String username, String password) {
      String message = RequestCode.LOGIN + "§ " + username + "§ " + password;
      return client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), serverHost, serverPort).getMessage();
   }

   public List<String> getFileListForUser(String username) throws IOException, ClassNotFoundException {
      String returnVal = client.sendAndReceive(ByteBuffer.wrap((RequestCode.DDLIST +"§ " + username).getBytes()), serverHost, serverPort).getMessage().trim();

      String[] splitReturnVal = returnVal.split(":");

      if(Short.parseShort(splitReturnVal[0].trim()) == ResponseCode.USER_FILES_LISTED) {
         System.out.println(splitReturnVal[0] + ": FileList retrieved.");

         ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(splitReturnVal[1].trim()));
         ObjectInputStream is = new ObjectInputStream(in);

         return (List<String>) is.readObject();
      } else {
         System.out.println(splitReturnVal[0] + ": FileList couldn't be retrieved.");
         return null;
      }
   }

   public void closeSSLClient() {
      client.done();
   }
}
