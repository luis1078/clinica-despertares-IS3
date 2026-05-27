package Service;

import Entity.HistoriaMedicaEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IHistoriaMedicaService {

    List<HistoriaMedicaEntity> listarTodos();

    Optional<HistoriaMedicaEntity> buscarPorId(Long codHistoriaMedica);

    HistoriaMedicaEntity guardar(HistoriaMedicaEntity historiaMedica);

    void eliminar(Long codHistoriaMedica);

    Optional<HistoriaMedicaEntity> buscarPorDniPaciente(String dniPaciente);

    List<HistoriaMedicaEntity> listarPorFechaCreacion(LocalDate fechaCreacion);

    HistoriaMedicaEntity crearHistoriaMedica(String dniPaciente, HistoriaMedicaEntity historiaMedica);

    HistoriaMedicaEntity actualizarHistoriaMedica(Long codHistoriaMedica, HistoriaMedicaEntity datosActualizados);
}
