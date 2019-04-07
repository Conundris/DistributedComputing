package sample;

import sample.SSL.SSLClient;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
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
   private static SSLClient client = new SSLClient(DEFAULTPORT);

   public static void main(String[] args) {
            InputStreamReader is = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(is);
      try {
         System.out.println("Welcome to the File Management System client");

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
               }
               serverResult = login(username, password);
               if(Utils.extractResponseCode(serverResult) == ResponseCode.USER_LOGGED_IN_PROCEED) {
                  user.setUsername(username);
                  user.setPassword(password);
                  user.setLoggedIn(true);
               }
               System.out.println(serverResult);
            }
            // Main Menu
            System.out.println("--------Enter your option-----------------" +
                    "\n1. Logout" +
                    "\n2. Upload" +
                    "\n3. Download" +
                    "\n4 Quit");
            String option = br.readLine();
            done = handleInput(br, option);
         } // end while
      } // end try  
      catch (Exception ex) {
         ex.printStackTrace();
      } // end catch
   } //end main

   private static boolean handleInput(BufferedReader br, String option) throws IOException, ClassNotFoundException {
      String serverResult;
      boolean done = false;

      switch (option) {
         case "1": //Logout
            System.out.println(user.getUsername() + " logging out.");
            serverResult = logout(user.getUsername(), user.getPassword());

            if(Utils.extractResponseCode(serverResult) == ResponseCode.USER_LOGGED_OUT_SERVICE_TERMINATED) {
               user.setUsername("");
               user.setPassword("");
               user.setLoggedIn(false);
            }
            System.out.println(serverResult);
            break;
         case "2": //Uploading File
            System.out.println("Upload:");
            if (user.isLoggedIn()){
               boolean fileChosen = false;

               JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
               JDialog dialog = new JDialog();
               File selectedFile;

               while(!fileChosen) {
                  int returnValue = jfc.showOpenDialog(dialog);

                  if (returnValue == JFileChooser.APPROVE_OPTION) {
                     if(Utils.getFileSizeKiloBytes(jfc.getSelectedFile()) > 63) {
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
         case "3": //Downloading File
            System.out.println("Download from Server");

            File selectedFileToSave = null;
            boolean selectedFile = false;
            int fileToDownload = -1;
            List<String> files = getFileListForUser();

            //File Selection Loop
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
                  fileToDownload = -1;
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
            System.out.println("Quitting Application.");
            // if user logged in -> logging him out
            if(user.isLoggedIn()) logout(user.getUsername(), user.getPassword());
            // Close Socket
            done = true;
            client.done();
            break;
         default:
            System.out.println("Invalid option! Try again");
            break;
      }// end switch
      return done;
   }

   public static void download(String username, String fileName, File path) throws IOException {
      String message = RequestCode.DDL + "§ " + username + "§ " + fileName;
      String result = client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), "localhost", 3000).getMessage().trim();

      System.out.println("Response received" + result);
      FileOutputStream fos = new FileOutputStream(path);
      fos.write(Base64.getDecoder().decode(result));
      fos.close();

      System.out.println("File Downloaded to this destination: " + path);
   }

   public static String upload(String username, File file) throws IOException {
      String encodedString = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));

      String message = RequestCode.UPL + "§" +  username + "§" + file.getName() + "§" + encodedString;

      return client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), "localhost", 3000).getMessage();
   }

   public static String logout(String username, String password) {
      String message = RequestCode.LOGOUT + "§ " + username + "§ " + password;
      return client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), "localhost", 3000).getMessage();
   }

   public static String login(String username, String password) {
      String message = RequestCode.LOGIN + "§ " + username + "§ " + password;
      return client.sendAndReceive(ByteBuffer.wrap(message.getBytes()), "localhost", 3000).getMessage();
   }

   private static List<String> getFileListForUser() throws IOException, ClassNotFoundException {
      String returnVal = client.sendAndReceive(ByteBuffer.wrap((RequestCode.DDLIST +"§ " + user.getUsername()).getBytes()), "localhost", 3000).getMessage().trim();

      ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(returnVal));
      ObjectInputStream is = new ObjectInputStream(in);

      return (List<String>) is.readObject();
   }

}
