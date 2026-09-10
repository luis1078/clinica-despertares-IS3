package Service.dto;

import Entity.HistoriaMedicaEntity;
import Entity.PacienteEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vista consolidada del historial clínico de un paciente: sus datos, su
 * historia médica (si ya la tiene) y la línea de tiempo de eventos, más
 * algunos totales para el encabezado de la pantalla.
 */
@Getter
@Setter
@AllArgsConstructor
public class HistorialClinicoDTO {

    private PacienteEntity paciente;
    /** Puede ser null: el paciente todavía podría no tener historia médica creada. */
    private HistoriaMedicaEntity historiaMedica;
    private List<EventoHistorialDTO> eventos;

    private int totalCitas;
    private int totalDiagnosticos;
    private int totalTratamientosActivos;
    private BigDecimal totalPagado;
    private BigDecimal totalPendiente;
}
