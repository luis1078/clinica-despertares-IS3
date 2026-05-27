package Service;

import Entity.ComprobantePagoEntity;
import Entity.Emuns.EstadoPagoEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IComprobantePagoService {

    List<ComprobantePagoEntity> listarTodos();

    Optional<ComprobantePagoEntity> buscarPorId(Long codComprobante);

    ComprobantePagoEntity guardar(ComprobantePagoEntity comprobantePago);

    void eliminar(Long codComprobante);

    List<ComprobantePagoEntity> listarPorPaciente(String dniPaciente);

    List<ComprobantePagoEntity> listarPorFechaEmision(LocalDate fechaEmision);

    List<ComprobantePagoEntity> listarPorEstado(EstadoPagoEnum estado);

    ComprobantePagoEntity registrarComprobante(String dniPaciente, ComprobantePagoEntity comprobantePago);

    ComprobantePagoEntity cancelarComprobante(Long codComprobante);

    ComprobantePagoEntity marcarComoFaltaPagar(Long codComprobante);
}
