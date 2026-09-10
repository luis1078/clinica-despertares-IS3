package Controller;

import Entity.CitaMedicaEntity;
import Entity.ComprobantePagoEntity;
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
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM");

    private final ICitaMedicaService citaMedicaService;
    private final IComprobantePagoService comprobantePagoService;
    private final IMedicamentoService medicamentoService;
    private final IPacienteService pacienteService;
    private final IDiagnosticoService diagnosticoService;

    public HomeController(ICitaMedicaService citaMedicaService,
                           IComprobantePagoService comprobantePagoService,
                           IMedicamentoService medicamentoService,
                           IPacienteService pacienteService,
                           IDiagnosticoService diagnosticoService) {
        this.citaMedicaService = citaMedicaService;
        this.comprobantePagoService = comprobantePagoService;
        this.medicamentoService = medicamentoService;
        this.pacienteService = pacienteService;
        this.diagnosticoService = diagnosticoService;
    }

    @GetMapping("/")
    public String inicio(HttpSession session, Model model) {
        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }

        cargarIndicadores(model);
        cargarGraficos(model);
        return "index";
    }

    /**
     * Indicadores del panel principal. Se calculan todos sobre datos ya cargados
     * por los servicios existentes (sin repositorios nuevos); el propio index.html
     * decide, según el rol de la sesión, cuáles tarjetas mostrar.
     */
    private void cargarIndicadores(Model model) {
        LocalDate hoy = LocalDate.now();
        YearMonth mesActual = YearMonth.from(hoy);

        long citasHoy = citaMedicaService.listarPorFecha(hoy).size();
        long citasPendientes = citaMedicaService.listarPorEstado(EstadoCitaEnum.PENDIENTE).size();

        BigDecimal cobradoMes = comprobantePagoService.listarTodos().stream()
                .filter(c -> c.getEstado() == EstadoPagoEnum.CANCELADO)
                .filter(c -> c.getFechaEmision() != null && YearMonth.from(c.getFechaEmision()).equals(mesActual))
                .map(c -> c.getMontoTotal() == null ? BigDecimal.ZERO : c.getMontoTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long comprobantesPendientes = comprobantePagoService.listarTodos().stream()
                .filter(c -> c.getEstado() == EstadoPagoEnum.FALTA_PAGAR)
                .count();

        long medicamentosStockBajo = medicamentoService.listarTodos().stream()
                .filter(MedicamentoEntity::isStockBajo)
                .count();

        long pacientesTotal = pacienteService.listarTodos().size();

        model.addAttribute("citasHoy", citasHoy);
        model.addAttribute("citasPendientes", citasPendientes);
        model.addAttribute("cobradoMes", cobradoMes);
        model.addAttribute("comprobantesPendientes", comprobantesPendientes);
        model.addAttribute("medicamentosStockBajo", medicamentosStockBajo);
        model.addAttribute("pacientesTotal", pacientesTotal);
    }

    /**
     * Datasets para los gráficos de Chart.js del panel principal. Igual que
     * los indicadores de arriba, se calculan todos y es la propia plantilla
     * la que decide, según el rol de la sesión, qué tarjeta de gráfico mostrar
     * (el canvas correspondiente simplemente no existe en el DOM si no aplica).
     */
    private void cargarGraficos(Model model) {
        LocalDate hoy = LocalDate.now();
        List<CitaMedicaEntity> todasLasCitas = citaMedicaService.listarTodos();
        List<ComprobantePagoEntity> todosLosComprobantes = comprobantePagoService.listarTodos();

        model.addAttribute("citasPorDia", citasPorDia(todasLasCitas, hoy));
        model.addAttribute("citasPorEstado", citasPorEstado(todasLasCitas));
        model.addAttribute("ingresosPorDia", ingresosPorDia(todosLosComprobantes, hoy));
        model.addAttribute("comprobantesPorEstado", comprobantesPorEstado(todosLosComprobantes));
        model.addAttribute("diagnosticosPorGravedad", diagnosticosPorGravedad());
        model.addAttribute("medicamentosPorStock", medicamentosPorStock());
    }

    private GraficoDTO citasPorDia(List<CitaMedicaEntity> citas, LocalDate hoy) {
        List<String> etiquetas = new ArrayList<>();
        List<Number> valores = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            long total = citas.stream().filter(c -> dia.equals(c.getFechaCita())).count();
            etiquetas.add(dia.format(FORMATO_DIA));
            valores.add(total);
        }
        return new GraficoDTO("Citas de los últimos 7 días", etiquetas, valores);
    }

    private GraficoDTO citasPorEstado(List<CitaMedicaEntity> citas) {
        Map<EstadoCitaEnum, Long> porEstado = citas.stream()
                .collect(Collectors.groupingBy(CitaMedicaEntity::getEstadoCita, Collectors.counting()));
        List<String> etiquetas = List.of("Pendiente", "Finalizada", "Cancelada");
        List<Number> valores = List.of(
                porEstado.getOrDefault(EstadoCitaEnum.PENDIENTE, 0L),
                porEstado.getOrDefault(EstadoCitaEnum.FINALIZADA, 0L),
                porEstado.getOrDefault(EstadoCitaEnum.CANCELADA, 0L));
        return new GraficoDTO("Citas por estado", etiquetas, valores);
    }

    private GraficoDTO ingresosPorDia(List<ComprobantePagoEntity> comprobantes, LocalDate hoy) {
        List<String> etiquetas = new ArrayList<>();
        List<Number> valores = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            BigDecimal total = comprobantes.stream()
                    .filter(c -> c.getEstado() == EstadoPagoEnum.CANCELADO && dia.equals(c.getFechaEmision()))
                    .map(c -> c.getMontoTotal() == null ? BigDecimal.ZERO : c.getMontoTotal())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            etiquetas.add(dia.format(FORMATO_DIA));
            valores.add(total);
        }
        return new GraficoDTO("Ingresos de los últimos 7 días (S/)", etiquetas, valores);
    }

    private GraficoDTO comprobantesPorEstado(List<ComprobantePagoEntity> comprobantes) {
        long pagados = comprobantes.stream().filter(c -> c.getEstado() == EstadoPagoEnum.CANCELADO).count();
        long pendientes = comprobantes.stream().filter(c -> c.getEstado() == EstadoPagoEnum.FALTA_PAGAR).count();
        return new GraficoDTO("Comprobantes por estado", List.of("Pagado", "Pendiente"), List.of(pagados, pendientes));
    }

    private GraficoDTO diagnosticosPorGravedad() {
        Map<GravedadDiagnosticoEnum, Long> porGravedad = diagnosticoService.listarTodos().stream()
                .collect(Collectors.groupingBy(d -> d.getGravedadDiagnostico(), Collectors.counting()));
        List<String> etiquetas = List.of("Leve", "Moderado", "Grave");
        List<Number> valores = List.of(
                porGravedad.getOrDefault(GravedadDiagnosticoEnum.LEVE, 0L),
                porGravedad.getOrDefault(GravedadDiagnosticoEnum.MODERADO, 0L),
                porGravedad.getOrDefault(GravedadDiagnosticoEnum.GRAVE, 0L));
        return new GraficoDTO("Diagnósticos por gravedad", etiquetas, valores);
    }

    private GraficoDTO medicamentosPorStock() {
        List<MedicamentoEntity> medicamentos = medicamentoService.listarTodos();
        long stockBajo = medicamentos.stream().filter(MedicamentoEntity::isStockBajo).count();
        long stockOk = medicamentos.size() - stockBajo;
        return new GraficoDTO("Medicamentos por estado de stock", List.of("Stock OK", "Stock bajo"), List.of(stockOk, stockBajo));
    }
}
