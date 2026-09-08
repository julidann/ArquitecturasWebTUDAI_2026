
import daos.ClienteDAO;
import daos.ProductoDAO;
import dtos.ClienteConFacturacionDTO;
import dtos.ProductoMayorRecaudacionDTO;
import factories.DAOFactory;
import readers.CSVReader;
import readers.DatabaseLoader;
import readers.CSVReader;

import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        CSVReader reader = new CSVReader();

        try {
            // 1 y 2. Crea las tablas, lee los CSV y carga los datos respetando las Foreign Keys
            DatabaseLoader.cargarDatos(reader);

            // Obtenemos la Factory de MySQL para instanciar los DAOs
            DAOFactory dbFactory = DAOFactory.getFactory(DAOFactory.MYSQL_JDBC);
            ProductoDAO productoDAO = dbFactory.getProductoDAO();
            ClienteDAO clienteDAO = dbFactory.getClienteDAO();

            // 3. Producto que más recaudó
            System.out.println("\n--- PUNTO 3: PRODUCTO MÁS RECAUDADOR ---");
            ProductoMayorRecaudacionDTO prodRecaudador = productoDAO.selectMayorRecaudacion();
            System.out.println(prodRecaudador);

            // 4. Clientes ordenados por facturación
            System.out.println("\n--- PUNTO 4: CLIENTES ORDENADOS POR FACTURACIÓN ---");
            List<ClienteConFacturacionDTO> clientesFacturacion = clienteDAO.clientesConMayorFacturacion();
            for (ClienteConFacturacionDTO c : clientesFacturacion) {
                System.out.println(c);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
