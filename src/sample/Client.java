package sample;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This module contains the presentaton logic of a FTP System.
 * Adaption of an EchoClient from M. L. Liu
 * @author M. L. Liu
 */
public class Client {
   private static final int DEFAULTPORT = 3000;
   private static User user = new User();


   public static void main(String[] args) {
      InputStreamReader is = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(is);
      try {
         System.out.println("Welcome to the File Management System client");
         String hostName = "localhost";
         String portNum = "3000";
         ClientHelper helper = new ClientHelper(hostName, portNum);
         boolean done = false;
         boolean loggedin = false;
         String message;
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
               if(serverResult == String.valueOf(ResponseCode.USER_LOGGED_IN_PROCEED)) {
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

   private static boolean handleInput(BufferedReader br, ClientHelper helper, boolean done, String option) throws IOException, EmptyArgsException {
      String serverResult;
      switch (option) {
         case "1": //Logout
            System.out.println("You want to log out");
            System.out.println("Enter username");
            serverResult = logout(user.getUsername(), user.getPassword());

            if(serverResult == String.valueOf(ResponseCode.USER_LOGGED_OUT_SERVICE_TERMINATED)) {
               user.setUsername("");
               user.setPassword("");
               user.setLoggedIn(false);
            }
            System.out.println(serverResult);
            break;
         case "2": //Upload
            //Check Users Details
            System.out.println("You want to upload");
            if (user.isLoggedIn()){
               System.out.println("Please ensure the file you want to upload is in C:\\Users\\exceeds\\Downloads\\FileManagementSystem-master\\DistributedComputingFileMgmtSystem\\" +
                       "\nEnter file name: ");
               String filePath = br.readLine();

               boolean fileChosen = false;
               JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
               File selectedFile;

               while(!fileChosen) {
                  int returnValue = jfc.showOpenDialog(null);
                  // int returnValue = jfc.showSaveDialog(null);

                  if (returnValue == JFileChooser.APPROVE_OPTION) {
                     if(getFileSizeKiloBytes(jfc.getSelectedFile()) > 64) {
                        System.out.println("Filesize can't be bigger than 64KB.");
                     } else {
                        selectedFile = jfc.getSelectedFile();
                        fileChosen = true;
                     }
                  }
               }

               //Save File as
               System.out.println("The File Management System supports the following file types: " +
                       "\n.jpg, .txt, .png, .pdf, .doc,");
               System.out.println("Enter server file name");
               String fileName = br.readLine();
               System.out.println("File you want to upload: " + fileName);
               String fileType = fileName.substring(fileName.length() - 4);
               boolean isValidFile = validateFileType(fileType);
               if (isValidFile == false) {
                  throw new EmptyArgsException("Invalid file type");
               }
               System.out.println("Your file type" + fileType);
               serverResult = upload(user.getUsername(), fileName);
               // If serverResult(Code)

               System.out.println(serverResult);
               break;
            } else {
               System.out.println(user.getUsername() + " is not logged in");
               break;
            }
         /*case "3": //Downloads
            System.out.println("You want to download");
            LoggedInUsers.AddToList(new User("AoifeSayers", "Hi"));
            LoggedInUsers.getLoggedInUsers();
            System.out.println("Enter username");
            username = br.readLine();
            if (LoggedInUsers.isLoggedIn(username)==false){
               System.out.println(username + " is not logged in");
               break;
            }
            serverResult = helper.send("4, " + username + ", "  +"getDirectory");
            System.out.println(serverResult);
            System.out.println("Enter file name you wish to download");
            fileName = br.readLine();
            System.out.println("Enter name you wish to call the file");
            String saveFileAs = br.readLine();
            serverResult = download(username, fileName, saveFileAs);
            System.out.println(serverResult);

            break;*/
         case "4": //Quit
            System.out.println("Quitting!");
            helper.done();
            done = true;
            break;
         default:
            System.out.println("Invalid option! Try again");
            break;
      }// end switch
      return done;
   }

   public static String download(String username, String fileName, String saveFileAs) throws IOException {
      ClientHelper helper = new ClientHelper("localhost", String.valueOf(DEFAULTPORT));
      String result = helper.send("4, " + username + ", " + fileName);
      System.out.println("Result received" + result);
      FileOutputStream fos = new FileOutputStream("C:\\Users\\exceeds\\Downloads\\FileManagementSystem-master\\DistributedComputingFileMgmtSystem\\" + saveFileAs);
      fos.write(result.getBytes());
      fos.close();
      System.out.println("File Downloaded to this destination: C:\\FileManagementSystem\\DistributedComputingFileMgmtSystem\\" + saveFileAs);
      return  result;
   }
   public static String upload(String username, String fileName) throws IOException {
      ClientHelper helper = new ClientHelper("localhost",String.valueOf(DEFAULTPORT));
      String homePath = "C:\\Users\\exceeds\\Downloads\\FileManagementSystem-master\\DistributedComputingFileMgmtSystem\\";
      Path path = Paths.get(homePath + fileName);
      byte[] data = Files.readAllBytes(path);
      String byteDataString = new String(data);
      String serverResult = helper.send("3" + ", " +  username + ", " + fileName + ", " + byteDataString);
      return serverResult;
   }
   public static String logout(String username, String password) throws IOException {
      ClientHelper helper = new ClientHelper("localhost", String.valueOf(DEFAULTPORT));
      String message = "2" + ", " + username + ", " + password;
      String serverResult = helper.send(message);
      return serverResult;
   }
   public static String login(String username, String password) throws IOException {
      ClientHelper helper = new ClientHelper("localhost", String.valueOf(DEFAULTPORT));
      String message = String.valueOf(ProtocolCode.LOGIN) + ", " + username + ", " + password;
      String serverResult = helper.send(message);
      return serverResult;
   }
   public static boolean validateFileType(String fileType) {
      if (fileType.equalsIgnoreCase(".jpg") || fileType.equalsIgnoreCase(".txt") ||
              fileType.equalsIgnoreCase(".png") || fileType.equalsIgnoreCase(".pdf") ||
              fileType.equalsIgnoreCase(".doc")) {
         return true;
      }
      return false;
   }
   private static int getFileSizeKiloBytes(File file) {
      return (int) file.length() / 1024;
   }
} // end class      
