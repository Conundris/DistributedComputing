package sample;

import java.util.ArrayList;
import java.util.List;

public class LoggedInUsers {
    private static ArrayList<User> loggedInUsers = new ArrayList<>();
    public static  void AddToList(User user) {       //This method is called to populate the list
        loggedInUsers.add(user);
    }
    public static void getLoggedInUsers(){
        System.out.println("All the currently logged in users:");
        for(User u: loggedInUsers){
            System.out.println(u.getUsername());
        }
    }
    public static String logOutUser(String username){
        String serverResponse = "";
        int i = 0;
        for (User u : loggedInUsers) {
            if (u.getUsername().equals(username)) {
                System.out.println(i);
                loggedInUsers.remove(i);
                serverResponse = ResponseCode.USER_LOGGED_OUT_SERVICE_TERMINATED + ": " + username + " was logged out";
                System.out.println(serverResponse);
                return serverResponse;
            }
            i++;
        }
        serverResponse = "User not logged in";
            return serverResponse;
        }
        public static Boolean isLoggedIn(String username){
            for (User u : loggedInUsers) {
                if (u.getUsername().equals(username)) {
                    return true;
                }
            }
            return false;
        }
}//End class

