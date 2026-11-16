package QA;

import Entity.*;
import Entity.Emuns.*;
import Repository.*;
import Service.ServiceImpl.CitaMedicaServiceImpl;
import Service.ServiceImpl.DetalleComprobanteServiceImpl;
import Service.ServiceImpl.MedicamentoServiceImpl;
import org.junit.jupiter.api.DisplayName;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de caja blanca (capa Service con repositorios simulados).
 * Cubren decisiones y condiciones de la lógica de negocio crítica.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de caja blanca (Service con Mockito)")
class FTC05_UnitariasServiceTest {

    @Mock ICitaMedicaRepository citaRepo;
    @Mock IPacienteRepository pacienteRepo;
    @Mock IMedicoRepository medicoRepo;
    @Mock IComprobantePagoRepository comprobanteRepo;
    @Mock IDetalleComprobanteRepository detalleRepo;
    @InjectMocks CitaMedicaServiceImpl citaService;

    @Test
    @DisplayName("FTC015 - No duplicar el comprobante si la cita ya tiene detalle")
    void ftc015() {
        PacienteEntity p = new PacienteEntity(); p.setDnipaciente("12345678");
        MedicoEntity m = new MedicoEntity(); m.setIdMedico(1L);
        CitaMedicaEntity c = new CitaMedicaEntity();
        c.setFechaCita(LocalDate.of(2026, 12, 1)); c.setHoraCita(LocalTime.of(9, 0));
        when(pacienteRepo.findById("12345678")).thenReturn(Optional.of(p));
        when(medicoRepo.findById(1L)).thenReturn(Optional.of(m));
        when(citaRepo.findByMedico_IdMedico(1L)).thenReturn(List.of());
        when(citaRepo.save(any())).thenAnswer(inv -> { CitaMedicaEntity x = inv.getArgument(0); x.setCodCitaMedica(10L); return x; });
        when(detalleRepo.existsByTipoItemAndIdReferencia(TipoItemEnum.CITA, 10L)).thenReturn(true);

        citaService.registrarCitaMedica("12345678", 1L, c);

        verify(citaRepo).save(c);
        verify(comprobanteRepo, never()).save(any());
        verify(detalleRepo, never()).save(any());
    }

    @Test
    @DisplayName("CB01 - Disponibilidad: cubre las tres condiciones (fecha, hora, estado ≠ CANCELADA)")
    void cb01_condicionesDisponibilidad() {
        MedicoEntity m = new MedicoEntity(); m.setIdMedico(2L);
        LocalDate f = LocalDate.of(2026, 12, 2);
        CitaMedicaEntity otraFecha = cita(f.plusDays(1), LocalTime.of(9, 0), EstadoCitaEnum.PENDIENTE);
        CitaMedicaEntity otraHora = cita(f, LocalTime.of(10, 0), EstadoCitaEnum.PENDIENTE);
        CitaMedicaEntity cancelada = cita(f, LocalTime.of(9, 0), EstadoCitaEnum.CANCELADA);
        CitaMedicaEntity choque = cita(f, LocalTime.of(9, 0), EstadoCitaEnum.FINALIZADA);
        when(citaRepo.findByMedico_IdMedico(2L)).thenReturn(List.of(otraFecha, otraHora, cancelada));
        assertTrue(citaService.existeDisponibilidadMedico(2L, f, LocalTime.of(9, 0)));
        when(citaRepo.findByMedico_IdMedico(2L)).thenReturn(List.of(otraFecha, otraHora, cancelada, choque));
        assertFalse(citaService.existeDisponibilidadMedico(2L, f, LocalTime.of(9, 0)));
    }

    @Test
    @DisplayName("CB02 - Reprogramar: decisión 'cita sin médico asignado'")
    void cb02_reprogramarSinMedico() {
        CitaMedicaEntity c = cita(LocalDate.of(2026, 12, 3), LocalTime.of(9, 0), EstadoCitaEnum.PENDIENTE);
        c.setMedico(null);
        when(citaRepo.findById(5L)).thenReturn(Optional.of(c));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> citaService.reprogramarCitaMedica(5L, LocalDate.of(2026, 12, 4), LocalTime.of(9, 0)));
        assertEquals("La cita no tiene un médico asignado.", e.getMessage());
    }

    @Test
    @DisplayName("CB03 - Stock: decisiones de actualizarStock (negativo / cero / positivo)")
    void cb03_actualizarStock() {
        IMedicamentoRepository medRepo = mock(IMedicamentoRepository.class);
        MedicamentoServiceImpl s = new MedicamentoServiceImpl(medRepo, mock(IProveedorRepository.class));
        MedicamentoEntity m = new MedicamentoEntity(); m.setStockInventario(20);
        when(medRepo.findById(1L)).thenReturn(Optional.of(m));
        when(medRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThrows(IllegalArgumentException.class, () -> s.actualizarStock(1L, -1));
        assertEquals(0, s.actualizarStock(1L, 0).getStockInventario());
        assertEquals(7, s.actualizarStock(1L, 7).getStockInventario());
    }

    @Test
    @DisplayName("CB04 - Detalle de comprobante: precio nulo → 0 y cantidad ≤ 0 rechazada")
    void cb04_calculoSubtotal() {
        IDetalleComprobanteRepository dRepo = mock(IDetalleComprobanteRepository.class);
        IComprobantePagoRepository cRepo = mock(IComprobantePagoRepository.class);
        DetalleComprobanteServiceImpl s = new DetalleComprobanteServiceImpl(dRepo, cRepo, mock(IMedicamentoRepository.class));
        ComprobantePagoEntity comp = new ComprobantePagoEntity(); comp.setCodcomprobante(3L);
        when(cRepo.findById(3L)).thenReturn(Optional.of(comp));
        when(dRepo.save(any())).thenAnswer(inv -> { DetalleComprobanteEntity d = inv.getArgument(0); d.setIddetalle(1L); return d; });
        when(dRepo.findByComprobantePago_Codcomprobante(3L)).thenReturn(List.of());

        DetalleComprobanteEntity sinPrecio = new DetalleComprobanteEntity(); sinPrecio.setCantidad(2);
        DetalleComprobanteEntity r = s.registrarDetallePorCita(3L, 99L, sinPrecio);
        assertEquals(0, BigDecimal.ZERO.compareTo(r.getSubtotal()));

        DetalleComprobanteEntity cero = new DetalleComprobanteEntity(); cero.setCantidad(0); cero.setPrecioUnitario(BigDecimal.TEN);
        assertThrows(IllegalArgumentException.class, () -> s.registrarDetallePorCita(3L, 98L, cero));
    }

    private CitaMedicaEntity cita(LocalDate f, LocalTime h, EstadoCitaEnum e) {
        CitaMedicaEntity c = new CitaMedicaEntity();
        c.setFechaCita(f); c.setHoraCita(h); c.setEstadoCita(e);
        MedicoEntity m = new MedicoEntity(); m.setIdMedico(2L); c.setMedico(m);
        return c;
    }
}
