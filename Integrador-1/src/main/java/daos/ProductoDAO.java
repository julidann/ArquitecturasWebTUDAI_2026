package daos;

import dtos.ProductoMayorRecaudacionDTO;
import entities.Producto;
import factories.MySqlFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO implements DAO<Producto> {
  private static ProductoDAO unicaInstancia;

    private ProductoDAO() throws SQLException {

    }

    public static ProductoDAO getInstance() throws SQLException {
        if (unicaInstancia == null) {
            unicaInstancia = new ProductoDAO();
        }

        return unicaInstancia;
    }

    @Override
    public void dropTable() throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String drop_table = "DROP TABLE IF EXISTS Producto";

        // try-with-resources asegura que PreparedStatement y ResultSet se cierren automáticamente
        try (PreparedStatement ps = conn.prepareStatement(drop_table)) {
            ps.executeUpdate();

            conn.commit();
            conn.close();
        } catch (SQLException e) {
            conn.rollback(); // Rollback en caso de error
            throw new SQLException("Error al eliminar la tabla Producto.", e);
        }
    }

    @Override
    public void createTable() throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String table = "CREATE TABLE IF NOT EXISTS Producto(" +
                "idProducto INT," +
                "nombre VARCHAR(45)," +
                "valor FLOAT," +
                "PRIMARY KEY(idProducto))";

        try (PreparedStatement ps = conn.prepareStatement(table)) {
            ps.executeUpdate();

            conn.commit();
            conn.close();
        } catch (SQLException e) {
            conn.rollback(); 
            throw new SQLException("Error al crear la tabla Producto.", e);
        }
    }

    @Override
    public void insert(Producto producto) throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String query = "INSERT INTO Producto(idProducto, nombre, valor) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, producto.getIdProducto());
            ps.setString(2, producto.getNombre());
            ps.setFloat(3, producto.getValor());
            ps.executeUpdate();

            conn.commit();
            conn.close();
        } catch (SQLException e) {
            conn.rollback(); 
            throw new SQLException("Error al insertar Producto!", e);
        }
    }

    @Override
    public Producto select(int id) throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        Producto p = null;
        String query = "SELECT * FROM Producto WHERE idProducto=?";

        try (PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            ps.setInt(1, id);

            p = new Producto(rs.getInt(1), rs.getString(2), rs.getFloat(3));

            conn.close();
        } catch (SQLException e) {
            throw new SQLException("Error al seleccionar Producto con id=" + id + "!", e);
        }

        return p;
    }

    @Override
    public List<Producto> selectAll() throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        List<Producto> productos = new ArrayList<>();
        String query = "SELECT * FROM Producto";

        try (PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(new Producto(rs.getInt(1), rs.getString(2), rs.getFloat(3)));
            }

            conn.close();
        } catch (SQLException e) {
            throw new SQLException("Error al obtener Productos!", e);
        }

        return productos;
    }

    @Override
    public boolean update(Producto producto) throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String query = "UPDATE Producto SET nombre = ?, valor = ? WHERE idProducto = ?";

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, producto.getNombre());
            ps.setDouble(2, producto.getValor());
            ps.setInt(3, producto.getIdProducto());

            int affectedRows = ps.executeUpdate();

            conn.commit();
            conn.close();

            return affectedRows > 0; 
        } catch (SQLException e) {
            conn.rollback(); 
            throw new SQLException("Error al actualizar Producto!", e);
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String query = "DELETE FROM Producto WHERE idProducto = ?";

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id);

            int affectedRows = ps.executeUpdate();

            conn.commit();
            conn.close();

            return affectedRows > 0; 
        } catch (SQLException e) {
            conn.rollback(); 
            throw new SQLException("Error al eliminar Producto con id=" + id, e);
        }
    }

    public ProductoMayorRecaudacionDTO selectMayorRecaudacion() throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        ProductoMayorRecaudacionDTO productoMayorRecaudacionDTO = null;
        String query = "SELECT p.idProducto, p.nombre, SUM(fp.cantidad * p.valor) AS Recaudacion ,p.valor FROM Producto p "
                + "JOIN Factura_Producto fp ON p.idProducto = fp.idProducto "
                + "GROUP BY p.idProducto, p.nombre, p.valor ORDER BY Recaudacion DESC LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                productoMayorRecaudacionDTO = new ProductoMayorRecaudacionDTO(rs.getInt(1), rs.getString(2), rs.getFloat(4), rs.getFloat(3));
            }
            conn.close();
        } catch (SQLException e) {
            throw new SQLException("Error al obtener ProductoMayorRecaudacion!", e);
        }
        return productoMayorRecaudacionDTO;
    }
}
