package Controller;

import Entity.ComprobantePagoEntity;
import Entity.Emuns.EstadoCitaEnum;
import Entity.Emuns.EstadoPagoEnum;
import Entity.MedicamentoEntity;
import Service.IComprobantePagoService;
import Service.ICitaMedicaService;
import Service.IMedicamentoService;
import Service.IPacienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Cubre el cálculo de los indicadores del panel principal (citas de hoy/pendientes,
 * cobrado del mes, comprobantes por cobrar y stock bajo), que hasta ahora vivía sin
 * ninguna prueba en HomeController.
 */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private ICitaMedicaService citaMedicaService;
    @Mock
    private IComprobantePagoService comprobantePagoService;
    @Mock
    private IMedicamentoService medicamentoService;
    @Mock
    private IPacienteService pacienteService;

    @InjectMocks
    private HomeController homeController;

    private MockHttpSession sesionLogueada;

    @BeforeEach
    void setUp() {
        sesionLogueada = new MockHttpSession();
        sesionLogueada.setAttribute("usuarioLogueado", "cualquiera");
    }

    @Test
    void inicio_sinSesion_redirigeALogin() {
        Model model = new ExtendedModelMap();

        String vista = homeController.inicio(new MockHttpSession(), model);

        assertEquals("redirect:/login", vista);
    }

    @Test
    void inicio_conSesion_calculaIndicadoresDelMesYFiltraPorEstado() {
        LocalDate hoy = LocalDate.now();

        when(citaMedicaService.listarPorFecha(hoy)).thenReturn(List.of(nuevaCitaVacia(), nuevaCitaVacia()));
        when(citaMedicaService.listarPorEstado(EstadoCitaEnum.PENDIENTE)).thenReturn(List.of(nuevaCitaVacia()));

        ComprobantePagoEntity pagadoEsteMes = comprobante(EstadoPagoEnum.CANCELADO, hoy, new BigDecimal("150.00"));
        ComprobantePagoEntity pagadoMesPasado = comprobante(EstadoPagoEnum.CANCELADO, hoy.minusMonths(1), new BigDecimal("999.00"));
        ComprobantePagoEntity pendiente = comprobante(EstadoPagoEnum.FALTA_PAGAR, hoy, new BigDecimal("40.00"));
        when(comprobantePagoService.listarTodos()).thenReturn(List.of(pagadoEsteMes, pagadoMesPasado, pendiente));

        when(medicamentoService.listarTodos()).thenReturn(List.of(medicamento(true), medicamento(false)));
        when(pacienteService.listarTodos()).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String vista = homeController.inicio(sesionLogueada, model);

        assertEquals("index", vista);
        assertEquals(2L, model.getAttribute("citasHoy"));
        assertEquals(1L, model.getAttribute("citasPendientes"));
        assertEquals(new BigDecimal("150.00"), model.getAttribute("cobradoMes"));
        assertEquals(1L, model.getAttribute("comprobantesPendientes"));
        assertEquals(1L, model.getAttribute("medicamentosStockBajo"));
        assertEquals(0L, model.getAttribute("pacientesTotal"));
    }

    private Entity.CitaMedicaEntity nuevaCitaVacia() {
        return new Entity.CitaMedicaEntity();
    }

    private ComprobantePagoEntity comprobante(EstadoPagoEnum estado, LocalDate fechaEmision, BigDecimal monto) {
        ComprobantePagoEntity comprobante = new ComprobantePagoEntity();
        comprobante.setEstado(estado);
        comprobante.setFechaEmision(fechaEmision);
        comprobante.setMontoTotal(monto);
        return comprobante;
    }

    private MedicamentoEntity medicamento(boolean stockBajo) {
        MedicamentoEntity medicamento = new MedicamentoEntity();
        medicamento.setStockInventario(stockBajo ? 0 : 100);
        medicamento.setCantidadMinima(5);
        return medicamento;
    }
}
