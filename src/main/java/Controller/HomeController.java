package Controller;

import Entity.Emuns.EstadoCitaEnum;
import Entity.Emuns.EstadoPagoEnum;
import Entity.MedicamentoEntity;
import Service.IComprobantePagoService;
import Service.ICitaMedicaService;
import Service.IMedicamentoService;
import Service.IPacienteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Controller
public class HomeController {

    private final ICitaMedicaService citaMedicaService;
    private final IComprobantePagoService comprobantePagoService;
    private final IMedicamentoService medicamentoService;
    private final IPacienteService pacienteService;

    public HomeController(ICitaMedicaService citaMedicaService,
                           IComprobantePagoService comprobantePagoService,
                           IMedicamentoService medicamentoService,
                           IPacienteService pacienteService) {
        this.citaMedicaService = citaMedicaService;
        this.comprobantePagoService = comprobantePagoService;
        this.medicamentoService = medicamentoService;
        this.pacienteService = pacienteService;
    }

    @GetMapping("/")
    public String inicio(HttpSession session, Model model) {
        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }

        cargarIndicadores(model);
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
}
