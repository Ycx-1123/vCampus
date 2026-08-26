package vCampus.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
	private static final String DB_URL = "jdbc:ucanaccess://vCampus.accdb";
	
	public static Connection getConnection() throws SQLException{
		return DriverManager.getConnection(DB_URL);
	}
	
	public static void close(Connection conn) {
		//连结不为空的时候关闭
		if(conn != null) {
			try {
				conn.close();
			} catch(SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
