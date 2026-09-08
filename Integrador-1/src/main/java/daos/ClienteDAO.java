package daos;

import dtos.ClienteConFacturacionDTO;
import dtos.ProductoMayorRecaudacionDTO;
import entities.Cliente;
import factories.MySqlFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO implements DAO<Cliente> {

    private static ClienteDAO unicaInstancia;

    private ClienteDAO() throws SQLException {

    }

    public static ClienteDAO getInstance() throws SQLException {
        if (unicaInstancia == null) {
            unicaInstancia = new ClienteDAO();
        }

        return unicaInstancia;
    }


    @Override
    public void dropTable() throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String drop_table = "DROP TABLE IF EXISTS Cliente";

        try (PreparedStatement ps = conn.prepareStatement(drop_table)) {
            ps.executeUpdate();

            conn.commit();
            conn.close();
        } catch (SQLException e) {
            conn.rollback();
            throw new SQLException("Error al eliminar la tabla Cliente.", e);
        }

    }

    @Override
    public void createTable() throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String table = "CREATE TABLE IF NOT EXISTS Cliente(" +
                "idCliente INT," +
                "nombre VARCHAR(500)," +
                "email VARCHAR(150)," +
                "PRIMARY KEY(idCliente))";

        try (PreparedStatement ps = conn.prepareStatement(table)) {
            ps.executeUpdate();

            conn.commit();
            conn.close();
        } catch (SQLException e) {
            conn.rollback();
            throw new SQLException("Error al crear la tabla Cliente.", e);
        }

    }

    @Override
    public void insert(Cliente cliente) throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String query = "INSERT INTO Cliente(idCliente, nombre, email) VALUES (?, ?, ?)";


        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, cliente.getIdCliente());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getEmail());
            ps.executeUpdate();

            conn.commit();
            conn.close();
        } catch (SQLException e) {
            conn.rollback();
            throw new SQLException("Error al insertar Cliente!", e);
        }

    }

    @Override
    public Cliente select(int id) throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        Cliente c = null;
        String query = "SELECT * FROM Cliente WHERE idCliente=?";


        try (PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            ps.setInt(1, id);

            c = new Cliente(rs.getInt(1), rs.getString(2), rs.getString(3));

            conn.close();
        } catch (SQLException e) {
            throw new SQLException("Error al seleccionar Cliente con id=" + id + "!", e);
        }

        return c;
    }

    @Override
    public List<Cliente> selectAll() throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        List<Cliente> clientes = new ArrayList<>();
        String query = "SELECT * FROM Cliente";


        try (PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(new Cliente(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }

            conn.close();
        } catch (SQLException e) {
            throw new SQLException("Error al obtener Clientes!", e);
        }

        return clientes;
    }

    @Override
    public boolean update(Cliente cliente) throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String query = "UPDATE Cliente SET nombre = ?, email = ? WHERE idCliente = ?";


        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getEmail());
            ps.setInt(3, cliente.getIdCliente());

            int affectedRows = ps.executeUpdate();

            conn.commit();
            conn.close();

            return affectedRows > 0;
        } catch (SQLException e) {
            conn.rollback();
            throw new SQLException("Error al actualizar Cliente!", e);
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        String query = "DELETE FROM Cliente WHERE idCliente = ?";


        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id);

            int affectedRows = ps.executeUpdate();

            conn.commit();
            conn.close();

            return affectedRows > 0;
        } catch (SQLException e) {
            conn.rollback();
            throw new SQLException("Error al eliminar Cliente con id=" + id, e);
        }
    }



    public List<ClienteConFacturacionDTO> clientesConMayorFacturacion() throws SQLException {
        Connection conn = MySqlFactory.getInstance().getConnection();

        List<ClienteConFacturacionDTO> clientesConMayorFacturacion = new ArrayList<>();
        String query = "SELECT c.idCliente, c.nombre, c.email, SUM(fp.cantidad * p.valor) as total_facturado " +
                "FROM Cliente c " +
                "INNER JOIN Factura f ON c.idCliente = f.idCliente " +
                "INNER JOIN Factura_Producto fp ON f.idFactura = fp.idFactura " +
                "INNER JOIN Producto p ON fp.idProducto = p.idProducto " +
                "GROUP BY c.idCliente, c.nombre, c.email " +
                "ORDER BY total_facturado DESC";

        try (PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ClienteConFacturacionDTO cliente = new ClienteConFacturacionDTO(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getFloat(4)
                );
                clientesConMayorFacturacion.add(cliente);
            }
        } catch (SQLException e) {
            throw new SQLException("Error al obtener clientes con mayor facturación!", e);
        }
        return clientesConMayorFacturacion;
    }


}