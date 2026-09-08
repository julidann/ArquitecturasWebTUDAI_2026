package readers;


import daos.DAO;
import entities.Cliente;
import entities.Factura;
import entities.FacturaProducto;
import entities.Producto;
import factories.DAOFactory;

import java.sql.SQLException;
import java.util.List;

public class DatabaseLoader {
    public static void cargarDatos(CSVReader reader) throws SQLException {
        List<Cliente> clientes = reader.leerArchivoClientes();
        List<Factura> facturas = reader.leerArchivoFacturas();
        List<Producto> productos = reader.leerArchivoProductos();
        List<FacturaProducto> facturasProductos = reader.leerArchivoFacturasProductos();

        DAOFactory dbF = DAOFactory.getFactory(1);
        DAO<Cliente> clienteDAO = dbF.getClienteDAO();
        DAO<Producto> productoDAO = dbF.getProductoDAO();
        DAO<FacturaProducto> facturaProductoDAO = dbF.getFacturaProductoDAO();
        DAO<Factura> facturaDAO = dbF.getFacturaDAO();

        facturaProductoDAO.dropTable();
        facturaDAO.dropTable();
        productoDAO.dropTable();
        clienteDAO.dropTable();

        clienteDAO.createTable();
        productoDAO.createTable();
        facturaDAO.createTable();
        facturaProductoDAO.createTable();

        cargarListaEnBaseDeDatos(clientes, clienteDAO);
        cargarListaEnBaseDeDatos(facturas, facturaDAO);
        cargarListaEnBaseDeDatos(productos, productoDAO);
        cargarListaEnBaseDeDatos(facturasProductos, facturaProductoDAO);
    }

    public static <T> void cargarListaEnBaseDeDatos(List<T> lista, DAO<T> dao) throws SQLException {
        for (T entidad : lista) {
            dao.insert(entidad);
        }
    }
}
