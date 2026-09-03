package Service.ServiceImpl;

import Entity.CitaMedicaEntity;
import Entity.ComprobantePagoEntity;
import Entity.DetalleComprobanteEntity;
import Entity.Emuns.EstadoCitaEnum;
import Entity.MedicoEntity;
import Entity.PacienteEntity;
import Repository.ICitaMedicaRepository;
import Repository.IComprobantePagoRepository;
import Repository.IDetalleComprobanteRepository;
import Repository.IMedicoRepository;
import Repository.IPacienteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre la disponibilidad de médico y el registro de citas médicas
 * (incluida la generación automática del comprobante de pago), que hoy
 * no tenían ninguna prueba pese a ser la lógica más crítica del módulo.
 */
@ExtendWith(MockitoExtension.class)
class CitaMedicaServiceImplTest {

    @Mock
    private ICitaMedicaRepository citaMedicaRepository;
    @Mock
    private IPacienteRepository pacienteRepository;
    @Mock
    private IMedicoRepository medicoRepository;
    @Mock
    private IComprobantePagoRepository comprobantePagoRepository;
    @Mock
    private IDetalleComprobanteRepository detalleComprobanteRepository;

    @InjectMocks
    private CitaMedicaServiceImpl citaMedicaService;

    private PacienteEntity paciente;
    private MedicoEntity medico;

    @BeforeEach
    void setUp() {
        paciente = new PacienteEntity();
        paciente.setDnipaciente("12345678");

        medico = new MedicoEntity();
        medico.setIdMedico(1L);
    }

    @Test
    void existeDisponibilidadMedico_true_sinCitasEnEseHorario() {
        when(citaMedicaRepository.findByMedico_IdMedico(1L)).thenReturn(List.of());

        boolean disponible = citaMedicaService.existeDisponibilidadMedico(
                1L, LocalDate.of(2026, 3, 10), LocalTime.of(9, 0));

        assertTrue(disponible);
    }

    @Test
    void existeDisponibilidadMedico_false_conCitaActivaEnMismoHorario() {
        CitaMedicaEntity citaExistente = new CitaMedicaEntity();
        citaExistente.setFechaCita(LocalDate.of(2026, 3, 10));
        citaExistente.setHoraCita(LocalTime.of(9, 0));
        citaExistente.setEstadoCita(EstadoCitaEnum.PENDIENTE);

        when(citaMedicaRepository.findByMedico_IdMedico(1L)).thenReturn(List.of(citaExistente));

        boolean disponible = citaMedicaService.existeDisponibilidadMedico(
                1L, LocalDate.of(2026, 3, 10), LocalTime.of(9, 0));

        assertFalse(disponible);
    }

    @Test
    void existeDisponibilidadMedico_true_siLaCitaEnEseHorarioEstaCancelada() {
        CitaMedicaEntity citaCancelada = new CitaMedicaEntity();
        citaCancelada.setFechaCita(LocalDate.of(2026, 3, 10));
        citaCancelada.setHoraCita(LocalTime.of(9, 0));
        citaCancelada.setEstadoCita(EstadoCitaEnum.CANCELADA);

        when(citaMedicaRepository.findByMedico_IdMedico(1L)).thenReturn(List.of(citaCancelada));

        boolean disponible = citaMedicaService.existeDisponibilidadMedico(
                1L, LocalDate.of(2026, 3, 10), LocalTime.of(9, 0));

        assertTrue(disponible);
    }

    @Test
    void registrarCitaMedica_exito_generaComprobanteYDetalleAutomaticamente() {
        CitaMedicaEntity cita = new CitaMedicaEntity();
        cita.setFechaCita(LocalDate.of(2026, 3, 10));
        cita.setHoraCita(LocalTime.of(9, 0));
        cita.setMotivoConsulta("Control general");

        when(pacienteRepository.findById("12345678")).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico));
        when(citaMedicaRepository.findByMedico_IdMedico(1L)).thenReturn(List.of());
        when(citaMedicaRepository.save(any(CitaMedicaEntity.class))).thenAnswer(invocacion -> {
            CitaMedicaEntity guardada = invocacion.getArgument(0);
            guardada.setCodCitaMedica(500L);
            return guardada;
        });
        when(detalleComprobanteRepository.existsByTipoItemAndIdReferencia(any(), any())).thenReturn(false);
        when(comprobantePagoRepository.save(any(ComprobantePagoEntity.class))).thenAnswer(invocacion -> {
            ComprobantePagoEntity guardado = invocacion.getArgument(0);
            guardado.setCodcomprobante(900L);
            return guardado;
        });

        CitaMedicaEntity resultado = citaMedicaService.registrarCitaMedica("12345678", 1L, cita);

        assertEquals(EstadoCitaEnum.PENDIENTE, resultado.getEstadoCita());
        assertEquals(paciente, resultado.getPaciente());
        assertEquals(medico, resultado.getMedico());

        verify(comprobantePagoRepository).save(any(ComprobantePagoEntity.class));
        verify(detalleComprobanteRepository).save(any(DetalleComprobanteEntity.class));
    }

    @Test
    void registrarCitaMedica_medicoSinDisponibilidad_lanzaExcepcionYNoGuardaNada() {
        CitaMedicaEntity citaExistente = new CitaMedicaEntity();
        citaExistente.setFechaCita(LocalDate.of(2026, 3, 10));
        citaExistente.setHoraCita(LocalTime.of(9, 0));
        citaExistente.setEstadoCita(EstadoCitaEnum.PENDIENTE);

        CitaMedicaEntity nuevaCita = new CitaMedicaEntity();
        nuevaCita.setFechaCita(LocalDate.of(2026, 3, 10));
        nuevaCita.setHoraCita(LocalTime.of(9, 0));
        nuevaCita.setMotivoConsulta("Otra consulta");

        when(pacienteRepository.findById("12345678")).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico));
        when(citaMedicaRepository.findByMedico_IdMedico(1L)).thenReturn(List.of(citaExistente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> citaMedicaService.registrarCitaMedica("12345678", 1L, nuevaCita));

        assertTrue(ex.getMessage().contains("no tiene disponibilidad"));
        verify(citaMedicaRepository, never()).save(any());
        verify(comprobantePagoRepository, never()).save(any());
    }

    @Test
    void registrarCitaMedica_pacienteNoExiste_lanzaEntityNotFoundExceptionYNoConsultaMedico() {
        CitaMedicaEntity cita = new CitaMedicaEntity();
        cita.setFechaCita(LocalDate.of(2026, 3, 10));
        cita.setHoraCita(LocalTime.of(9, 0));

        when(pacienteRepository.findById("99999999")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> citaMedicaService.registrarCitaMedica("99999999", 1L, cita));

        verify(medicoRepository, never()).findById(any());
        verify(citaMedicaRepository, never()).save(any());
    }
}
