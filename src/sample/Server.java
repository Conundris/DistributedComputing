package sample;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
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
    private static String keystoreFile = "fms.jks";
    private static String keyStorePwd = "ittralee";

    /*
     * The following is to set up the keystores.
     */
    private static final String pathToStores = "C:\\";
    private static final String keyStoreFile = "nanithefuck.jks";
    private static final String trustStoreFile = "public.jks";
    private static final String passwd = "ittralee";

    private static final String keyFilename =
            pathToStores + "\\" + keyStoreFile;
    private static final String trustFilename =
            pathToStores + "\\" + trustStoreFile;

    public static void main(String[] args) throws KeyStoreException, IOException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
        String messageCode;
        String username;
        String password;
        String fileName;
        String outputPath;

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        char[] passphrase = passwd.toCharArray();

        try (FileInputStream fis = new FileInputStream(keyFilename)) {
            ks.load(fis, passphrase);
        }

        try (FileInputStream fis = new FileInputStream(trustFilename)) {
            ts.load(fis, passphrase);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("DTLS");

        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        SSLEngine engine = sslCtx.createSSLEngine("localhost", DEFAULTSERVERPORT);
        engine.setUseClientMode(false);

        // Read all Users from File and create their folders on server if needed
        populateUsers();

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

                String[] splitMessage = message.split("§");

                messageCode = splitMessage[0];
                username = splitMessage[1];
                //Removing whitespace from message
                messageCode = messageCode.trim();
                username = username.trim();

                //Determine which type of message & invoke different methods
                switch (messageCode) {
                    case "600":
                        password = splitMessage[2];
                        //Removing whitespace from message
                        password = password.trim();

                        System.out.println("Log in - server");
                        String loginResp = login(username, password);
                        mySocket.sendMessage(request.getAddress(), request.getPort(), loginResp);
                        break;
                    case "2":
                        System.out.println("Log Out - server");
                        String logoutResp = logout(username);
                        mySocket.sendMessage(request.getAddress(), request.getPort(), logoutResp);
                        break;
                    case "111":
                        System.out.println("Upload - server");
                        System.out.println("The message recieved from the client was: " + message);

                        fileName = splitMessage[2];
                        fileName = fileName.trim();
                        try {
                            String fileContent = splitMessage[3];
                            fileContent = fileContent.trim();
                            byte[] decodedBytes = Base64.getDecoder().decode(fileContent);
                            String decodedString = new String(decodedBytes);

                            FileOutputStream fos = new FileOutputStream(DEFAULTFOLDERPATH + "\\" + username + "\\" + fileName);
                            fos.write(decodedBytes);
                            fos.close();
                            mySocket.sendMessage(request.getAddress(), request.getPort(),  ResponseCode.CLOSING_DATA_CONNECTION + ": File Uploaded successfully");
                        }catch (Exception ex){
                            mySocket.sendMessage(request.getAddress(), request.getPort(), ResponseCode.CANT_OPEN_DATA_CONNECTION + ": Error Uploading File");
                            ex.printStackTrace();
                        }
                        break;
                    case "200":

                        List<String> userFiles = getUserFiles(username);

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        ObjectOutputStream outputStream = new ObjectOutputStream(out);
                        outputStream.writeObject(userFiles);
                        outputStream.close();

                        mySocket.sendMessage(request.getAddress(), request.getPort(), out.toByteArray());

                        break;
                    case "4":
                        System.out.println("Download to Client");

                        fileName = splitMessage[2];

                        System.out.println("Getting file");
                        String strPath = "C:\\Users\\exceeds\\Downloads\\FileManagementSystem-master\\DistributedComputingFileMgmtSystem\\users\\" + username+"\\" + fileName;
                        Path path = Paths.get(strPath);
                        byte[] data = Files.readAllBytes(path);

                        String encodedString = Base64.getEncoder().encodeToString(Files.readAllBytes(path));

                        String byteDataString = new String(data);
                        mySocket.sendMessage(request.getAddress(), request.getPort(), encodedString);
                        break;
                    default:
                        System.out.println("An error occured!");
                        String resp = "00: An error occured on ther server try again";
                        mySocket.sendMessage(request.getAddress(), request.getPort(), resp);
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
                serverResponse = ResponseCode.USER_LOGGED_IN_PROCEED + ": " + username + " found & logged in";
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


