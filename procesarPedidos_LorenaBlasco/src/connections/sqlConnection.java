package connections;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class sqlConnection {

    static Properties properties = new Properties();

    public static Connection openConnection() throws SQLException {
        try {
            properties.load(new FileReader("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(properties.getProperty("URL"),properties.getProperty("user"),properties.getProperty("password"));
    }

    public static void closeConnection(Connection conexion) {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}


