package sample;

import sample.SSL.SSLClient;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.List;

/**
 * This module contains the presentaton logic of a FTP System.
 * Adaption of an EchoClient from M. L. Liu
 * @author M. L. Liu
 */
@SuppressWarnings("Duplicates")
public class Client {
   private static final int DEFAULTPORT = 3000;
   private static User user = new User();
   private static SSLClient client = new SSLClient();

   public static void main(String[] args) {
            InputStreamReader is = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(is);
      try {
         System.out.println("Welcome to the File Management System client");
         String hostName = "localhost";
         String portNum = "3000";
         ClientHelper helper = new ClientHelper(hostName, portNum);
         boolean done = false;
         String serverResult;

         //Program Loop
         while (!done) {
            // Login Loop
            while (!user.isLoggedIn()) {
               System.out.println("Login");
               System.out.println("Enter username");
               String username = br.readLine();
               System.out.println("Enter password");
               String password = br.readLine();
               if (username.equals("") || username.isEmpty() ||
                       password.equals("") || password.isEmpty()) {
                  throw new EmptyArgsException("You left Empty Fields");
               }
               serverResult = login(username, password);
               if(Utils.extractOpcode(serverResult) == ResponseCode.USER_LOGGED_IN_PROCEED) {
                  user.setUsername(username);
                  user.setPassword(password);
                  user.setLoggedIn(true);
               }
               System.out.println(serverResult);
            }
            System.out.println("--------Enter your option-----------------" +
                    "\n1. Logout" +
                    "\n2. Upload" +
                    "\n3. Download" +
                    "\n4 Quit");
            String option = br.readLine();
            done = handleInput(br, helper, done, option);
         } // end while
      } // end try  
      catch (Exception ex) {
         ex.printStackTrace();
      } // end catch
   } //end main

   private static boolean handleInput(BufferedReader br, ClientHelper helper, boolean done, String option) throws IOException, EmptyArgsException, ClassNotFoundException {
      String serverResult;
      switch (option) {
         case "1": //Logout
            System.out.println(user.getUsername() + " logging out.");
            serverResult = logout(user.getUsername(), user.getPassword());

            if(Utils.extractOpcode(serverResult) == ResponseCode.USER_LOGGED_OUT_SERVICE_TERMINATED) {
               user.setUsername("");
               user.setPassword("");
               user.setLoggedIn(false);
            }
            System.out.println(serverResult);
            break;
         case "2": //Upload
            System.out.println("Upload:");
            if (user.isLoggedIn()){
               boolean fileChosen = false;

               JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
               JDialog dialog = new JDialog();
               File selectedFile;

               while(!fileChosen) {
                  int returnValue = jfc.showOpenDialog(dialog);

                  if (returnValue == JFileChooser.APPROVE_OPTION) {
                     if(Utils.getFileSizeKiloBytes(jfc.getSelectedFile()) > 64) {
                        System.out.println("Filesize can't be bigger than 64KB.");
                     } else {
                        selectedFile = jfc.getSelectedFile();
                        fileChosen = true;

                        serverResult = upload(user.getUsername(), selectedFile);
                        System.out.println(serverResult);
                     }
                  }
               }
               break;
            } else {
               System.out.println(user.getUsername() + " is not logged in");
               break;
            }
         case "3": //Downloads
            File selectedFileToSave = null;
            boolean selectedFile = false;
            int fileToDownload = -1;

            System.out.println("Download");

            String returnVal = client.sendAndReceive(ByteBuffer.wrap(("200§ " + user.getUsername()).getBytes()), "localhost", 3000).getMessage();
            returnVal = returnVal.trim();
            ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(returnVal));
            ObjectInputStream is = new ObjectInputStream(in);
            List<String> files = (List<String>) is.readObject();

            while(!selectedFile) {

               for (int i = 0; i < files.size(); i++) {
                  System.out.println(i + ": " + files.get(i));
               }

               System.out.println("Enter which file you wish to download");
               String answer = br.readLine();

               try {
                  fileToDownload = Integer.parseInt(answer.trim());
                  selectedFile = true;
               } catch (NumberFormatException nfe) {
                  System.out.println("NumberFormatException: " + nfe.getMessage());
                  System.out.println("Please input only a number.");
               }
            }
            if(fileToDownload != -1){

               JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory() + "\\" + files.get(fileToDownload));
               int returnValue = jfc.showSaveDialog(null);

               if(returnValue == JFileChooser.APPROVE_OPTION) {
                  selectedFileToSave = jfc.getSelectedFile();
                  System.out.println(selectedFileToSave.getName());
               }

               download(user.getUsername(), files.get(fileToDownload), selectedFileToSave);
            }

            break;
         case "4": //Quit
            System.out.println("Quitting!");
            // if user logged in -> logging him out
            logout(user.getUsername(), user.getPassword());
            // Close Socket
            helper.done();
            done = true;
            break;
         default:
            System.out.println("Invalid option! Try again");
            break;
      }// end switch
      return done;
   }

   private static List<String> getFileList() throws IOException, ClassNotFoundException {

      ClientHelper helper = new ClientHelper("localhost", String.valueOf(DEFAULTPORT));
      //List<String> files = (List<String>) helper.receiveFilePacketsWithSender();

      return null;
   }

   public static void download(String username, String fileName, File path) throws IOException {
      String message = "4§ " + username + "§ " + fileName;
      String result = client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), "localhost", 3000).getMessage();
      result = result.trim();

      System.out.println("Result received" + result);
      FileOutputStream fos = new FileOutputStream(path);
      fos.write(Base64.getDecoder().decode(result));
      fos.close();

      System.out.println("File Downloaded to this destination: " + path);
   }

   public static String upload(String username, File file) throws IOException {
      String encodedString = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));

      String message = ProtocolCode.WRQ + "§" +  username + "§" + file.getName() + "§" + encodedString;

      return client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), "localhost", 3000).getMessage();
   }

   public static String logout(String username, String password) throws IOException {
      String message = "2" + "§ " + username + "§ " + password;
      return client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), "localhost", 3000).getMessage();
   }
   public static String login(String username, String password) throws IOException {
      String message = ProtocolCode.LOGIN + "§ " + username + "§ " + password;
      return client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), "localhost", 3000).getMessage();
   }
}
