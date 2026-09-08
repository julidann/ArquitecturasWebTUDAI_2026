package dtos;

public class ProductoMayorRecaudacionDTO {
    private int idProducto;
    private String nombre;
    private float valor;
    private float recaudacion;

    public ProductoMayorRecaudacionDTO(int idProducto, String nombre, float valor, float recaudacion) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.valor = valor;
        this.recaudacion = recaudacion;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public float getValor() {
        return valor;
    }

    public void setValor(float valor) {
        this.valor = valor;
    }

    public float getRecaudacion() {
        return recaudacion;
    }

    public void setRecaudacion(float recaudacion) {
        this.recaudacion = recaudacion;
    }

    @Override
    public String toString() {
        return "ProductoMayorRecaudacionDTO{" +
                "idProducto=" + idProducto +
                ", nombre='" + nombre + '\'' +
                ", valor=" + valor +
                ", recaudacion=" + recaudacion +
                '}';
    }
}
