package daos;

import java.sql.SQLException;
import java.util.List;

public interface DAO<T> {

    void dropTable() throws SQLException;

    void createTable() throws SQLException;

    void insert(T t) throws SQLException;

    T select(int id) throws SQLException;

    List<T> selectAll() throws SQLException;

    boolean update(T t) throws SQLException;

    boolean delete(int id) throws SQLException;
}