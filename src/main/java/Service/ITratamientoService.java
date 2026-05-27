package Service;

import Entity.Emuns.EstadoTratamientoEnum;
import Entity.TratamientoEntity;

import java.util.List;
import java.util.Optional;

public interface ITratamientoService {

    List<TratamientoEntity> listarTodos();

    Optional<TratamientoEntity> buscarPorId(Long idTratamiento);

    TratamientoEntity guardar(TratamientoEntity tratamiento);

    void eliminar(Long idTratamiento);

    List<TratamientoEntity> listarPorTipo(String tipoTratamiento);

    List<TratamientoEntity> listarPorEstado(EstadoTratamientoEnum estadoTratamiento);

    List<TratamientoEntity> listarPorHistoriaMedica(Long codHistoriaMedica);

    TratamientoEntity registrarTratamiento(Long codHistoriaMedica, TratamientoEntity tratamiento);

    TratamientoEntity finalizarTratamiento(Long idTratamiento);

    TratamientoEntity suspenderTratamiento(Long idTratamiento);
}
