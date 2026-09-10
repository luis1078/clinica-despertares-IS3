package Controller;

import Entity.CitaMedicaEntity;
import Entity.ComprobantePagoEntity;
import Entity.DiagnosticoEntity;
import Entity.Emuns.EstadoCitaEnum;
import Entity.Emuns.EstadoPagoEnum;
import Entity.Emuns.GravedadDiagnosticoEnum;
import Entity.MedicamentoEntity;
import Service.IComprobantePagoService;
import Service.ICitaMedicaService;
import Service.IDiagnosticoService;
import Service.IMedicamentoService;
import Service.IPacienteService;
import Service.dto.GraficoDTO;
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
 * cobrado del mes, comprobantes por cobrar y stock bajo) y de los datasets que
 * alimentan los gráficos de Chart.js (línea de tiempo de 7 días, agrupaciones por
 * estado/gravedad y el reparto de stock).
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
    @Mock
    private IDiagnosticoService diagnosticoService;

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
        when(citaMedicaService.listarTodos()).thenReturn(List.of());

        ComprobantePagoEntity pagadoEsteMes = comprobante(EstadoPagoEnum.CANCELADO, hoy, new BigDecimal("150.00"));
        ComprobantePagoEntity pagadoMesPasado = comprobante(EstadoPagoEnum.CANCELADO, hoy.minusMonths(1), new BigDecimal("999.00"));
        ComprobantePagoEntity pendiente = comprobante(EstadoPagoEnum.FALTA_PAGAR, hoy, new BigDecimal("40.00"));
        when(comprobantePagoService.listarTodos()).thenReturn(List.of(pagadoEsteMes, pagadoMesPasado, pendiente));

        when(medicamentoService.listarTodos()).thenReturn(List.of(medicamento(true), medicamento(false)));
        when(pacienteService.listarTodos()).thenReturn(List.of());
        when(diagnosticoService.listarTodos()).thenReturn(List.of());

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

    @Test
    void inicio_conSesion_armaLosDatasetsDeLosGraficos() {
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);

        when(citaMedicaService.listarPorFecha(hoy)).thenReturn(List.of());
        when(citaMedicaService.listarPorEstado(EstadoCitaEnum.PENDIENTE)).thenReturn(List.of());
        when(citaMedicaService.listarTodos()).thenReturn(List.of(
                cita(hoy, EstadoCitaEnum.PENDIENTE),
                cita(hoy, EstadoCitaEnum.FINALIZADA),
                cita(ayer, EstadoCitaEnum.CANCELADA)));

        when(comprobantePagoService.listarTodos()).thenReturn(List.of(
                comprobante(EstadoPagoEnum.CANCELADO, hoy, new BigDecimal("100.00")),
                comprobante(EstadoPagoEnum.CANCELADO, ayer, new BigDecimal("50.00")),
                comprobante(EstadoPagoEnum.FALTA_PAGAR, hoy, new BigDecimal("30.00"))));

        when(medicamentoService.listarTodos()).thenReturn(List.of(medicamento(false), medicamento(false), medicamento(true)));
        when(pacienteService.listarTodos()).thenReturn(List.of());
        when(diagnosticoService.listarTodos()).thenReturn(List.of(
                diagnostico(GravedadDiagnosticoEnum.LEVE),
                diagnostico(GravedadDiagnosticoEnum.LEVE),
                diagnostico(GravedadDiagnosticoEnum.GRAVE)));

        Model model = new ExtendedModelMap();
        homeController.inicio(sesionLogueada, model);

        GraficoDTO citasPorDia = (GraficoDTO) model.getAttribute("citasPorDia");
        assertEquals(7, citasPorDia.getEtiquetas().size());
        assertEquals(2L, citasPorDia.getValores().get(6)); // hoy: última posición
        assertEquals(1L, citasPorDia.getValores().get(5)); // ayer: penúltima posición

        GraficoDTO citasPorEstado = (GraficoDTO) model.getAttribute("citasPorEstado");
        assertEquals(List.of(1L, 1L, 1L), citasPorEstado.getValores());

        GraficoDTO ingresosPorDia = (GraficoDTO) model.getAttribute("ingresosPorDia");
        assertEquals(new BigDecimal("100.00"), ingresosPorDia.getValores().get(6));
        assertEquals(new BigDecimal("50.00"), ingresosPorDia.getValores().get(5));

        GraficoDTO comprobantesPorEstado = (GraficoDTO) model.getAttribute("comprobantesPorEstado");
        assertEquals(List.of(2L, 1L), comprobantesPorEstado.getValores());

        GraficoDTO diagnosticosPorGravedad = (GraficoDTO) model.getAttribute("diagnosticosPorGravedad");
        assertEquals(List.of(2L, 0L, 1L), diagnosticosPorGravedad.getValores());

        GraficoDTO medicamentosPorStock = (GraficoDTO) model.getAttribute("medicamentosPorStock");
        assertEquals(List.of(2L, 1L), medicamentosPorStock.getValores());
    }

    private Entity.CitaMedicaEntity nuevaCitaVacia() {
        return new Entity.CitaMedicaEntity();
    }

    private CitaMedicaEntity cita(LocalDate fechaCita, EstadoCitaEnum estado) {
        CitaMedicaEntity cita = new CitaMedicaEntity();
        cita.setFechaCita(fechaCita);
        cita.setEstadoCita(estado);
        return cita;
    }

    private DiagnosticoEntity diagnostico(GravedadDiagnosticoEnum gravedad) {
        DiagnosticoEntity diagnostico = new DiagnosticoEntity();
        diagnostico.setGravedadDiagnostico(gravedad);
        return diagnostico;
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
