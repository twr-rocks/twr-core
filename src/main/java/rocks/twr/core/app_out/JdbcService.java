package rocks.twr.core.app_out;

import java.sql.*;

public class JdbcService {

    private final Connection conn;

    JdbcService(String driver, String url, String username, String password) throws ClassNotFoundException, SQLException {
        // TODO use sysprops to determine which to load
        // TODO erm... connection pooling?? or use hibernate api?
        Class.forName(driver);
        conn = DriverManager.getConnection(url, username, password);
    }

    // TODO use callbacks to close this
    public ResultSet executeQuery(String sql) throws SQLException {
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeQuery();
        }
    }
    public int executeUpdate(String sql) throws SQLException {
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }
}
