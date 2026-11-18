import java.io.*;
import java.util.*;

public class UserInterface {
    
    private Scanner scnr;
    private FileManager fileManager;

    public UserInterface(){
        Scanner scnr = new Scanner(System.in);
        System.out.println("Welcome to the Delivery Managment System.");    
         
    }

    public String LoginInterface(){
        System.out.println("Please enter your credentials below.");   
        System.out.println("Username: ");
        String username = scnr.nextLine();
        System.out.println("Password: ");
        String password = scnr.nextLine();

        return null;
    }



}
