package Service.ServiceImpl;

import Entity.ComprobantePagoEntity;
import Entity.DetalleComprobanteEntity;
import Entity.Emuns.EstadoPagoEnum;
import Entity.Emuns.MetodoPagoEnum;
import Entity.Emuns.TipoComprobanteEnum;
import Entity.Emuns.TipoItemEnum;
import Entity.MedicamentoEntity;
import Repository.IComprobantePagoRepository;
import Repository.IDetalleComprobanteRepository;
import Repository.IMedicamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre la lógica de negocio más riesgosa de DetalleComprobanteServiceImpl:
 * descuento/devolución de stock de medicamentos y recálculo del total del
 * comprobante, que hoy no tenían ninguna prueba.
 */
@ExtendWith(MockitoExtension.class)
class DetalleComprobanteServiceImplTest {

    @Mock
    private IDetalleComprobanteRepository detalleComprobanteRepository;
    @Mock
    private IComprobantePagoRepository comprobantePagoRepository;
    @Mock
    private IMedicamentoRepository medicamentoRepository;

    @InjectMocks
    private DetalleComprobanteServiceImpl detalleComprobanteService;

    private ComprobantePagoEntity comprobante;
    private MedicamentoEntity medicamento;

    @BeforeEach
    void setUp() {
        comprobante = new ComprobantePagoEntity();
        comprobante.setCodcomprobante(1L);
        comprobante.setFechaEmision(LocalDate.now());
        comprobante.setTipoComprobante(TipoComprobanteEnum.BOLETA);
        comprobante.setMetodoPago(MetodoPagoEnum.EFECTIVO);
        comprobante.setEstado(EstadoPagoEnum.FALTA_PAGAR);
        comprobante.setSubtotal(BigDecimal.ZERO);
        comprobante.setMontoTotal(BigDecimal.ZERO);

        medicamento = new MedicamentoEntity();
        medicamento.setCodMedicamento(5L);
        medicamento.setNombreMedicamento("Paracetamol");
        medicamento.setStockInventario(10);
        medicamento.setCantidadMinima(2);
    }

    @Test
    void registrarDetallePorMedicamento_descuentaStockYCalculaSubtotal() {
        DetalleComprobanteEntity detalle = new DetalleComprobanteEntity();
        detalle.setCantidad(3);
        detalle.setPrecioUnitario(new BigDecimal("15.50"));
        detalle.setDescripcionDetalle("Paracetamol x3");

        when(comprobantePagoRepository.findById(1L)).thenReturn(Optional.of(comprobante));
        when(medicamentoRepository.findById(5L)).thenReturn(Optional.of(medicamento));
        when(detalleComprobanteRepository.save(any(DetalleComprobanteEntity.class)))
                .thenAnswer(invocacion -> {
                    DetalleComprobanteEntity guardado = invocacion.getArgument(0);
                    if (guardado.getIddetalle() == null) {
                        guardado.setIddetalle(100L);
                    }
                    return guardado;
                });
        when(detalleComprobanteRepository.findByComprobantePago_Codcomprobante(1L))
                .thenReturn(List.of(detalle));

        DetalleComprobanteEntity resultado = detalleComprobanteService.registrarDetallePorMedicamento(1L, 5L, detalle);

        assertEquals(new BigDecimal("46.50"), resultado.getSubtotal());
        assertEquals(7, medicamento.getStockInventario());
        assertEquals(TipoItemEnum.MEDICAMENTO, resultado.getTipoItem());
        assertEquals(5L, resultado.getIdReferencia());
        assertEquals(new BigDecimal("46.50"), comprobante.getMontoTotal());

        verify(medicamentoRepository).save(medicamento);
        verify(comprobantePagoRepository).save(comprobante);
    }

    @Test
    void registrarDetallePorMedicamento_stockInsuficiente_lanzaExcepcionYNoGuardaNada() {
        DetalleComprobanteEntity detalle = new DetalleComprobanteEntity();
        detalle.setCantidad(20);
        detalle.setPrecioUnitario(new BigDecimal("15.50"));

        when(comprobantePagoRepository.findById(1L)).thenReturn(Optional.of(comprobante));
        when(medicamentoRepository.findById(5L)).thenReturn(Optional.of(medicamento));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> detalleComprobanteService.registrarDetallePorMedicamento(1L, 5L, detalle));

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        assertEquals(10, medicamento.getStockInventario());
        verify(medicamentoRepository, never()).save(any());
        verify(detalleComprobanteRepository, never()).save(any());
    }

    @Test
    void guardar_cantidadCero_lanzaExcepcionAntesDeGuardar() {
        DetalleComprobanteEntity detalle = new DetalleComprobanteEntity();
        detalle.setComprobantePago(comprobante);
        detalle.setTipoItem(TipoItemEnum.CITA);
        detalle.setIdReferencia(99L);
        detalle.setCantidad(0);
        detalle.setPrecioUnitario(new BigDecimal("50.00"));

        when(detalleComprobanteRepository.findByTipoItem(TipoItemEnum.CITA)).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> detalleComprobanteService.guardar(detalle));

        assertEquals("La cantidad debe ser mayor a cero.", ex.getMessage());
        verify(detalleComprobanteRepository, never()).save(any());
    }

    @Test
    void eliminar_detalleDeMedicamento_devuelveStockAntesDeBorrar() {
        DetalleComprobanteEntity detalle = new DetalleComprobanteEntity();
        detalle.setIddetalle(10L);
        detalle.setTipoItem(TipoItemEnum.MEDICAMENTO);
        detalle.setIdReferencia(5L);
        detalle.setCantidad(3);
        detalle.setComprobantePago(comprobante);

        when(detalleComprobanteRepository.findById(10L)).thenReturn(Optional.of(detalle));
        when(medicamentoRepository.findById(5L)).thenReturn(Optional.of(medicamento));
        when(detalleComprobanteRepository.findByComprobantePago_Codcomprobante(1L)).thenReturn(List.of());

        detalleComprobanteService.eliminar(10L);

        assertEquals(13, medicamento.getStockInventario());
        verify(medicamentoRepository).save(medicamento);
        verify(detalleComprobanteRepository).deleteById(10L);
        verify(comprobantePagoRepository).save(comprobante);
        assertEquals(BigDecimal.ZERO, comprobante.getMontoTotal());
    }
}
