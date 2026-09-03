package Entity.Emuns;

public enum EstadoCitaEnum {
    PENDIENTE("Pendiente"),
    CANCELADA("Cancelada"),
    FINALIZADA("Finalizada");

    private final String valorBaseDatos;

    EstadoCitaEnum(String valorBaseDatos) {
        this.valorBaseDatos = valorBaseDatos;
    }

    public String getValorBaseDatos() {
        return valorBaseDatos;
    }

    public String getEtiqueta() {
        return valorBaseDatos;
    }

    public static EstadoCitaEnum desdeValorBaseDatos(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        for (EstadoCitaEnum estado : values()) {
            if (estado.name().equalsIgnoreCase(valor) || estado.valorBaseDatos.equalsIgnoreCase(valor)) {
                return estado;
            }
        }

        throw new IllegalArgumentException("Estado de cita no válido: " + valor);
    }
}
