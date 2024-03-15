package ua.kiev.splash.mathhelper.db;

import ua.kiev.splash.mathhelper.exceptions.UnexpectedException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
    public static Connection connect() throws SQLException {
        String jdbcUrl = DatabaseConfig.getDbUrl();
        String user = DatabaseConfig.getDbUsername();
        String password = DatabaseConfig.getDbPassword();
        Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
        connection.setAutoCommit(true); // default
        return connection;
    }
}
