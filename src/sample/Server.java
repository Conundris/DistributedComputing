package sample;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This module contains the application logic of an ftp server
 * which uses a connectionless datagram socket for interprocess 
 * communication.
 * A command-line argument is required to specify the server port.
 * @author M. L. Liu
 */
/* Codes for responses
Login success 500
Login unsuccessful 501

register success 600
register unsuccessful 601

logout succesfull 700
logout unsuccceful 701

upload success 800
upload unsuccessful 801

download success
download unsuccessful
 */
public class Server {
    private static List<User> listOfAllUsers = new ArrayList<>();

    public static void main(String[] args) {
        final int DEFAULTSERVERPORT = 3000;

        populateUsers();

        String messageCode;
        String username;
        String password;
        String fileName;
        String outputPath;


        try {
            // instantiates a datagram socket for both sending and receiving data
            MyServerDatagramSocket mySocket = new MyServerDatagramSocket(DEFAULTSERVERPORT);
            System.out.println("File Management server ready.");

            while (true) {  // forever loop
                //Send & receive data
                DatagramMessage request = mySocket.receiveMessageAndSender();
                System.out.println("Request received");
                String message = request.getMessage();

                System.out.println("message received: " + message);
                // Decode message from 1, myUsername, myPassword etc.
                String[] splitMessage = message.split(",");
                messageCode = splitMessage[0];
                username = splitMessage[1];
                password = splitMessage[2];
                //Removing whitespace from message
                messageCode = messageCode.trim();
                username = username.trim();
                password = password.trim();

                //Determine which type of message & invoke different methods
                //1 Login,  2 Logout, 3 upload, 4 download, 5. register
                switch (messageCode) {
                    case "1":
                        System.out.println("Log in - server");
                        String loginResp = login(username, password);
                        mySocket.sendMessage(request.getAddress(), request.getPort(), loginResp);
                        break;
                    case "2":
                        System.out.println("Log Out - server");
                        String logoutResp = logout(username);
                        mySocket.sendMessage(request.getAddress(), request.getPort(), logoutResp);
                        break;
                    case "3":
                        System.out.println("Upload - server");
                        System.out.println("The message recieved from the client was: " + message);

                        String[] splitUploadMessage = message.split(",");
                        messageCode = splitUploadMessage[0];
                        messageCode = messageCode.trim();

                        username = splitUploadMessage[1];
                        username = username.trim();

                        fileName = splitUploadMessage[2];
                        fileName = fileName.trim();
                        try {
                            String fileContent = splitUploadMessage[3];
                            FileOutputStream fos = new FileOutputStream("C:\\Users\\exceeds\\Downloads\\FileManagementSystem-master\\DistributedComputingFileMgmtSystem\\users\\" + username + "\\" + fileName);
                            fos.write(fileContent.getBytes());
                            fos.close();
                            mySocket.sendMessage(request.getAddress(), request.getPort(), "800 File Uploaded successfully");
                        }catch (Exception ex){
                            mySocket.sendMessage(request.getAddress(), request.getPort(), "801 Error Uploading File");
                            ex.printStackTrace();
                        }
                        break;
                    case "4":
                        System.out.println("Download -server");
                        if(password.equals("getDirectory")){
                            System.out.println("Getting "  + username + "'s directory");
                            File[] files = new File("C:\\Users\\exceeds\\Downloads\\FileManagementSystem-master\\DistributedComputingFileMgmtSystem\\users\\"+username).listFiles();
                            System.out.println("\\users\\"+username);
                            List<String> listOfFiles = new ArrayList<String>();
                            for(File f:files){
                                System.out.println(f.getName());
                                listOfFiles.add(f.getName());
                            }
                            String response = "Getting Directory \\users\\"+username + ": \n" + listOfFiles.toString();
                            System.out.println(response);
                            mySocket.sendMessage(request.getAddress(), request.getPort(), response);
                        }
                        else {
                            System.out.println("Getting file");
                            String strPath = "C:\\Users\\exceeds\\Downloads\\FileManagementSystem-master\\DistributedComputingFileMgmtSystem\\users\\" + username+"\\"+password;
                            Path path = Paths.get(strPath);
                            byte[] data = Files.readAllBytes(path);
                            String byteDataString = new String(data);
                            mySocket.sendMessage(request.getAddress(), request.getPort(), byteDataString);
                        }
                        break;
                    case "5":
                        System.out.println("Create account - server");
                        String resp = createUser(username, password);
                        mySocket.sendMessage(request.getAddress(), request.getPort(), resp);
                        break;
                    default:
                        System.out.println("An error occured!");
                        resp = "00: An error occured on ther server try again";
                        mySocket.sendMessage(request.getAddress(), request.getPort(), resp);
                }
            } //end while
        } // end try
        catch (Exception ex) {
            ex.printStackTrace();
        } // end catch
    } //end main

    private static void populateUsers() {
        final String workingDir = System.getProperty("user.dir");
        final String usersFile = workingDir + "\\users\\Users.txt";
        System.out.println("current dir = " + workingDir);

        try {
            Scanner scanner = new Scanner(new File(usersFile));
            while (scanner.hasNextLine()) {

                String[] splitMessage = scanner.nextLine().split(",");
                User user = new User();
                user.setUsername(splitMessage[0].trim());
                user.setPassword(splitMessage[1].trim());

                listOfAllUsers.add(user);
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static String checkIfLoggedIn(String username){
       Boolean isLoggedIn =  LoggedInUsers.isLoggedIn(username);
        System.out.println(isLoggedIn);
        String loggedInResp = "default";
        if(isLoggedIn.equals(false)){
            loggedInResp = username + " is not logged in";
            return loggedInResp;
        }
        else if(isLoggedIn.equals(true)) {
            loggedInResp = username + " is logged in";
        }
        return loggedInResp;
    }
    public static String logout(String username){
        String logoutResp = LoggedInUsers.logOutUser(username);
        return logoutResp;
    }

    public static String login(String username, String password) {
        String serverResponse = "";
        /*BufferedReader br = null;
        BufferedReader bRead;
        FileReader fr = null;

        try {
            fr = new FileReader("");
            br = new BufferedReader(fr);
            InputStreamReader is = new InputStreamReader(System.in);
            bRead = new BufferedReader(is);
            String sCurrentLine;
            String uname = "", pass = "";
            while ((sCurrentLine = br.readLine()) != null) {
                System.out.println(sCurrentLine);
                String[] splitMessage = sCurrentLine.split(", ");
                uname = splitMessage[0];
                uname = uname.trim();
                pass = splitMessage[1];
                pass = pass.trim();
                //listOfAllUsers.add(new User((String) uname, (String) pass));
            }*/
        serverResponse = findUsers(username, password);
        System.out.println(serverResponse);
        return serverResponse;
    }

    public static String findUsers(String username, String password){
        String serverResponse = "501: Credentials entered incorrect/ user does not exist";
        for(User u: listOfAllUsers)
        {
            if(username.equals(u.getUsername()) &&  password.equals(u.getPassword()))
            {
                serverResponse =  "500: " + username + " found & logged in";
                LoggedInUsers.AddToList(new User(username, password));
                return serverResponse;
            }
        }
        return serverResponse;
    }

    public static String createUser(String username, String password) {
        BufferedWriter bw = null;
        FileWriter fw = null;
        //Set path & Create directory for each user in users/myName
        String path = "C:\\Users\\exceeds\\Downloads\\FileManagementSystem-master\\DistributedComputingFileMgmtSystem\\users\\";;
        File dir = new File(path+username);
        String serverMessage = "default mssg";
        //Check if directory exists
        if(!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println(dir.toString() + " has been created");
                try {
                    String message = username + ", " + password;
                    fw = new FileWriter(path+"Users.txt", true);
                    bw = new BufferedWriter(fw);
                    bw.write(message + "\n");
                    bw.append("");
                    System.out.println("Users were added to file");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (bw != null)
                            bw.close();
                        if (fw != null)
                            fw.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    serverMessage = "600: User Created: " + username;
                }
            } else {
                System.out.println("error occured");
                serverMessage = "601: Sorry an error occured - user may already exist or something went wrong";
            }
        }
        return serverMessage;
    }
}//end class


