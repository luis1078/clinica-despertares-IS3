package Service.ServiceImpl;

import Entity.CitaMedicaEntity;
import Entity.ComprobantePagoEntity;
import Entity.DetalleTratamientoEntity;
import Entity.DiagnosticoEntity;
import Entity.Emuns.EstadoCitaEnum;
import Entity.Emuns.EstadoExamenMedicoEnum;
import Entity.Emuns.EstadoPagoEnum;
import Entity.Emuns.EstadoTratamientoEnum;
import Entity.Emuns.GravedadDiagnosticoEnum;
import Entity.Emuns.MetodoPagoEnum;
import Entity.Emuns.TipoComprobanteEnum;
import Entity.ExamenLaboratorioEntity;
import Entity.HistoriaMedicaEntity;
import Entity.MedicamentoEntity;
import Entity.MedicoEntity;
import Entity.PacienteEntity;
import Entity.TratamientoEntity;
import Service.IPacienteService;
import Service.dto.EventoHistorialDTO;
import Service.dto.HistorialClinicoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Cubre el ensamblado de la línea de tiempo del historial clínico: que
 * combine citas, diagnósticos/exámenes, tratamientos y pagos en orden
 * cronológico descendente, que funcione sin historia médica creada y que
 * calcule bien los totales de pagos/tratamientos activos.
 */
@ExtendWith(MockitoExtension.class)
class HistorialClinicoServiceImplTest {

    @Mock
    private IPacienteService pacienteService;

    @InjectMocks
    private HistorialClinicoServiceImpl historialClinicoService;

    private PacienteEntity paciente;

    @BeforeEach
    void setUp() {
        paciente = new PacienteEntity();
        paciente.setDnipaciente("12345678");
        paciente.setNombrePaciente("Ana");
        paciente.setApellidoPaciente("Torres");
    }

    @Test
    void construirHistorial_pacienteNoExiste_lanzaIllegalArgumentException() {
        when(pacienteService.buscarPorId("99999999")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> historialClinicoService.construirHistorial("99999999"));

        assertTrue(ex.getMessage().contains("99999999"));
    }

    @Test
    void construirHistorial_sinHistoriaMedica_soloIncluyeCitasYPagosSinFallar() {
        CitaMedicaEntity cita = citaDe(LocalDate.of(2026, 1, 10), EstadoCitaEnum.PENDIENTE);
        paciente.setCitasMedicas(List.of(cita));
        paciente.setComprobantePagos(List.of());
        paciente.setHistoriaMedica(null);

        when(pacienteService.buscarPorId("12345678")).thenReturn(Optional.of(paciente));

        HistorialClinicoDTO historial = historialClinicoService.construirHistorial("12345678");

        assertEquals(1, historial.getEventos().size());
        assertEquals("CITA", historial.getEventos().get(0).getTipo());
        assertEquals(0, historial.getTotalDiagnosticos());
        assertEquals(0, historial.getTotalTratamientosActivos());
    }

    @Test
    void construirHistorial_combinaTodasLasFuentesYOrdenaPorFechaDescendente() {
        CitaMedicaEntity cita = citaDe(LocalDate.of(2026, 1, 5), EstadoCitaEnum.FINALIZADA);

        ExamenLaboratorioEntity examen = new ExamenLaboratorioEntity();
        examen.setFechaExamen(LocalDate.of(2026, 1, 15));
        examen.setEstadoExamenMedico(EstadoExamenMedicoEnum.FINALIZADO);
        examen.setNombreMuestra("Sangre");
        examen.setDescripcionResultado("Normal");

        DiagnosticoEntity diagnostico = new DiagnosticoEntity();
        diagnostico.setFechaDiagnostico(LocalDate.of(2026, 1, 12));
        diagnostico.setDescripcionDiagnostico("Gripe estacional");
        diagnostico.setGravedadDiagnostico(GravedadDiagnosticoEnum.LEVE);
        diagnostico.setExamenMedicos(List.of(examen));

        MedicamentoEntity medicamento = new MedicamentoEntity();
        medicamento.setNombreMedicamento("Paracetamol");

        DetalleTratamientoEntity detalle = new DetalleTratamientoEntity();
        detalle.setMedicamento(medicamento);
        detalle.setDosisIndicada("500mg");
        detalle.setFrecuenciaDeToma("Cada 8 horas");

        TratamientoEntity tratamiento = new TratamientoEntity();
        tratamiento.setFechaInicio(LocalDate.of(2026, 1, 20));
        tratamiento.setFechaFin(LocalDate.of(2026, 1, 27));
        tratamiento.setDescripcionTratamiento("Reposo y medicación");
        tratamiento.setTipoTratamiento("Ambulatorio");
        tratamiento.setEstadoTratamiento(EstadoTratamientoEnum.ACTIVO);
        tratamiento.setDetalleTratamientos(List.of(detalle));

        HistoriaMedicaEntity historiaMedica = new HistoriaMedicaEntity();
        historiaMedica.setDiagnosticos(List.of(diagnostico));
        historiaMedica.setTratamientos(List.of(tratamiento));

        ComprobantePagoEntity pagoCancelado = pagoDe(LocalDate.of(2026, 1, 6), EstadoPagoEnum.CANCELADO, new BigDecimal("100.00"));
        ComprobantePagoEntity pagoPendiente = pagoDe(LocalDate.of(2026, 1, 28), EstadoPagoEnum.FALTA_PAGAR, new BigDecimal("50.00"));

        paciente.setCitasMedicas(List.of(cita));
        paciente.setHistoriaMedica(historiaMedica);
        paciente.setComprobantePagos(List.of(pagoCancelado, pagoPendiente));

        when(pacienteService.buscarPorId("12345678")).thenReturn(Optional.of(paciente));

        HistorialClinicoDTO historial = historialClinicoService.construirHistorial("12345678");

        assertEquals(6, historial.getEventos().size());
        assertEquals(1, historial.getTotalCitas());
        assertEquals(1, historial.getTotalDiagnosticos());
        assertEquals(1, historial.getTotalTratamientosActivos());
        assertEquals(new BigDecimal("100.00"), historial.getTotalPagado());
        assertEquals(new BigDecimal("50.00"), historial.getTotalPendiente());

        // El más reciente (28 ene, el pago pendiente) debe ir primero; el más antiguo
        // (5 ene, la cita) debe ir al final.
        List<EventoHistorialDTO> eventos = historial.getEventos();
        assertEquals(LocalDate.of(2026, 1, 28), eventos.get(0).getFecha());
        assertEquals(LocalDate.of(2026, 1, 5), eventos.get(eventos.size() - 1).getFecha());
        for (int i = 0; i < eventos.size() - 1; i++) {
            assertTrue(!eventos.get(i).getFecha().isBefore(eventos.get(i + 1).getFecha()));
        }
    }

    private CitaMedicaEntity citaDe(LocalDate fecha, EstadoCitaEnum estado) {
        CitaMedicaEntity cita = new CitaMedicaEntity();
        cita.setFechaCita(fecha);
        cita.setHoraCita(LocalTime.of(9, 0));
        cita.setEstadoCita(estado);
        cita.setMotivoConsulta("Control general");
        cita.setMedico(new MedicoEntity());
        return cita;
    }

    private ComprobantePagoEntity pagoDe(LocalDate fecha, EstadoPagoEnum estado, BigDecimal monto) {
        ComprobantePagoEntity pago = new ComprobantePagoEntity();
        pago.setFechaEmision(fecha);
        pago.setEstado(estado);
        pago.setMontoTotal(monto);
        pago.setSubtotal(monto);
        pago.setTipoComprobante(TipoComprobanteEnum.BOLETA);
        pago.setMetodoPago(MetodoPagoEnum.EFECTIVO);
        return pago;
    }
}
