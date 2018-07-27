package DatabaseConnect;

public class ConnectStrings {
	/**
     * 3306 is the default port for MySQL in XAMPP. Note both the 
     * MySQL server and Apache must be running. 
     */
    public static String url = "jdbc:mysql://localhost:3305/";

    /**
     * The MySQL user.
     */
    public static String user = "root";
    
    /**
     * Password for the above MySQL user. If no password has been 
     * set (as is the default for the root user in XAMPP's MySQL),
     * an empty string can be used.
     */
    public static String password = "";
}
