package Entity.Converter;

import Entity.Emuns.EstadoCitaEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class EstadoCitaConverter implements AttributeConverter<EstadoCitaEnum, String> {

    @Override
    public String convertToDatabaseColumn(EstadoCitaEnum estado) {
        return estado == null ? null : estado.getValorBaseDatos();
    }

    @Override
    public EstadoCitaEnum convertToEntityAttribute(String valor) {
        return EstadoCitaEnum.desdeValorBaseDatos(valor);
    }
}
