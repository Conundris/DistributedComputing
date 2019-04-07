package sample;

import sample.SSL.DTLSServer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
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
@SuppressWarnings("Duplicates")
public class Server {
    private static List<User> listOfAllUsers = new ArrayList<>();
    private final static String DEFAULTFOLDERPATH = System.getProperty("user.home") + "\\FileManagementServer";
    private final static int DEFAULTSERVERPORT = 3000;

    public static void main(String[] args) {
        Short requestCode = 999;
        String messageCode;
        String username;
        String password;
        String fileName;
        String encodedString;



        // Read all Users from File and create their folders on server if needed
        populateUsers();

        try {
            DTLSServer server = new DTLSServer();
            // instantiates a datagram socket for both sending and receiving data
            //MyServerDatagramSocket mySocket = new MyServerDatagramSocket(DEFAULTSERVERPORT);
            System.out.println("File Management server ready.");

            while (true) {  // forever loop
                //Send & receive data
                DatagramMessage request = server.receive("localhost", 3001);
                System.out.println("Request received");
                String message = request.getMessage();
                System.out.println("message received: " + request);
                // Example Message for Login: 600§admin§password
                String[] splitMessage = message.split("§");
                //Removing whitespace from message
                messageCode = splitMessage[0].trim();
                username = splitMessage[1].trim();

                try {
                    requestCode = Short.parseShort(messageCode.trim());
                } catch (NumberFormatException nfe) {
                    System.out.println("Invalid request code found.");
                }

                //Determine which type of message & invoke different methods
                switch (requestCode) {
                    case RequestCode.LOGIN:
                        password = splitMessage[2];
                        //Removing whitespace from message
                        password = password.trim();

                        System.out.println("Log in - server");
                        String loginResp = login(username, password);
                        server.sendMessage(server.getEngine(), request.getAddress(), request.getPort(), loginResp);
                        break;
                    case RequestCode.LOGOUT:
                        System.out.println("Log Out - server");
                        String logoutResp = logout(username);
                        server.sendMessage(server.getEngine(), request.getAddress(), request.getPort(), logoutResp);
                        break;
                    case RequestCode.UPL:
                        System.out.println("Upload - server");
                        System.out.println("The message recieved from the client was: " + request);

                        fileName = splitMessage[2];
                        fileName = fileName.trim();
                        try {
                            String fileContent = splitMessage[3];
                            fileContent = fileContent.trim();
                            byte[] decodedBytes = Base64.getDecoder().decode(fileContent);

                            FileOutputStream fos = new FileOutputStream(DEFAULTFOLDERPATH + "\\" + username + "\\" + fileName);
                            fos.write(decodedBytes);
                            fos.close();
                            server.sendMessage(server.getEngine(), request.getAddress(), request.getPort(), ResponseCode.UPLOAD_SUCCESSFUL + ": File Uploaded successfully");
                        } catch (Exception ex){
                            server.sendMessage(server.getEngine(), request.getAddress(), request.getPort(), ResponseCode.UPLOAD_NOT_SUCCESSFUL+ ": Error Uploading File");
                            ex.printStackTrace();
                        }
                        break;
                    case RequestCode.DDLIST:

                        try {
                            List<String> userFiles = getUserFiles(username);

                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            ObjectOutputStream outputStream = new ObjectOutputStream(out);
                            outputStream.writeObject(userFiles);
                            outputStream.close();

                            encodedString = Base64.getEncoder().encodeToString(out.toByteArray());

                            server.sendMessage(server.getEngine(), request.getAddress(), request.getPort(),  ResponseCode.USER_FILES_LISTED + ": " + encodedString);
                        } catch(Exception e) {
                            server.sendMessage(server.getEngine(), request.getAddress(), request.getPort(), String.valueOf(ResponseCode.USER_FILES_ERROR));
                        }
                        break;
                    case RequestCode.DDL:
                        System.out.println("Download to Client");

                        try {
                            fileName = splitMessage[2];
                            fileName = fileName.trim();

                            System.out.println("Getting file: " + fileName);
                            Path path = Paths.get(getUserFolder(username) + "\\" + fileName);

                            encodedString = Base64.getEncoder().encodeToString(Files.readAllBytes(path));

                            server.sendMessage(server.getEngine(), request.getAddress(), request.getPort(), ResponseCode.DOWNLOAD_SUCCESSFUL + ": " + encodedString);
                        } catch(Exception e) {
                            server.sendMessage(server.getEngine(), request.getAddress(), request.getPort(), String.valueOf(ResponseCode.DOWNLOAD_UNSUCCESSFUL));
                        }
                        break;
                    default:
                        System.out.println("An error occured!");
                        String resp = ResponseCode.COMMAND_UNRECOGNIZED + ": An error occured on the server try again";
                        server.sendMessage(server.getEngine(), request.getAddress(), request.getPort(), resp);
                }
            } //end while
        } // end try
        catch (Exception ex) {
            ex.printStackTrace();
        } // end catch
    } //end main

    private static List<String> getUserFiles(String username) {
        File[] listOfFiles = getUserFolder(username).listFiles();

        ArrayList<String> listOfUserFiles = new ArrayList<>();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) listOfUserFiles.add(listOfFiles[i].getName());
        }

        return listOfUserFiles;
    }

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

                checkIfFolderExists(user);

                listOfAllUsers.add(user);
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static String logout(String username){
        String logoutResp = LoggedInUsers.logOutUser(username);
        return logoutResp;
    }

    public static String login(String username, String password) {
        String serverResponse;
        serverResponse = findUser(username, password);
        System.out.println(serverResponse);
        return serverResponse;
    }

    public static String findUser(String username, String password){
        String serverResponse =  ResponseCode.USER_LOGIN_NOT_SUCCUESSFUL + ": Credentials entered incorrect/ user does not exist";
        for(User u: listOfAllUsers)
        {
            if(username.equals(u.getUsername()) &&  password.equals(u.getPassword()))
            {
                serverResponse = ResponseCode.USER_LOGGED_IN_PROCEED + ": " + username + " logged in";
                LoggedInUsers.AddToList(new User(username, password));
                return serverResponse;
            }
        }
        return serverResponse;
    }

    private static File getUserFolder(String username) {
        return new File(DEFAULTFOLDERPATH + "\\" + username);
    }

    private static void checkIfFolderExists(User user) {
        File baseServerFolder = new File(DEFAULTFOLDERPATH);
        File userServerFolder = getUserFolder(user.getUsername());

        if(!baseServerFolder.exists()) {
            baseServerFolder.mkdir();
        }

        if(!userServerFolder.exists()) {
            userServerFolder.mkdir();
        }
    }
}


