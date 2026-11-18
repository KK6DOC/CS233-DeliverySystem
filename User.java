public class User {
    
    protected String username;
    protected String password;
    protected String email;

    public User(String username, String password, String email){
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public boolean Login(String username, String password){
        return true; 
    }

    public void Logout(){

    }

    protected String getUsername(){

    }




}
