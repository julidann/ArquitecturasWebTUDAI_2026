package factories;

import daos.ClienteDAO;
import daos.FacturaDAO;
import daos.FacturaProductoDAO;
import daos.ProductoDAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlFactory extends DAOFactory {
    private final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private final String DB_URI = "jdbc:mysql://localhost:3306/integrador_1?createDatabaseIfNotExist=true";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "password";

    private static MySqlFactory unicaInstancia = null;
    private Connection conn;

    private MySqlFactory() {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    public static MySqlFactory getInstance() {
        if (unicaInstancia == null) {
            unicaInstancia = new MySqlFactory();
        }
        return unicaInstancia;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(DB_URI, DB_USER, DB_PASSWORD);
                conn.setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new SQLException(e);
        }

        return conn;
    }

    @Override
    public void closeConnection() throws SQLException {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public ClienteDAO getClienteDAO() throws SQLException {
        return ClienteDAO.getInstance();
    }

    @Override
    public FacturaDAO getFacturaDAO() throws SQLException {
        return FacturaDAO.getInstance();
    }

    @Override
    public FacturaProductoDAO getFacturaProductoDAO() throws SQLException {
        return FacturaProductoDAO.getInstance();
    }

    @Override
    public ProductoDAO getProductoDAO() throws SQLException {
        return ProductoDAO.getInstance();
    }
  
}
