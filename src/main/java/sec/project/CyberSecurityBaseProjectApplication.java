package sec.project;

import org.h2.tools.RunScript;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

@SpringBootApplication
public class CyberSecurityBaseProjectApplication {

    public static Connection connection;

    public static void main(String[] args) throws Throwable {

        // Open/Create connection to a database for admin logi cred.
        String databaseAddress = "jdbc:h2:file:./database";
        if (args.length > 0) {
            databaseAddress = args[0];
        }

        connection = DriverManager.getConnection(databaseAddress, "sa", "");

        try {
            // If database has not yet been created, insert content
            RunScript.execute(connection, new FileReader("sql/database-schema.sql"));
            RunScript.execute(connection, new FileReader("sql/database-import.sql"));
        } catch (Throwable t) {
            System.err.println(t.getMessage());
        }

        // Add the code that reads the Agents from the database
        // and prints them here
        // Execute query and retrieve the query results
        //ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM signups");

        // run spring
        SpringApplication.run(CyberSecurityBaseProjectApplication.class);


    }
}
