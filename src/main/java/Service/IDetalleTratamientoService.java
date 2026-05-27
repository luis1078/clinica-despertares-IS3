package Service;

import Entity.DetalleTratamientoEntity;

import java.util.List;
import java.util.Optional;

public interface IDetalleTratamientoService {

    List<DetalleTratamientoEntity> listarTodos();

    Optional<DetalleTratamientoEntity> buscarPorId(Long idDetalleTratamiento);

    DetalleTratamientoEntity guardar(DetalleTratamientoEntity detalleTratamiento);

    void eliminar(Long idDetalleTratamiento);

    List<DetalleTratamientoEntity> listarPorTratamiento(Long idTratamiento);

    List<DetalleTratamientoEntity> listarPorMedicamento(Long codMedicamento);

    DetalleTratamientoEntity registrarDetalleTratamiento(Long idTratamiento, Long codMedicamento, DetalleTratamientoEntity detalleTratamiento);
}
