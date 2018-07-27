package DatabaseConnect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import lucene.Indexer;

public class ConnectDB {    
	/**
	 * Insert pairs of lucene search results
	 */
	public static void insertDB(int ID_Sub_A, String Link_A, int ID_Sub_B, String Link_B)
    {
        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            Connection con = DriverManager.getConnection(ConnectStrings.url, ConnectStrings.user, ConnectStrings.password);

            Statement stt = con.createStatement();

            /**
             * Create and select a database for use. 
             */
            stt.execute("CREATE DATABASE IF NOT EXISTS justcompare_db");
            stt.execute("USE justcompare_db");

            /**
             * Create an table
             */
            //stt.execute("CREATE TABLE IF NOT EXISTS pair_files");
            stt.execute("CREATE TABLE IF NOT EXISTS pair_files (" +
                    "id INT(10) NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "ID_Sub_A INT(10) NOT NULL,"
                    + "Link_A CHAR(255) NOT NULL,"
                    + "ID_Sub_B INT(10) NOT NULL,"
                    + "Link_B CHAR(255) NOT NULL,"
                    + "Link_html CHAR(255) DEFAULT NULL,"
                    + "Modified INT(10) DEFAULT 0,"
                    + "Percentage_Modified CHAR(255) DEFAULT NULL,"
                    + "Unmodified INT(10) DEFAULT 0,"
                    + "Percentage_Unmodified CHAR(255) DEFAULT NULL)");


            String insertq = "INSERT INTO pair_files (ID_Sub_A, Link_A, ID_Sub_B, Link_B, Link_html, Modified, Percentage_Modified, Unmodified, Percentage_Unmodified) "
            		+ "VALUES (?,?,?,?,null,0,null,0,null)";
            PreparedStatement prep = con.prepareStatement(insertq);
            prep.setInt(1, ID_Sub_A);
            prep.setString(2, Link_A);
            prep.setInt(3, ID_Sub_B);
            prep.setString(4, Link_B);
            
            prep.executeUpdate();

            /**
             * Free all opened resources
             */
            //res.close();
            stt.close();
            prep.close();
            con.close();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
	
	/**
	 * Delete pairs that map to updated or deleted submissions
	 */
	public static void deleteDB(int idToDelete) {

        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            Connection con = DriverManager.getConnection(ConnectStrings.url, ConnectStrings.user, ConnectStrings.password);

            Statement stt = con.createStatement();

            /**
             * Create and select a database for use. 
             */
            stt.execute("CREATE DATABASE IF NOT EXISTS justcompare_db");
            stt.execute("USE justcompare_db");
            ResultSet exists = stt.executeQuery("SHOW TABLES LIKE 'pair_files'");
            if(exists.first() == false) {
            	return;
            }
            

            /**
             * Delete old pairs to prepare for latest update submission 
             */
            String search = "SELECT * FROM 	pair_files WHERE ID_Sub_A = ? OR ID_Sub_B = ?";
            PreparedStatement prep = con.prepareStatement(search);
            prep.setInt(1, idToDelete);
            prep.setInt(2, idToDelete);

            ResultSet res = prep.executeQuery();
            if(res.first() != false) {
            	String query = "DELETE FROM pair_files WHERE ID_Sub_A = ? OR ID_Sub_B = ?";
                prep = con.prepareStatement(query);
                prep.setInt(1, idToDelete);
                prep.setInt(2, idToDelete);
                prep.executeUpdate();
                
                Indexer.deleteDoc(idToDelete);
                //System.out.println("clean up pair_files with submission id = " + idToDelete);
            }

            /**
             * Free all opened resources
             */
            //res.close();
            stt.close();
            prep.close();
            con.close();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}
	
	/**
	 * Return a list of submissions. Each element include submissionid and submission directory
	 * Make sure to split string by space first
	 */
	public static List<String> getDB() {
		List<String> ListSubmission = new ArrayList<String>();
		try
        {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            Connection con = DriverManager.getConnection(ConnectStrings.url, ConnectStrings.user, ConnectStrings.password);

            Statement stt = con.createStatement();
            
            /**
             * Create and select a database for use. 
             */
            stt.execute("CREATE DATABASE IF NOT EXISTS justcompare_db");
            stt.execute("USE justcompare_db");
            
	        /**
	         * Query submission entries with the compare status 0
	         */
	        ResultSet res = stt.executeQuery("SELECT * FROM submissions WHERE search_status = 0");
	
	        /**
	         * Iterate over the result set from the above query
	         */
	        while (res.next())
	        {
	        	String rowInfo = res.getInt("id") + " " + res.getString("filepath");
	            //.out.println(res.getInt("id") + " " + res.getString("filepath"));
	            
	            deleteDB(res.getInt("id"));
	            
	            ListSubmission.add(rowInfo);
	            //Mo doan nay truoc khi export
	            String query = "UPDATE submissions SET search_status = ? WHERE id = ? ";
	            PreparedStatement prep = con.prepareStatement(query);
	            prep.setInt(1, 1);
	            prep.setInt(2, res.getInt("id"));
	            
	            prep.executeUpdate();
	            prep.close();
	            
	        }
	        
	        res.close();
            stt.close();
            con.close();
        }
		
        catch (Exception e)
        {
            e.printStackTrace();
        }
		
		return ListSubmission;
	}
}
