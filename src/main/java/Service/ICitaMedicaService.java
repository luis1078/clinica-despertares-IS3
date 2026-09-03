package Service;

import Entity.CitaMedicaEntity;
import Entity.Emuns.EstadoCitaEnum;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ICitaMedicaService {

    List<CitaMedicaEntity> listarTodos();

    Optional<CitaMedicaEntity> buscarPorId(Long codCitaMedica);

    CitaMedicaEntity guardar(CitaMedicaEntity citaMedica);

    void eliminar(Long codCitaMedica);

    List<CitaMedicaEntity> buscarCitas(String texto, EstadoCitaEnum estadoCita);

    List<CitaMedicaEntity> listarPorFecha(LocalDate fechaCita);

    List<CitaMedicaEntity> listarPorMedico(Long idMedico);

    List<CitaMedicaEntity> listarPorPaciente(String dniPaciente);

    List<CitaMedicaEntity> listarPorEstado(EstadoCitaEnum estadoCita);

    CitaMedicaEntity registrarCitaMedica(String dniPaciente, Long idMedico, CitaMedicaEntity citaMedica);

    CitaMedicaEntity cancelarCitaMedica(Long codCitaMedica);

    CitaMedicaEntity finalizarCitaMedica(Long codCitaMedica);

    CitaMedicaEntity reprogramarCitaMedica(Long codCitaMedica, LocalDate nuevaFecha, LocalTime nuevaHora);

    boolean existeDisponibilidadMedico(Long idMedico, LocalDate fechaCita, LocalTime horaCita);
}
