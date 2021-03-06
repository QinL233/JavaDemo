package jdbc.util;

import java.sql.*;

public class JdbcUtils {
    // 可以把几个字符串定义成常量：用户名，密码，URL，驱动类
    private static final String DRIVER = PropertiesUtils.getDB().getProperty("driver");
    private static final String URL = PropertiesUtils.getDB().getProperty("url");
    private static final String USER = PropertiesUtils.getDB().getProperty("user");
    private static final String PWD = PropertiesUtils.getDB().getProperty("password");

    /**
     * 注册驱动(可以省略)
     */
    static {
        try {
            Class.forName(DRIVER); }
        catch (ClassNotFoundException e) { e.printStackTrace(); }
    }

    /**
     * 得到数据库的连接
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PWD);
    }

    /**
     * 关闭所有打开的资源
     */
    public static void close(Connection conn, Statement stmt){
        if(stmt != null) {
            try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        if(conn != null) {
            try { conn.close(); }catch (SQLException e) { e.printStackTrace(); }
        }

    }

    /**
     * 关闭所有打开的资源 重载
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        if(rs != null) {
            try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
        }

        close(conn, stmt);
    }
}