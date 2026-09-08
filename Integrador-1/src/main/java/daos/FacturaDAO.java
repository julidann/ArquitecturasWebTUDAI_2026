package daos;

import entities.Factura;
import factories.MySqlFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FacturaDAO implements DAO<Factura> {

    private static FacturaDAO unicaInstancia;

    private FacturaDAO() throws SQLException {

    }

    public static FacturaDAO getInstance() throws SQLException {
        if (unicaInstancia == null) {
            unicaInstancia = new FacturaDAO();
        }

        return unicaInstancia;
    }

    @Override
    public void dropTable() throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String drop_table = "DROP TABLE IF EXISTS Factura";

        // try-with-resources asegura que PreparedStatement y ResultSet se cierren automáticamente
        try (PreparedStatement ps = conn.prepareStatement(drop_table)) {
            ps.executeUpdate();

            conn.commit();
            conn.close();
        } catch (SQLException e) {
            conn.rollback(); // Rollback en caso de error
            throw new SQLException("Error al eliminar la tabla Factura.", e);
        }
    }

    @Override
    public void createTable() throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String table = "CREATE TABLE IF NOT EXISTS Factura(" +
                "idFactura INT," +
                "idCliente INT," +
                "PRIMARY KEY(idFactura))";

        // try-with-resources asegura que PreparedStatement y ResultSet se cierren automáticamente
        try (PreparedStatement ps = conn.prepareStatement(table)) {
            ps.executeUpdate();

            conn.commit();
            conn.close();
        } catch (SQLException e) {
            conn.rollback(); // Rollback en caso de error
            throw new SQLException("Error al crear la tabla Factura.", e);
        }

    }

    @Override
    public void insert(Factura factura) throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String query = "INSERT INTO Factura(idFactura, idCliente) VALUES (?, ?)";

        // try-with-resources asegura que PreparedStatement y ResultSet se cierren automáticamente
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, factura.getIdFactura());
            ps.setInt(2, factura.getIdCliente());
            ps.executeUpdate();

            conn.commit();
            conn.close();
        } catch (SQLException e) {
            conn.rollback(); // Rollback en caso de error
            throw new SQLException("Error al insertar Factura!", e);
        }
    }

    @Override
    public Factura select(int id) throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        Factura f = null;
        String query = "SELECT * FROM Factura WHERE idFactura=?";

        // try-with-resources asegura que PreparedStatement y ResultSet se cierren automáticamente
        try (PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            ps.setInt(1, id);

            f = new Factura(rs.getInt(1), rs.getInt(2));

            conn.close();
        } catch (SQLException e) {
            throw new SQLException("Error al seleccionar Cliente con id=" + id + "!", e);
        }

        return f;
    }

    @Override
    public List<Factura> selectAll() throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        List<Factura> facturas = new ArrayList<>();
        String query = "SELECT * FROM Factura";

        // try-with-resources asegura que PreparedStatement y ResultSet se cierren automáticamente
        try (PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                facturas.add(new Factura(rs.getInt(1), rs.getInt(2)));
            }

            conn.close();
        } catch (SQLException e) {
            throw new SQLException("Error al obtener Facturas!", e);
        }

        return facturas;
    }

    @Override
    public boolean update(Factura factura) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean delete(int id) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
