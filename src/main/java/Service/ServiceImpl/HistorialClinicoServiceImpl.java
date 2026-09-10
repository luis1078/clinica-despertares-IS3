package Service.ServiceImpl;

import Entity.CitaMedicaEntity;
import Entity.ComprobantePagoEntity;
import Entity.DetalleTratamientoEntity;
import Entity.DiagnosticoEntity;
import Entity.Emuns.EstadoPagoEnum;
import Entity.Emuns.EstadoTratamientoEnum;
import Entity.Emuns.TipoExamenEnum;
import Entity.ExamenLaboratorioEntity;
import Entity.ExamenMedicoEntity;
import Entity.HistoriaMedicaEntity;
import Entity.ImagenDiagnosticaEntity;
import Entity.MedicoEntity;
import Entity.PacienteEntity;
import Entity.TratamientoEntity;
import Service.IHistorialClinicoService;
import Service.IPacienteService;
import Service.dto.EventoHistorialDTO;
import Service.dto.HistorialClinicoDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Combina las distintas piezas del expediente de un paciente (citas,
 * diagnósticos con sus exámenes, tratamientos con sus medicamentos y
 * comprobantes de pago) en una única línea de tiempo ordenada por fecha
 * descendente, pensada para la vista de historial clínico de
 * {@code Controller.PacienteController}.
 *
 * Se apoya en las colecciones perezosas de {@link PacienteEntity} y
 * {@link HistoriaMedicaEntity} (igual que ya hacen las plantillas de
 * listados), así que debe ejecutarse dentro de una petición con la sesión
 * de Hibernate abierta.
 */
@Service
public class HistorialClinicoServiceImpl implements IHistorialClinicoService {

    private final IPacienteService pacienteService;

    public HistorialClinicoServiceImpl(IPacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @Override
    public HistorialClinicoDTO construirHistorial(String dniPaciente) {
        PacienteEntity paciente = pacienteService.buscarPorId(dniPaciente)
                .orElseThrow(() -> new IllegalArgumentException("No existe el paciente con DNI: " + dniPaciente));

        HistoriaMedicaEntity historia = paciente.getHistoriaMedica();
        List<EventoHistorialDTO> eventos = new ArrayList<>();

        List<CitaMedicaEntity> citas = paciente.getCitasMedicas() != null ? paciente.getCitasMedicas() : List.of();
        for (CitaMedicaEntity cita : citas) {
            eventos.add(eventoDeCita(cita));
        }

        List<DiagnosticoEntity> diagnosticos = historia != null && historia.getDiagnosticos() != null
                ? historia.getDiagnosticos() : List.of();
        for (DiagnosticoEntity diagnostico : diagnosticos) {
            eventos.add(eventoDeDiagnostico(diagnostico));
            List<ExamenMedicoEntity> examenes = diagnostico.getExamenMedicos() != null
                    ? diagnostico.getExamenMedicos() : List.of();
            for (ExamenMedicoEntity examen : examenes) {
                eventos.add(eventoDeExamen(examen));
            }
        }

        List<TratamientoEntity> tratamientos = historia != null && historia.getTratamientos() != null
                ? historia.getTratamientos() : List.of();
        for (TratamientoEntity tratamiento : tratamientos) {
            eventos.add(eventoDeTratamiento(tratamiento));
        }

        List<ComprobantePagoEntity> pagos = paciente.getComprobantePagos() != null
                ? paciente.getComprobantePagos() : List.of();
        for (ComprobantePagoEntity pago : pagos) {
            eventos.add(eventoDePago(pago));
        }

        eventos.sort(Comparator.comparing(EventoHistorialDTO::getFecha)
                .thenComparing(e -> e.getHora() != null ? e.getHora() : LocalTime.MIDNIGHT)
                .reversed());

        int totalTratamientosActivos = (int) tratamientos.stream()
                .filter(t -> t.getEstadoTratamiento() == EstadoTratamientoEnum.ACTIVO)
                .count();

        BigDecimal totalPagado = sumarMontos(pagos, EstadoPagoEnum.CANCELADO);
        BigDecimal totalPendiente = sumarMontos(pagos, EstadoPagoEnum.FALTA_PAGAR);

        return new HistorialClinicoDTO(paciente, historia, eventos, citas.size(), diagnosticos.size(),
                totalTratamientosActivos, totalPagado, totalPendiente);
    }

    private BigDecimal sumarMontos(List<ComprobantePagoEntity> pagos, EstadoPagoEnum estado) {
        return pagos.stream()
                .filter(pago -> pago.getEstado() == estado)
                .map(ComprobantePagoEntity::getMontoTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private EventoHistorialDTO eventoDeCita(CitaMedicaEntity cita) {
        MedicoEntity medico = cita.getMedico();
        String medicoTexto = medico != null
                ? "Dr(a). " + medico.getNombreMedico() + " " + medico.getApellidoMedico() + " · " + medico.getEspecialidad()
                : "Médico no asignado";
        String colorEstado = switch (cita.getEstadoCita()) {
            case FINALIZADA -> "verde";
            case CANCELADA -> "rojo";
            case PENDIENTE -> "ambar";
        };
        return new EventoHistorialDTO("CITA", cita.getFechaCita(), cita.getHoraCita(), "calendario",
                "Cita médica", medicoTexto + " — " + cita.getMotivoConsulta(),
                cita.getEstadoCita().getEtiqueta(), colorEstado, List.of());
    }

    private EventoHistorialDTO eventoDeDiagnostico(DiagnosticoEntity diagnostico) {
        String colorEstado = switch (diagnostico.getGravedadDiagnostico()) {
            case GRAVE -> "rojo";
            case MODERADO -> "ambar";
            case LEVE -> "verde";
        };
        return new EventoHistorialDTO("DIAGNOSTICO", diagnostico.getFechaDiagnostico(), null, "estetoscopio",
                "Diagnóstico", diagnostico.getDescripcionDiagnostico(),
                capitalizar(diagnostico.getGravedadDiagnostico().name()), colorEstado, List.of());
    }

    private EventoHistorialDTO eventoDeExamen(ExamenMedicoEntity examen) {
        String colorEstado = switch (examen.getEstadoExamenMedico()) {
            case FINALIZADO -> "verde";
            case EN_PROCESO -> "azul";
            case PENDIENTE -> "ambar";
        };
        boolean esLaboratorio = examen.getTipoExamen() == TipoExamenEnum.LABORATORIO;
        String titulo = esLaboratorio ? "Examen de laboratorio" : "Imagen diagnóstica";
        String icono = esLaboratorio ? "microscopio" : "imagen";

        List<String> detalles = new ArrayList<>();
        if (examen instanceof ExamenLaboratorioEntity laboratorio) {
            detalles.add("Muestra: " + laboratorio.getNombreMuestra());
            String referencia = laboratorio.getValorReferencia() != null
                    ? " (ref. " + laboratorio.getValorReferencia()
                        + (laboratorio.getUnidadMedida() != null ? " " + laboratorio.getUnidadMedida() : "") + ")"
                    : "";
            detalles.add("Resultado: " + laboratorio.getDescripcionResultado() + referencia);
        } else if (examen instanceof ImagenDiagnosticaEntity imagen) {
            detalles.add("Región: " + imagen.getRegionCuerpo());
            detalles.add("Informe: " + imagen.getInformeMedico());
            if (imagen.isUsaContraste()) {
                detalles.add("Contraste: " + imagen.getTipoContraste());
            }
        } else if (examen.getObservaciones() != null) {
            detalles.add(examen.getObservaciones());
        }

        String subtitulo = "Resultado " + (examen.getFechaResultado() != null
                ? "el " + examen.getFechaResultado() : "pendiente");
        return new EventoHistorialDTO("EXAMEN", examen.getFechaExamen(), null, icono, titulo, subtitulo,
                capitalizar(examen.getEstadoExamenMedico().name()), colorEstado, detalles);
    }

    private EventoHistorialDTO eventoDeTratamiento(TratamientoEntity tratamiento) {
        String colorEstado = switch (tratamiento.getEstadoTratamiento()) {
            case ACTIVO -> "azul";
            case FINALIZADO -> "verde";
            case SUSPENDIDO -> "rojo";
        };

        List<String> detalles = new ArrayList<>();
        List<DetalleTratamientoEntity> detalleTratamientos = tratamiento.getDetalleTratamientos();
        if (detalleTratamientos != null) {
            for (DetalleTratamientoEntity detalle : detalleTratamientos) {
                String nombreMedicamento = detalle.getMedicamento() != null
                        ? detalle.getMedicamento().getNombreMedicamento() : "Medicamento";
                detalles.add(nombreMedicamento + " — " + detalle.getDosisIndicada() + ", " + detalle.getFrecuenciaDeToma());
            }
        }

        String subtitulo = tratamiento.getTipoTratamiento() + " · hasta " + tratamiento.getFechaFin();
        return new EventoHistorialDTO("TRATAMIENTO", tratamiento.getFechaInicio(), null, "pastilla",
                tratamiento.getDescripcionTratamiento(), subtitulo,
                capitalizar(tratamiento.getEstadoTratamiento().name()), colorEstado, detalles);
    }

    private EventoHistorialDTO eventoDePago(ComprobantePagoEntity pago) {
        boolean pagado = pago.getEstado() == EstadoPagoEnum.CANCELADO;
        String subtitulo = pago.getTipoComprobante() + " · " + pago.getMetodoPago() + " · S/ " + pago.getMontoTotal();
        return new EventoHistorialDTO("PAGO", pago.getFechaEmision(), null, "recibo",
                "Comprobante de pago", subtitulo,
                pagado ? "Pagado" : "Pendiente", pagado ? "verde" : "rojo", List.of());
    }

    private String capitalizar(String texto) {
        String minuscula = texto.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(minuscula.charAt(0)) + minuscula.substring(1);
    }
}
